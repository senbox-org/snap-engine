package org.esa.snap.dataio.netcdf.remote;

import org.esa.snap.core.util.SystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Read-only {@link ucar.unidata.io.RandomAccessFile} over HTTP(S) byte-range requests.
 * <p>
 * On open it follows the server's redirects by hand, sending credentials only to hosts a
 * {@link RemoteAuthProvider} claims (for NASA Earthdata Cloud: the archive host and Earthdata Login,
 * not the signed CloudFront URL it redirects to). Data is fetched in aligned blocks held in a shared
 * {@link BlockCache}; a read spanning several missing blocks fetches them in parallel. If the resolved
 * URL expires (HTTP 401/403) the original location is resolved again once.
 */
class HttpRangeRandomAccessFile extends ucar.unidata.io.RandomAccessFile {

    private static final Logger LOG = SystemUtils.LOG;
    private static final int MAX_REDIRECTS = 10;
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private static final ExecutorService PREFETCH = Executors.newFixedThreadPool(RemoteFileAccess.THREADS, r -> {
        Thread t = new Thread(r, "snap-netcdf-remote-read");
        t.setDaemon(true);
        return t;
    });

    /** Resolved URL and length per location, shared by all handles so each open skips the login redirects. */
    private static final Map<String, Resolution> RESOLVED = new ConcurrentHashMap<>();

    private record Resolution(URI uri, long length) {
    }

    private final URI origin;
    private final int blockSize;
    private final String cacheKey;
    private volatile URI resolved;
    private long length;
    private volatile boolean closed;

    HttpRangeRandomAccessFile(String location) throws IOException {
        this(location, RemoteFileAccess.BLOCK_SIZE);
    }

    HttpRangeRandomAccessFile(String location, int blockSize) throws IOException {
        super(8192);
        this.location = location;
        this.origin = URI.create(location);
        this.blockSize = blockSize;
        this.cacheKey = blockSize + ":" + location;
        Resolution known = RESOLVED.get(location);
        if (known != null) {
            resolved = known.uri();
            length = known.length();
        } else {
            resolve();
        }
    }

    static void clearResolved() {
        RESOLVED.clear();
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    protected int read_(long pos, byte[] b, int offset, int len) throws IOException {
        ensureOpen();
        if (pos >= length) {
            return -1;
        }
        len = (int) Math.min(len, length - pos);
        if (len <= 0) {
            return 0;
        }
        long first = pos / blockSize;
        long last = (pos + len - 1) / blockSize;
        if (!RemoteFileAccess.CACHE.contains(cacheKey, last)) {
            readAhead(last + 1);
        }
        if (last > first) {
            prefetch(first, last);
        }
        int done = 0;
        while (done < len) {
            long p = pos + done;
            byte[] block = block(p / blockSize);
            int inBlock = (int) (p % blockSize);
            int n = Math.min(len - done, block.length - inBlock);
            if (n <= 0) {
                throw new IOException("Short block from " + location + " at offset " + p);
            }
            System.arraycopy(block, inBlock, b, offset + done, n);
            done += n;
        }
        return len;
    }

    @Override
    public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
        byte[] chunk = new byte[(int) Math.min(nbytes, 4L * blockSize)];
        long done = 0;
        while (done < nbytes) {
            int n = read_(offset + done, chunk, 0, (int) Math.min(chunk.length, nbytes - done));
            if (n <= 0) {
                break;
            }
            ByteBuffer buffer = ByteBuffer.wrap(chunk, 0, n);
            while (buffer.hasRemaining()) {
                dest.write(buffer);
            }
            done += n;
        }
        return done;
    }

    @Override
    public void flush() {
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    @Override
    public String toString() {
        return location;
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("File is closed: " + location);
        }
    }

    private byte[] block(long index) throws IOException {
        return RemoteFileAccess.CACHE.get(cacheKey, index, () -> {
            long start = index * blockSize;
            long end = Math.min(start + blockSize, length) - 1;
            return fetchRange(start, end);
        });
    }

    /**
     * Loads every block that overlaps the given byte ranges, in parallel, and waits for them.
     */
    void prefetchRanges(List<ChunkLocator.Range> ranges) throws IOException {
        java.util.TreeSet<Long> blocks = new java.util.TreeSet<>();
        for (ChunkLocator.Range r : ranges) {
            long end = Math.min(r.start() + r.length(), length) - 1;
            for (long i = r.start() / blockSize; i <= end / blockSize; i++) {
                blocks.add(i);
            }
        }
        if ((long) blocks.size() * blockSize > RemoteFileAccess.CACHE.maxBytes() / 2) {
            LOG.fine("Skipping prefetch of " + blocks.size() + " blocks of " + location + ": larger than half the cache");
            return;
        }
        List<Future<byte[]>> futures = new ArrayList<>();
        for (long i : blocks) {
            if (!RemoteFileAccess.CACHE.contains(cacheKey, i)) {
                long index = i;
                futures.add(PREFETCH.submit(() -> block(index)));
            }
        }
        awaitAll(futures);
    }

