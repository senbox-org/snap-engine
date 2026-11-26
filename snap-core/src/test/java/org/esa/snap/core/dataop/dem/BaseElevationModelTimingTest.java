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
package org.esa.snap.core.dataop.dem;

import com.bc.ceres.annotation.STTM;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.NullProgressMonitor;
import eu.esa.snap.core.util.ProgressMonitorContext;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.junit.Test;

import java.io.File;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Timing harness for the per-sample DEM lookup hot path (RangeDopplerGeocodingOp calls
 * {@code getSamples} with 2x2 / 4x4 arrays per output pixel). Prints ns per call; it asserts
 * only on correctness, never on time, so it cannot be flaky.
 */
public class BaseElevationModelTimingTest {

    private static final int TILE = 256;

    @Test
    @STTM("SNAP-4391")
    public void timeHotPath() throws Exception {
        final InMemoryModel model = new InMemoryModel(Resampling.BILINEAR_INTERPOLATION);
        try {
            final ProgressMonitor pm = new NullProgressMonitor();   // real monitor, like GPF binds
            try (ProgressMonitorContext.Scope ignored = ProgressMonitorContext.use(pm)) {
                final double[][] s2 = new double[2][2];
                final double[][] s4 = new double[4][4];
                final int[] x2 = new int[2], y2 = new int[2], x4 = new int[4], y4 = new int[4];
                final GeoPos geoPos = new GeoPos();

                // touch every line once so the timed loops measure the cached path
                for (int y = 0; y < 2 * TILE; y++) {
                    model.getSample(0, y);
                    model.getSample(TILE, y);
                }

                final long n = 2_000_000;
                double getSamples2 = Double.MAX_VALUE, getSamples4 = Double.MAX_VALUE;
                double getSample = Double.MAX_VALUE, getElevation = Double.MAX_VALUE;
                double sink = 0;
                for (int round = 0; round < 6; round++) {
                    long t0 = System.nanoTime();
                    for (long k = 0; k < n; k++) {
                        final int x = (int) (k % (2 * TILE - 2)), y = (int) ((k >> 3) % (2 * TILE - 2));
                        x2[0] = x; x2[1] = x + 1; y2[0] = y; y2[1] = y + 1;
                        model.getSamples(x2, y2, s2);
                        sink += s2[1][1];
                    }
                    getSamples2 = Math.min(getSamples2, (System.nanoTime() - t0) / (double) n);

                    t0 = System.nanoTime();
                    for (long k = 0; k < n; k++) {
                        final int x = (int) (k % (2 * TILE - 4)), y = (int) ((k >> 3) % (2 * TILE - 4));
                        for (int q = 0; q < 4; q++) {
                            x4[q] = x + q;
                            y4[q] = y + q;
                        }
                        model.getSamples(x4, y4, s4);
                        sink += s4[3][3];
                    }
                    getSamples4 = Math.min(getSamples4, (System.nanoTime() - t0) / (double) n);

                    t0 = System.nanoTime();
                    for (long k = 0; k < n; k++) {
                        sink += model.getSample(k % (2 * TILE), (k >> 3) % (2 * TILE));
                    }
                    getSample = Math.min(getSample, (System.nanoTime() - t0) / (double) n);

                    t0 = System.nanoTime();
                    for (long k = 0; k < n; k++) {
                        geoPos.lat = 1.5 - (k % 1000) * 0.0015;
                        geoPos.lon = 0.1 + ((k >> 3) % 1000) * 0.0015;
                        sink += model.getElevation(geoPos);
                    }
                    getElevation = Math.min(getElevation, (System.nanoTime() - t0) / (double) n);
                }
                System.out.printf(Locale.ROOT,
                        "BaseElevationModel ns/call: getSamples(2x2)=%.1f getSamples(4x4)=%.1f getSample=%.1f getElevation(bilinear)=%.1f (sink %.0f)%n",
                        getSamples2, getSamples4, getSample, getElevation, sink);
                assertTrue(sink > 0);
            }
        } finally {
            model.dispose();
        }
    }

