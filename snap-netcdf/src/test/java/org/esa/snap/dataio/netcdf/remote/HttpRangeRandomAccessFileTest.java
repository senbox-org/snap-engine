package org.esa.snap.dataio.netcdf.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Serves a NetCDF-4 test file the way NASA Earthdata Cloud does: an archive URL on one host that
 * checks a bearer token and redirects to a signed URL on another host, which serves byte ranges.
 * "localhost" and "127.0.0.1" stand in for the two hosts.
 */
public class HttpRangeRandomAccessFileTest {

    private static final String TOKEN = "Bearer test-token";
    private static final Pattern RANGE = Pattern.compile("bytes=(\\d+)-(\\d+)");

    private HttpServer server;
    private byte[] content;
    private File localFile;
    private int port;
    private final AtomicInteger signedVersion = new AtomicInteger(1);
    private final AtomicInteger rangeRequests = new AtomicInteger();
    private final AtomicBoolean authLeakedToStorage = new AtomicBoolean();
    private final AtomicBoolean ignoreRanges = new AtomicBoolean();
    private final RemoteAuthProvider auth = new RemoteAuthProvider() {
        @Override
        public boolean handles(URI uri) {
            return "localhost".equals(uri.getHost());
        }

        @Override
        public String getAuthorizationHeader(URI uri) {
            return TOKEN;
        }
    };

    @Before
    public void setUp() throws Exception {
        localFile = new File(getClass().getResource("/org/esa/snap/dataio/netcdf/metadata/profiles/cf/tileSizeTest_nc4CF.nc").toURI());
        content = Files.readAllBytes(localFile.toPath());
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/archive/", this::archive);
        server.createContext("/signed/", this::signed);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        port = server.getAddress().getPort();
        RemoteFileAccess.clearCache();
        RemoteFileAccess.registerAuthProvider(auth);
    }

    @After
    public void tearDown() {
        RemoteFileAccess.unregisterAuthProvider(auth);
        RemoteFileAccess.clearCache();
        server.stop(0);
    }

    @Test
    public void testIsRemoteLocation() {
        assertTrue(RemoteFileAccess.isRemoteLocation("https://host/a.nc"));
        assertTrue(RemoteFileAccess.isRemoteLocation(" HTTP://host/a.nc"));
        assertTrue(RemoteFileAccess.isRemoteLocation(URI.create("https://host/a.nc")));
        assertFalse(RemoteFileAccess.isRemoteLocation("/data/a.nc"));
        assertFalse(RemoteFileAccess.isRemoteLocation(new File("a.nc")));
        assertEquals("a.nc", RemoteFileAccess.getFileName("https://host/bucket/a.nc?sig=1"));
    }

    @Test
    public void testOpenerReadsSameDataAsLocalFile() throws Exception {
        try (NetcdfFile remote = NetcdfFileOpener.open(archiveUrl());
             NetcdfFile local = NetcdfFileOpener.open(localFile)) {
            assertNotNull(remote);
            assertEquals(archiveUrl(), remote.getLocation());
            for (String name : new String[]{"B1", "lat", "lon"}) {
                assertSameValues(name, local.findVariable(name).read(), remote.findVariable(name).read());
            }
        }
        assertFalse("credentials must not reach the storage host", authLeakedToStorage.get());
    }

    @Test
    public void testSmallBlocksAreFetchedAndAssembledCorrectly() throws Exception {
        try (NetcdfFile remote = NetcdfFiles.open(new HttpRangeRandomAccessFile(archiveUrl(), 4096), archiveUrl(), null, null);
             NetcdfFile local = NetcdfFileOpener.open(localFile)) {
            assertSameValues("B1", local.findVariable("B1").read(), remote.findVariable("B1").read());
        }
        long blocks = (localFile.length() + 4095) / 4096;
        assertTrue("consecutive blocks should share requests: " + rangeRequests.get() + " requests for " + blocks + " blocks",
                rangeRequests.get() > 0 && rangeRequests.get() < blocks / 4);
    }

    @Test
    public void testReadsAcrossCachedAndMissingBlocksReturnTheFileBytes() throws Exception {
        byte[] expected = java.nio.file.Files.readAllBytes(localFile.toPath());
        java.util.Random random = new java.util.Random(42);
        try (HttpRangeRandomAccessFile raf = new HttpRangeRandomAccessFile(archiveUrl(), 1024)) {
            for (int i = 0; i < 400; i++) {
                int pos = random.nextInt(expected.length - 1);
                int len = 1 + random.nextInt(Math.min(20000, expected.length - pos));
                byte[] actual = new byte[len];
                raf.seek(pos);
                raf.readFully(actual);
                assertArrayEquals("bytes " + pos + "+" + len, java.util.Arrays.copyOfRange(expected, pos, pos + len), actual);
            }
        }
    }