    /**
     * Starts background fetches of the blocks after a cache miss. Chunks of one variable are usually
     * stored next to each other, so the next reads often find their blocks already loaded or in flight.
     */
    private void readAhead(long from) {
        long lastBlock = (length - 1) / blockSize;
        long to = Math.min(from + RemoteFileAccess.READ_AHEAD - 1, lastBlock);
        for (long i = from; i <= to; i++) {
            if (!RemoteFileAccess.CACHE.containsOrLoading(cacheKey, i)) {
                long index = i;
                PREFETCH.submit(() -> {
                    try {
                        block(index);
                    } catch (IOException e) {
                        LOG.log(Level.FINE, "Read-ahead failed for " + location, e);
                    }
                });
            }
        }
    }

    private void prefetch(long first, long last) throws IOException {
        List<Future<byte[]>> futures = new ArrayList<>();
        for (long i = first; i <= last; i++) {
            if (!RemoteFileAccess.CACHE.contains(cacheKey, i)) {
                long index = i;
                futures.add(PREFETCH.submit(() -> block(index)));
            }
        }
        awaitAll(futures);
    }

    private void awaitAll(List<Future<byte[]>> futures) throws IOException {
        for (Future<byte[]> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while reading " + location, e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                throw cause instanceof IOException ioe ? ioe : new IOException(cause.getMessage(), cause);
            }
        }
    }

    /**
     * Follows redirects from the original location to the URL that serves the bytes,
     * and reads the file length from the first range response.
     */
    private synchronized void resolve() throws IOException {
        URI uri = origin;
        boolean retriedAuth = false;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            HttpRequest.Builder builder = rangeRequest(uri, 0, 0);
            RemoteAuthProvider auth = RemoteFileAccess.findAuthProvider(uri);
            if (auth != null) {
                String header = auth.getAuthorizationHeader(uri);
                if (header != null) {
                    builder.header("Authorization", header);
                }
            }
            HttpResponse<InputStream> response = send(builder.build());
            int status = response.statusCode();
            try (InputStream ignored = response.body()) {
                if (status >= 300 && status < 400) {
                    String next = response.headers().firstValue("Location")
                            .orElseThrow(() -> new IOException("Redirect without Location header from " + hostOf(response.uri())));
                    uri = uri.resolve(next);
                    continue;
                }
                if (status == 401 && auth != null && !retriedAuth) {
                    auth.invalidate(uri);
                    retriedAuth = true;
                    uri = origin;
                    hop = -1;
                    continue;
                }
                if (status == 206) {
                    length = parseTotalLength(response);
                    resolved = uri;
                    RESOLVED.put(location, new Resolution(uri, length));
                    return;
                }
                if (status == 200) {
                    throw new IOException("Server does not support byte-range requests: " + hostOf(uri));
                }
                throw new IOException(describe(status) + " while opening " + location);
            }
        }
        throw new IOException("Too many redirects while opening " + location);
    }

    private byte[] fetchRange(long start, long end) throws IOException {
        IOException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            ensureOpen();
            URI uri = resolved;
            try {
                HttpResponse<byte[]> response = CLIENT.send(rangeRequest(uri, start, end).build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                int status = response.statusCode();
                if (status == 206) {
                    byte[] body = response.body();
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest("Fetched bytes " + start + "-" + end + " of " + location);
                    }
                    if (body.length != end - start + 1) {
                        throw new IOException("Expected " + (end - start + 1) + " bytes, got " + body.length);
                    }
                    return body;
                }
                if (status == 401 || status == 403 || (status >= 300 && status < 400)) {
                    // The signed URL has expired: resolve the original location again.
                    LOG.fine("Re-resolving " + location + " after HTTP " + status);
                    reResolve(uri);
                    last = new IOException(describe(status) + " while reading " + location);
                    continue;
                }
                last = new IOException(describe(status) + " while reading " + location);
                if (status < 500 && status != 429) {
                    throw last;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while reading " + location, e);
            } catch (IOException e) {
                last = e;
                LOG.log(Level.FINE, "Range read failed (attempt " + attempt + ") for " + location, e);
            }
            sleepBeforeRetry(attempt);
        }
        throw last;
    }

    /**
     * Resolves again unless another thread already replaced the stale URL.
     */
    private synchronized void reResolve(URI stale) throws IOException {
        if (stale.equals(resolved)) {
            resolve();
        }
    }

    private static HttpRequest.Builder rangeRequest(URI uri, long start, long end) {
        return HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Range", "bytes=" + start + "-" + end)
                .GET();
    }

    private static HttpResponse<InputStream> send(HttpRequest request) throws IOException {
        try {
            return CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        }
    }

    private static long parseTotalLength(HttpResponse<?> response) throws IOException {
        String range = response.headers().firstValue("Content-Range").orElse("");
        int slash = range.lastIndexOf('/');
        if (slash < 0 || range.endsWith("*")) {
            throw new IOException("Server did not report the file size (Content-Range: '" + range + "')");
        }
        return Long.parseLong(range.substring(slash + 1).trim());
    }

    private static void sleepBeforeRetry(int attempt) throws IOException {
        try {
            Thread.sleep(250L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        }
    }

    private static String hostOf(URI uri) {
        return uri == null ? "?" : uri.getHost();
    }

    private static String describe(int status) {
        return switch (status) {
            case 401 -> "HTTP 401 (login not accepted)";
            case 403 -> "HTTP 403 (access denied)";
            case 404 -> "HTTP 404 (file not found)";
            default -> "HTTP " + status;
        };
    }
}
