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

import com.bc.ceres.annotation.STTM;

import com.bc.ceres.core.NullProgressMonitor;
import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.util.ProgressMonitorContext;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.image.ImageManager;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.io.File;
import java.util.Locale;

import static org.junit.Assert.assertTrue;

/**
 * Timing harness for the per-sample external-DEM lookup path used by terrain correction.
 * Prints ns per call; it never asserts on time.
 */
public class FileElevationModelTimingTest {

    private static final int SIZE = 256;

    @Test
    @STTM("SNAP-4391")
    public void timeHotPath() throws Exception {
        final File demFile = File.createTempFile("FileElevationModelTimingTest", ".tif");
        demFile.deleteOnExit();
        try {
            writeDem(demFile);
            final FileElevationModel dem = new FileElevationModel(demFile, ResamplingFactory.BILINEAR_INTERPOLATION_NAME, 0.0);
            dem.applyEarthGravitionalModel(false);
            final ProgressMonitor pm = new NullProgressMonitor();
            try (ProgressMonitorContext.Scope ignored = ProgressMonitorContext.use(pm)) {
                for (int y = 0; y < SIZE; y++) {
                    dem.getSample(0, y);     // load all lines (fits in the line cache)
                }
                final int[] x2 = new int[2], y2 = new int[2];
                final double[][] s2 = new double[2][2];
                final GeoPos geoPos = new GeoPos();
                final long n = 2_000_000;
                double getSamples = Double.MAX_VALUE, getSample = Double.MAX_VALUE, getElevation = Double.MAX_VALUE;
                double sink = 0;
                for (int round = 0; round < 6; round++) {
                    long t0 = System.nanoTime();
                    for (long k = 0; k < n; k++) {
                        final int x = (int) (k % (SIZE - 2)), y = (int) ((k >> 3) % (SIZE - 2));
                        x2[0] = x; x2[1] = x + 1; y2[0] = y; y2[1] = y + 1;
                        dem.getSamples(x2, y2, s2);
                        sink += s2[1][1];
                    }
                    getSamples = Math.min(getSamples, (System.nanoTime() - t0) / (double) n);

                    t0 = System.nanoTime();
                    for (long k = 0; k < n; k++) {
                        sink += dem.getSample(k % SIZE, (k >> 3) % SIZE);
                    }
                    getSample = Math.min(getSample, (System.nanoTime() - t0) / (double) n);

                    final long m = n / 10;    // getElevation is dominated by CrsGeoCoding.getPixelPos
                    t0 = System.nanoTime();
                    for (long k = 0; k < m; k++) {
                        geoPos.lat = 49.0 - (k % 200) * 0.05;
                        geoPos.lon = 11.0 + ((k >> 3) % 200) * 0.05;
                        sink += dem.getElevation(geoPos);
                    }
                    getElevation = Math.min(getElevation, (System.nanoTime() - t0) / (double) m);
                }
                System.out.printf(Locale.ROOT,
                        "FileElevationModel ns/call: getSamples(2x2)=%.1f getSample=%.1f getElevation(bilinear)=%.1f (sink %.0f)%n",
                        getSamples, getSample, getElevation, sink);
                assertTrue(sink > 0);
            } finally {
                dem.dispose();
            }
        } finally {
            demFile.delete();
        }
    }

    private static void writeDem(File file) throws Exception {
        final Product product = new Product("timing-dem", "GeoTIFF", SIZE, SIZE);
        final Band band = product.addBand("elevation", ProductData.TYPE_FLOAT32);
        final float[] data = new float[SIZE * SIZE];
        for (int i = 0; i < data.length; i++) {
            data[i] = 1 + (i % 977);
        }
        band.setDataElems(data);
        ImageManager.getInstance().getSourceImage(band, 0);
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, SIZE, SIZE, 10.0, 50.0, 0.05, 0.05));
        ProductIO.writeProduct(product, file, "GeoTIFF", false);
    }
}
