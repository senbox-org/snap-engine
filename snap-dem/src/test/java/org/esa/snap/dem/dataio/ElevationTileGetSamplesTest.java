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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dem.dataio.cdem.CDEMElevationModel;
import org.esa.snap.dem.dataio.cdem.CDEMElevationModelDescriptor;
import org.esa.snap.dem.dataio.cdem.CDEMElevationTile;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationTile;
import org.esa.snap.dem.dataio.copernicus.copernicus30m.Copernicus30mElevationModel;
import org.esa.snap.dem.dataio.copernicus.copernicus30m.Copernicus30mElevationModelDescriptor;
import org.esa.snap.runtime.Config;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;

/**
 * {@code getSamples(x, y, samples)} fills {@code samples[row][column]} with the heights at columns {@code x} and
 * rows {@code y}, which is how the bilinear and bicubic resamplers read their neighbourhood. The CDEM and
 * Copernicus tiles indexed it as {@code getSample(x[i], y[j])}, swapping columns and rows: CDEM interpolated from
 * a transposed neighbourhood, and lists of different lengths read the wrong pixels or failed.
 */
public class ElevationTileGetSamplesTest {

    private static final int W = 5, H = 3;
    private static final String EGM_KEY = "snap.useDEMGravitationalModel";

    private String savedEgmPref;

    @Before
    public void rawHeights() {
        // compare raw heights: no geoid added
        final Preferences prefs = Config.instance().preferences();
        savedEgmPref = prefs.get(EGM_KEY, null);
        prefs.putBoolean(EGM_KEY, false);
        try {
            EarthGravitationalModel96.instance();       // both tiles load a geoid when they are created
        } catch (IOException e) {
            Assume.assumeNoException("EGM96 auxdata unavailable", e);
        }
    }

    @After
    public void restore() {
        final Preferences prefs = Config.instance().preferences();
        if (savedEgmPref == null) {
            prefs.remove(EGM_KEY);
        } else {
            prefs.put(EGM_KEY, savedEgmPref);
        }
    }

    private static float height(final int x, final int y) {
        return 100 + 10 * x + y;
    }

    /** A one-band product whose height identifies the pixel. */
    private static Product tileProduct() {
        final Product p = new Product("tile", "DEM", W, H);
        final float[] data = new float[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                data[y * W + x] = height(x, y);
            }
        }
        final Band b = new Band("elevation", ProductData.TYPE_FLOAT32, W, H);
        b.setRasterData(ProductData.createInstance(data));
        p.addBand(b);
        return p;
    }

    private static void assertRowsAndColumns(final Resampling.Raster tile) throws Exception {
        final int[] x = {1, 3, 4};
        final int[] y = {0, 2};
        final double[][] samples = new double[y.length][x.length];
        tile.getSamples(x, y, samples);
        for (int i = 0; i < y.length; i++) {
            for (int j = 0; j < x.length; j++) {
                assertEquals("row " + y[i] + " column " + x[j], height(x[j], y[i]), samples[i][j], 0.0);
            }
        }
    }

    @Test
    public void testCDEMTile() throws Exception {
        final CDEMElevationModel dem = new CDEMElevationModel(new CDEMElevationModelDescriptor(),
                Resampling.BILINEAR_INTERPOLATION);
        assertRowsAndColumns(new CDEMElevationTile(dem, tileProduct()));
    }

    @Test
    public void testCopernicusTile() throws Exception {
        final Copernicus30mElevationModel dem = new Copernicus30mElevationModel(
                new Copernicus30mElevationModelDescriptor(), Resampling.BILINEAR_INTERPOLATION);
        assertRowsAndColumns(new CopernicusElevationTile(dem, tileProduct()));
    }
}