    @Test
    @STTM("SNAP-4391")
    public void samplesAreCorrect() throws Exception {
        final InMemoryModel model = new InMemoryModel(Resampling.NEAREST_NEIGHBOUR);
        try {
            final double[][] s = new double[2][2];
            assertTrue(model.getSamples(new int[]{3, TILE + 4}, new int[]{5, TILE + 6}, s));
            assertEquals(value(3, 5), s[0][0], 0);
            assertEquals(value(TILE + 4, 5), s[0][1], 0);
            assertEquals(value(3, TILE + 6), s[1][0], 0);
            assertEquals(value(TILE + 4, TILE + 6), s[1][1], 0);
            assertEquals(value(7, 9), model.getSample(7, 9), 0);
        } finally {
            model.dispose();
        }
    }

    @Test
    @STTM("SNAP-4391")
    public void tileLoadStillHonoursCancellation() {
        final InMemoryModel model = new InMemoryModel(Resampling.NEAREST_NEIGHBOUR);
        try {
            final ProgressMonitor pm = new NullProgressMonitor();
            pm.setCanceled(true);
            try (ProgressMonitorContext.Scope ignored = ProgressMonitorContext.use(pm)) {
                // no tile loaded yet: the load path must observe the cancellation
                assertThrows(java.util.concurrent.CancellationException.class,
                             () -> model.getSamples(new int[]{0, 1}, new int[]{0, 1}, new double[2][2]));
            }
        } finally {
            model.dispose();
        }
    }

    private static float value(int x, int y) {
        return 1 + x + 1000 * y;
    }

    private static final class InMemoryFile extends ElevationFile {
        private final BaseElevationModel model;
        private final int tx, ty;

        InMemoryFile(BaseElevationModel model, int tx, int ty) {
            super(new File(System.getProperty("java.io.tmpdir"), "mem_" + tx + "_" + ty + ".tif"), null);
            this.model = model;
            this.tx = tx;
            this.ty = ty;
        }

        @Override
        protected boolean findLocalFile() {
            return true;
        }

        @Override
        protected void getLocalFile() {
            final Product product = new Product("t", "t", TILE, TILE);
            final Band band = product.addBand("elevation", ProductData.TYPE_FLOAT32);
            final float[] data = new float[TILE * TILE];
            for (int y = 0; y < TILE; y++) {
                for (int x = 0; x < TILE; x++) {
                    data[y * TILE + x] = value(tx * TILE + x, ty * TILE + y);
                }
            }
            band.setDataElems(data);
            tile = new BaseElevationTile(model, product);
            model.updateCache(tile);
        }

        @Override
        protected Boolean getRemoteFile() {
            return Boolean.FALSE;
        }
    }

    private static final class InMemoryModel extends BaseElevationModel {
        InMemoryModel(Resampling resampling) {
            super(new Descriptor(), resampling);
        }

        @Override
        protected void createElevationFile(ElevationFile[][] files, int x, int y, File demInstallDir) {
            files[x][y] = new InMemoryFile(this, x, y);
        }

        @Override
        public double getIndexX(GeoPos geoPos) {
            return geoPos.lon * TILE;
        }

        @Override
        public double getIndexY(GeoPos geoPos) {
            return (2 - geoPos.lat) * TILE;
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos) {
            return new GeoPos(2 - pixelPos.y / TILE, pixelPos.x / TILE);
        }
    }

    private static final class Descriptor implements ElevationModelDescriptor {
        @Override public String getName() { return "InMemory"; }
        @Override public float getNoDataValue() { return -32768f; }
        @Override public int getRasterWidth() { return 2 * TILE; }
        @Override public int getRasterHeight() { return 2 * TILE; }
        @Override public int getTileWidthInDegrees() { return 1; }
        @Override public int getTileWidth() { return TILE; }
        @Override public int getNumXTiles() { return 2; }
        @Override public int getNumYTiles() { return 2; }
        @Override public ElevationModel createDem(Resampling resampling) { return null; }
        @Override public boolean canBeDownloaded() { return false; }
        @Override public File getDemInstallDir() { return new File("."); }
    }
}