    @Test
    public void testConcurrentHandlesShareBlocksAndReadCorrectData() throws Exception {
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(8);
        try (NetcdfFile local = NetcdfFileOpener.open(localFile)) {
            ucar.ma2.Array expected = local.findVariable("B1").read();
            java.util.List<java.util.concurrent.Future<ucar.ma2.Array>> reads = new java.util.ArrayList<>();
            for (int i = 0; i < 16; i++) {
                reads.add(pool.submit(() -> {
                    try (NetcdfFile remote = NetcdfFiles.open(new HttpRangeRandomAccessFile(archiveUrl(), 1024), archiveUrl(), null, null)) {
                        return remote.findVariable("B1").read();
                    }
                }));
            }
            for (java.util.concurrent.Future<ucar.ma2.Array> read : reads) {
                assertSameValues("B1", expected, read.get());
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void testSecondOpenReusesCachedBlocks() throws Exception {
        try (NetcdfFile first = NetcdfFileOpener.open(archiveUrl())) {
            first.findVariable("B1").read();
        }
        int afterFirst = rangeRequests.get();
        try (NetcdfFile second = NetcdfFileOpener.open(archiveUrl())) {
            second.findVariable("B1").read();
        }
        // the second open reuses both the resolved signed URL and the cached blocks
        assertEquals(afterFirst, rangeRequests.get());
    }

    @Test
    public void testPrefetchVariablesLoadsAllChunksBeforeTheRead() throws Exception {
        // lat/lon are stored as 150 gzip chunks each; small blocks make every chunk a separate fetch
        // open with the classic H5iosp, as NetcdfFileOpener does (NetcdfFiles.open would pick H5iospNew)
        try (NetcdfFile remote = new NetcdfFile(new ucar.nc2.iosp.hdf5.H5iosp(), new HttpRangeRandomAccessFile(archiveUrl(), 1024), archiveUrl(), null) {
        };
             NetcdfFile local = NetcdfFileOpener.open(localFile)) {
            assertFalse(ChunkLocator.chunkRanges(remote.findVariable("lat")).isEmpty());
            prefetchWithBlockSize(remote, 1024, "lat", "lon");
            int afterPrefetch = rangeRequests.get();
            assertSameValues("lat", local.findVariable("lat").read(), remote.findVariable("lat").read());
            assertSameValues("lon", local.findVariable("lon").read(), remote.findVariable("lon").read());
            assertEquals("reads after the prefetch should all be cache hits", afterPrefetch, rangeRequests.get());
        }
    }

    @Test
    public void testWarmedChunkIndexIsWalkedWithoutRequests() throws Exception {
        try (NetcdfFile remote = new NetcdfFile(new ucar.nc2.iosp.hdf5.H5iosp(), new HttpRangeRandomAccessFile(archiveUrl(), 1024), archiveUrl(), null) {
        }) {
            java.util.List<ucar.nc2.Variable> variables = java.util.List.of(remote.findVariable("lat"), remote.findVariable("lon"));
            int before = rangeRequests.get();
            try (HttpRangeRandomAccessFile raf = new HttpRangeRandomAccessFile(archiveUrl(), 1024)) {
                ChunkLocator.warmIndexes(variables, raf);
            }
            int afterWarm = rangeRequests.get();
            assertTrue("the index nodes should have been fetched", afterWarm > before);
            assertEquals(150, ChunkLocator.chunkRanges(variables.get(0)).size());
            assertEquals(150, ChunkLocator.chunkRanges(variables.get(1)).size());
            assertEquals("walking a warmed index needs no requests", afterWarm, rangeRequests.get());
        }
    }

    @Test
    public void testChunkRangesOfASliceCoverOnlyTheSlice() throws Exception {
        try (NetcdfFile local = NetcdfFileOpener.open(localFile)) {
            ucar.nc2.Variable lat = local.findVariable("lat"); // 100 x 150 in 10 x 10 chunks
            assertEquals(150, ChunkLocator.chunkRanges(lat).size());
            assertEquals("one row of chunks", 15, ChunkLocator.chunkRanges(lat.slice(0, 5)).size());
            assertEquals("one column of chunks", 10, ChunkLocator.chunkRanges(lat.slice(1, 42)).size());
            assertTrue("nested slices are skipped", ChunkLocator.chunkRanges(lat.slice(0, 5).slice(0, 3)).isEmpty());
        }
    }

    @Test
    public void testPrefetchVariablesIgnoresLocalFilesAndUnchunkedVariables() throws Exception {
        try (NetcdfFile local = NetcdfFileOpener.open(localFile)) {
            RemoteFileAccess.prefetchVariables(local, java.util.List.of(local.findVariable("lat")));
            assertTrue(ChunkLocator.chunkRanges(local.findVariable("B1")).isEmpty());
        }
        assertEquals(0, rangeRequests.get());
    }

    private static void prefetchWithBlockSize(NetcdfFile ncfile, int blockSize, String... names) throws IOException {
        java.util.List<ChunkLocator.Range> ranges = new java.util.ArrayList<>();
        for (String name : names) {
            ranges.addAll(ChunkLocator.chunkRanges(ncfile.findVariable(name)));
        }
        try (HttpRangeRandomAccessFile raf = new HttpRangeRandomAccessFile(ncfile.getLocation(), blockSize)) {
            raf.prefetchRanges(ranges);
        }
    }

    @Test
    public void testExpiredSignedUrlIsResolvedAgain() throws Exception {
        try (HttpRangeRandomAccessFile raf = new HttpRangeRandomAccessFile(archiveUrl(), 4096)) {
            byte[] head = new byte[8];
            raf.seek(0);
            raf.readFully(head);
            signedVersion.incrementAndGet(); // the old signature now returns 403
            byte[] tail = new byte[100];
            raf.seek(content.length - 100);
            raf.readFully(tail);
            assertArrayEquals(java.util.Arrays.copyOfRange(content, content.length - 100, content.length), tail);
        }
    }

    @Test
    public void testMissingCredentialsGiveClearError() {
        RemoteFileAccess.unregisterAuthProvider(auth);
        IOException e = assertThrows(IOException.class, () -> RemoteFileAccess.open(archiveUrl()));
        assertTrue(e.getMessage(), e.getMessage().contains("401"));
    }

    @Test
    public void testServerWithoutRangeSupportIsRejected() {
        ignoreRanges.set(true);
        IOException e = assertThrows(IOException.class, () -> RemoteFileAccess.open(archiveUrl()));
        assertTrue(e.getMessage(), e.getMessage().contains("byte-range"));
    }

    private static void assertSameValues(String name, Array expected, Array actual) {
        assertEquals(name, expected.getSize(), actual.getSize());
        for (int i = 0; i < expected.getSize(); i++) {
            assertEquals(name + "[" + i + "]", expected.getDouble(i), actual.getDouble(i), 0.0);
        }
    }

    private String archiveUrl() {
        return "http://localhost:" + port + "/archive/test.nc";
    }

    private void archive(HttpExchange ex) throws IOException {
        if (!TOKEN.equals(ex.getRequestHeaders().getFirst("Authorization"))) {
            respond(ex, 401, new byte[0]);
            return;
        }
        ex.getResponseHeaders().set("Location", "http://127.0.0.1:" + port + "/signed/test.nc?sig=" + signedVersion.get());
        respond(ex, 303, new byte[0]);
    }

    private void signed(HttpExchange ex) throws IOException {
        if (ex.getRequestHeaders().containsKey("Authorization")) {
            authLeakedToStorage.set(true);
        }
        if (!("sig=" + signedVersion.get()).equals(ex.getRequestURI().getQuery())) {
            respond(ex, 403, new byte[0]);
            return;
        }
        Matcher m = RANGE.matcher(String.valueOf(ex.getRequestHeaders().getFirst("Range")));
        if (ignoreRanges.get() || !m.matches()) {
            respond(ex, 200, content);
            return;
        }
        rangeRequests.incrementAndGet();
        int start = Integer.parseInt(m.group(1));
        int end = Math.min(Integer.parseInt(m.group(2)), content.length - 1);
        ex.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + content.length);
        respond(ex, 206, java.util.Arrays.copyOfRange(content, start, end + 1));
    }

    private static void respond(HttpExchange ex, int status, byte[] body) throws IOException {
        ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        }
        ex.close();
    }
}
