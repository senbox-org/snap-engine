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
package org.esa.snap.dem.dataio;

import org.esa.snap.core.util.SystemUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.esa.snap.dem.dataio.EarthGravitationalModel2008.NUM_LATS;
import static org.esa.snap.dem.dataio.EarthGravitationalModel2008.NUM_LONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EarthGravitationalModel2008Test {

    @ClassRule
    public static final TemporaryFolder tmp = new TemporaryFolder();

    private static EarthGravitationalModel2008 synthetic;

    /** Synthetic field in cm: linear in the row, smooth and periodic in the column. */
    private static int field(final int row, final int col) {
        return row + (int) Math.round(1000 * Math.sin(2 * Math.PI * col / NUM_LONS));
    }

    @BeforeClass
    public static void writeSyntheticGrid() throws IOException {
        synthetic = new EarthGravitationalModel2008(writeZip("synthetic.zip", NUM_LATS * NUM_LONS));
    }

    private static File writeZip(final String name, final int numValues) throws IOException {
        final File zip = new File(tmp.getRoot(), name);
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry(EarthGravitationalModel2008.NAME));
            final DataOutputStream data = new DataOutputStream(new BufferedOutputStream(out, 1 << 16));
            for (int i = 0; i < numValues; i++) {
                data.writeShort(field(i / NUM_LONS, i % NUM_LONS));
            }
            data.flush();
            out.closeEntry();
        }
        return zip;
    }

    private static double latOf(final double row) {
        return 90.0 - row / 12.0;
    }

    private static double lonOf(final double col) {
        return -180.0 + col / 12.0;
    }

    @Test
    public void testNodesAreReproducedExactly() {
        for (int row : new int[]{0, 1, 500, 1080, 2159, 2160}) {
            for (int col : new int[]{0, 1, 1234, 2160, NUM_LONS - 1}) {
                assertEquals("node " + row + "," + col, field(row, col) * 0.01f,
                        synthetic.getEGM(latOf(row), lonOf(col)), 1e-4f);
            }
        }
    }

    /** Bicubic (Catmull-Rom) interpolation reproduces a field that is linear in latitude. */
    @Test
    public void testLinearInLatitudeBetweenNodes() {
        final int col = 1000;
        for (double row = 2.25; row < 2150; row += 97.5) {
            final double expected = (row + Math.round(1000 * Math.sin(2 * Math.PI * col / NUM_LONS))) * 0.01;
            assertEquals("row " + row, expected, synthetic.getEGM(latOf(row), lonOf(col)), 1e-4);
        }
    }

    /** 180W and 180E are the same meridian; positive longitudes up to 360 are accepted too. */
    @Test
    public void testLongitudeWrapsAround() {
        for (double lat : new double[]{60.0, 0.04, -45.5}) {
            assertEquals(synthetic.getEGM(lat, -180.0), synthetic.getEGM(lat, 180.0), 1e-5f);
            assertEquals(synthetic.getEGM(lat, -179.99), synthetic.getEGM(lat, 180.01), 1e-5f);
            assertEquals(synthetic.getEGM(lat, -90.3), synthetic.getEGM(lat, 269.7), 1e-5f);
        }
    }

    @Test
    public void testPolesAreClamped() {
        assertEquals(field(0, 0) * 0.01f, synthetic.getEGM(90.0, -180.0), 1e-4f);
        assertEquals(field(NUM_LATS - 1, 0) * 0.01f, synthetic.getEGM(-90.0, -180.0), 1e-4f);
    }

    @After
    public void restoreSingleton() {
        EarthGravitationalModel2008.resetForTests(null, null);
    }

    private static File syntheticZip() {
        return new File(tmp.getRoot(), "synthetic.zip");
    }

    /** A fetcher that copies the synthetic grid into place, counting its calls. */
    private static EarthGravitationalModel2008.ZipFetcher copying(final AtomicInteger calls) {
        return target -> {
            calls.incrementAndGet();
            Files.copy(syntheticZip().toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return target;
        };
    }

    @Test
    public void testOversizedGridIsRejected() throws IOException {
        try {
            new EarthGravitationalModel2008(writeZip("long.zip", NUM_LATS * NUM_LONS + 1));
            fail("an oversized grid must be rejected");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("larger"));
        }
    }

    @Test
    public void testZipWithoutGridIsRejected() throws IOException {
        final File zip = new File(tmp.getRoot(), "empty.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("readme.txt"));
            out.closeEntry();
        }
        try {
            new EarthGravitationalModel2008(zip);
            fail("a zip without the grid must be rejected");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("does not contain"));
        }
    }

    @Test
    public void testMissingGridIsFetched() throws IOException {
        final File dir = tmp.newFolder();
        final AtomicInteger calls = new AtomicInteger();
        final EarthGravitationalModel2008 egm = EarthGravitationalModel2008.load(dir, copying(calls));
        assertEquals(1, calls.get());
        assertEquals(field(500, 7) * 0.01f, egm.node(500, 7), 1e-4f);
        // present now: loaded without fetching again
        EarthGravitationalModel2008.load(dir, copying(calls));
        assertEquals(1, calls.get());
    }

    /** An interrupted or damaged download is discarded and fetched once more instead of failing forever. */
    @Test
    public void testUnreadableLocalCopyIsFetchedAgain() throws IOException {
        final File dir = tmp.newFolder();
        Files.write(new File(dir, EarthGravitationalModel2008.ZIPNAME).toPath(), new byte[]{'P', 'K', 3, 4, 0});
        final AtomicInteger calls = new AtomicInteger();
        final EarthGravitationalModel2008 egm = EarthGravitationalModel2008.load(dir, copying(calls));
        assertEquals(1, calls.get());
        assertEquals(field(2000, 4000) * 0.01f, egm.node(2000, 4000), 1e-4f);
    }

    private static void assumeEGM96Available() {
        try {
            EarthGravitationalModel96.instance();
        } catch (IOException e) {
            Assume.assumeNoException("EGM96 auxdata unavailable", e);
        }
    }

    /** Without the EGM2008 grid the Copernicus tiles keep working with EGM96; no retry within the interval. */
    @Test
    public void testFallbackToEGM96IsCachedUntilTheRetryInterval() throws IOException {
        assumeEGM96Available();
        final AtomicInteger calls = new AtomicInteger();
        EarthGravitationalModel2008.resetForTests(tmp.newFolder(), target -> {
            calls.incrementAndGet();
            throw new IOException("offline");
        });
        assertSame(EarthGravitationalModel96.instance(), EarthGravitationalModel2008.instanceOrEGM96());
        assertSame(EarthGravitationalModel96.instance(), EarthGravitationalModel2008.instanceOrEGM96());
        assertEquals("download not retried within the interval", 1, calls.get());
    }

    /** A RuntimeException from the download (e.g. a response without Content-Length) must not break the DEM. */
    @Test
    public void testRuntimeFailureFallsBackAndIsCached() throws IOException {
        assumeEGM96Available();
        final AtomicInteger calls = new AtomicInteger();
        EarthGravitationalModel2008.resetForTests(tmp.newFolder(), target -> {
            calls.incrementAndGet();
            throw new IllegalArgumentException("Buffer size <= 0");
        });
        assertSame(EarthGravitationalModel96.instance(), EarthGravitationalModel2008.instanceOrEGM96());
        try {
            EarthGravitationalModel2008.instance();
            fail("instance() must report the cached failure");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Buffer size"));
        }
        assertEquals(1, calls.get());
    }

    /** A transient failure (offline on first use) is retried after the interval, and EGM2008 is then used. */
    @Test
    public void testFailedLoadIsRetriedAfterTheInterval() throws IOException {
        assumeEGM96Available();
        final AtomicInteger calls = new AtomicInteger();
        final EarthGravitationalModel2008.ZipFetcher copy = copying(new AtomicInteger());
        EarthGravitationalModel2008.resetForTests(tmp.newFolder(), target -> {
            if (calls.incrementAndGet() == 1) {
                throw new IOException("offline");
            }
            return copy.fetch(target);
        });
        assertSame(EarthGravitationalModel96.instance(), EarthGravitationalModel2008.instanceOrEGM96());
        assertEquals(1, calls.get());

        EarthGravitationalModel2008.retryIntervalMs = 0;     // the interval has passed
        final GeoidModel egm = EarthGravitationalModel2008.instanceOrEGM96();
        assertTrue(egm instanceof EarthGravitationalModel2008);
        assertEquals(2, calls.get());
        assertSame(egm, EarthGravitationalModel2008.instanceOrEGM96());
        assertEquals("loaded: no further downloads", 2, calls.get());
    }

    @Test
    public void testInstanceLoadsTheFetchedGridOnce() throws IOException {
        final AtomicInteger calls = new AtomicInteger();
        EarthGravitationalModel2008.resetForTests(tmp.newFolder(), copying(calls));
        final EarthGravitationalModel2008 egm = EarthGravitationalModel2008.instance();
        assertSame(egm, EarthGravitationalModel2008.instanceOrEGM96());
        assertEquals(field(1080, 0) * 0.01f, egm.getEGM(0.0, -180.0), 1e-4f);
        assertEquals(1, calls.get());
    }

    @Test
    public void testTruncatedGridIsRejected() throws IOException {
        try {
            new EarthGravitationalModel2008(writeZip("short.zip", NUM_LONS * 10));
            fail("a truncated grid must be rejected");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("smaller"));
        }
    }

    /**
     * The real grid against NGA's 2.5' EGM2008 values at those exact nodes (us_nga_egm08_25.tif), including the
     * global minimum and maximum. Runs when the grid is installed in the SNAP auxdata folder or given with
     * -Dsnap.dem.egm2008.zip=path.
     */
    @Test
    public void testRealGridMatchesNga() throws IOException {
        File zip = new File(System.getProperty("snap.dem.egm2008.zip", ""));
        if (!zip.isFile()) {
            zip = SystemUtils.getAuxDataPath().resolve("dem").resolve("egm2008")
                    .resolve(EarthGravitationalModel2008.ZIPNAME).toFile();
        }
        Assume.assumeTrue("EGM2008 grid not installed", zip.isFile());
        final EarthGravitationalModel2008 egm = new EarthGravitationalModel2008(zip);
        final Object[][] reference = {
                {"Everest", 28.000000, 86.916667, -28.3439},
                {"Aconcagua", -32.666667, -70.000000, 34.1794},
                {"Mont Blanc", 45.833333, 6.875000, 53.5772},
                {"Indian Ocean low", 4.708333, 78.791667, -106.9026},
                {"New Guinea high", -8.416667, 147.416667, 85.7352},
        };
        for (Object[] ref : reference) {
            assertEquals((String) ref[0], (double) ref[3], egm.getEGM((double) ref[1], (double) ref[2]), 0.05);
        }
    }
}
