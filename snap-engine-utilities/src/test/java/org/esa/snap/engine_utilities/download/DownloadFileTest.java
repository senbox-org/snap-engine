/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.engine_utilities.download;

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link DownloadableContentImpl#downloadFile} against a local HTTP server: responses with and without
 * Content-Length, a response that ends early, and a missing file. A finished download appears under its final
 * name only when complete, and no temporary file is left behind.
 */
public class DownloadFileTest {

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    private static final byte[] PAYLOAD = new byte[100_000];

    static {
        for (int i = 0; i < PAYLOAD.length; i++) {
            PAYLOAD[i] = (byte) (i * 31);
        }
    }

    private HttpServer server;
    private String base;

    @Before
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        // fixed length
        server.createContext("/sized/", ex -> {
            ex.sendResponseHeaders(200, PAYLOAD.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(PAYLOAD);
            }
        });
        // chunked: no Content-Length, which used to fail with "Buffer size <= 0"
        server.createContext("/chunked/", ex -> {
            ex.sendResponseHeaders(200, 0);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(PAYLOAD);
            }
        });
        // announces the full length but sends half, then closes the connection
        server.createContext("/truncated/", ex -> {
            ex.sendResponseHeaders(200, PAYLOAD.length);
            final OutputStream os = ex.getResponseBody();
            os.write(PAYLOAD, 0, PAYLOAD.length / 2);
            os.flush();
            ex.close();     // closes the connection: too few bytes for the announced length
        });
        server.createContext("/missing/", ex -> {
            ex.sendResponseHeaders(404, -1);
            ex.close();
        });
        server.start();
        base = "http://" + server.getAddress().getHostString() + ':' + server.getAddress().getPort();
    }

    @After
    public void stopServer() {
        server.stop(0);
    }

    private File download(final String context, final File dir) throws IOException {
        return DownloadableContentImpl.downloadFile(new URL(base + context + "grid.zip"), new File(dir, "grid.zip"));
    }

    private static void assertNoPartFiles(final File dir) {
        final String[] names = dir.list();
        assertTrue(Arrays.toString(names),
                names == null || Arrays.stream(names).noneMatch(n -> n.endsWith(".part")));
    }

    @Test
    public void testDownloadWithContentLength() throws IOException {
        final File dir = tmp.newFolder();
        final File file = download("/sized/", dir);
        assertEquals(new File(dir, "grid.zip"), file);
        assertArrayEquals(PAYLOAD, Files.readAllBytes(file.toPath()));
        assertNoPartFiles(dir);
    }

    @Test
    public void testDownloadWithoutContentLength() throws IOException {
        final File dir = tmp.newFolder();
        final File file = download("/chunked/", dir);
        assertArrayEquals(PAYLOAD, Files.readAllBytes(file.toPath()));
        assertNoPartFiles(dir);
    }

    @Test
    public void testDownloadReplacesAnExistingFile() throws IOException {
        final File dir = tmp.newFolder();
        Files.write(new File(dir, "grid.zip").toPath(), new byte[]{1, 2, 3});
        final File file = download("/sized/", dir);
        assertArrayEquals(PAYLOAD, Files.readAllBytes(file.toPath()));
        assertNoPartFiles(dir);
    }

    /** A download cut short must fail and leave nothing under the final name. */
    @Test
    public void testTruncatedDownloadFailsAndLeavesNoFile() throws IOException {
        final File dir = tmp.newFolder();
        try {
            download("/truncated/", dir);
            fail("a truncated download must fail");
        } catch (IOException expected) {
            // the JDK may report the early end itself, or the length check does
        }
        assertFalse(new File(dir, "grid.zip").exists());
        assertNoPartFiles(dir);
    }

    @Test
    public void testMissingRemoteFileGivesNull() throws IOException {
        final File dir = tmp.newFolder();
        assertNull(DownloadableContentImpl.getRemoteHttpFile(new URL(base + "/missing/"), new File(dir, "grid.zip")));
        assertFalse(new File(dir, "grid.zip").exists());
        assertNoPartFiles(dir);
    }
}
