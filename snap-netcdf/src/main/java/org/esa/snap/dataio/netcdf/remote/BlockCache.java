package org.esa.snap.dataio.netcdf.remote;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Process-wide LRU cache of fixed-size, aligned file blocks, keyed by file location.
 * <p>
 * Shared by every handle on the same remote file, so a file opened twice (once to identify the format,
 * once to read it) fetches its header only once. Concurrent requests for the same missing block wait
 * for a single download.
 */
final class BlockCache {

    interface Loader {
        byte[] load() throws IOException;
    }

    private record Key(String location, long index) {
    }

    private final long maxBytes;
    private final LinkedHashMap<Key, byte[]> blocks = new LinkedHashMap<>(256, 0.75f, true);
    private final Map<Key, CompletableFuture<byte[]>> inFlight = new ConcurrentHashMap<>();
    private long currentBytes;

    BlockCache(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    long maxBytes() {
        return maxBytes;
    }

    synchronized boolean contains(String location, long index) {
        return blocks.containsKey(new Key(location, index));
    }

    boolean containsOrLoading(String location, long index) {
        return contains(location, index) || inFlight.containsKey(new Key(location, index));
    }

    byte[] get(String location, long index, Loader loader) throws IOException {
        Key key = new Key(location, index);
        synchronized (this) {
            byte[] block = blocks.get(key);
            if (block != null) {
                return block;
            }
        }
        CompletableFuture<byte[]> mine = new CompletableFuture<>();
        CompletableFuture<byte[]> existing = inFlight.putIfAbsent(key, mine);
        if (existing != null) {
            return await(existing);
        }
        try {
            byte[] block = loader.load();
            put(key, block);
            mine.complete(block);
            return block;
        } catch (IOException | RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key);
        }
    }

    synchronized void clear() {
        blocks.clear();
        currentBytes = 0;
    }

    synchronized long sizeInBytes() {
        return currentBytes;
    }

    private synchronized void put(Key key, byte[] block) {
        byte[] old = blocks.put(key, block);
        if (old != null) {
            currentBytes -= old.length;
        }
        currentBytes += block.length;
        var it = blocks.entrySet().iterator();
        while (currentBytes > maxBytes && it.hasNext()) {
            var eldest = it.next();
            if (eldest.getKey().equals(key)) {
                continue;
            }
            currentBytes -= eldest.getValue().length;
            it.remove();
        }
    }

    private static byte[] await(CompletableFuture<byte[]> future) throws IOException {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for a remote read", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioe) {
                throw ioe;
            }
            throw new IOException(cause.getMessage(), cause);
        }
    }
}
