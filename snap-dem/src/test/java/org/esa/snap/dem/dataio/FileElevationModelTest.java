/*
 * Copyright (C) 2026 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.image.ImageManager;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Regression test for https://forum.step.esa.int/t/external-dem-is-sampled-half-a-pixel-off/46042
 * <p>
 * FileElevationModel.getElevation() obtains a pixel position via GeoCoding.getPixelPos(), which
 * already follows SNAP's pixel-center convention (integer coordinate == pixel corner,
 * +0.5 == pixel center). It must therefore be indexed with Resampling.computeIndex(), the same way
 * every other reader of a getPixelPos() result does (e.g. CDEMElevationModel). Calling
 * computeCornerBasedIndex() instead adds an extra, unwanted 0.5 pixel shift to the south-east.
 */
public class FileElevationModelTest {

    private static final double PIXEL_SIZE_DEG = 1.0;
    private static final double EASTING = 10.0;
    private static final double NORTHING = 50.0;
    private static final int RASTER_SIZE = 5;

    @Test
    @STTM("SNAP-4264")
    public void getElevationSamplesThePixelAtTheExactGeoPosition() throws Exception {
        final File demFile = File.createTempFile("FileElevationModelTest", ".tif");
        demFile.deleteOnExit();
        try {
            writeSyntheticDem(demFile);

            final FileElevationModel dem = new FileElevationModel(demFile,
                    ResamplingFactory.NEAREST_NEIGHBOUR_NAME, 0.0);
            // isolate the pixel-index logic from the (unrelated) EGM96 geoid correction
            dem.applyEarthGravitionalModel(false);
            try {
                // Exact center of pixel (col=3, row=2): lon = 13.0, lat = 48.0 (see writeSyntheticDem)
                final double elevation = dem.getElevation(new GeoPos(48.0, 13.0));

                assertEquals(valueAt(2, 3), elevation, 1.0e-9);
            } finally {
                dem.dispose();
            }
        } finally {
            demFile.delete();
        }
    }

    private static double valueAt(final int row, final int col) {
        return row * 10.0 + col;
    }

    private static void writeSyntheticDem(final File file) throws Exception {
        final Product product = new Product("synthetic-dem", "GeoTIFF", RASTER_SIZE, RASTER_SIZE);
        final Band band = product.addBand("elevation", ProductData.TYPE_FLOAT32);
        final float[] data = new float[RASTER_SIZE * RASTER_SIZE];
        for (int row = 0; row < RASTER_SIZE; row++) {
            for (int col = 0; col < RASTER_SIZE; col++) {
                data[row * RASTER_SIZE + col] = (float) valueAt(row, col);
            }
        }
        band.setDataElems(data);
        ImageManager.getInstance().getSourceImage(band, 0);

        // referencePixelX = referencePixelY = 0.5: the center of pixel (0,0) is placed at (EASTING, NORTHING).
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                RASTER_SIZE, RASTER_SIZE, EASTING, NORTHING, PIXEL_SIZE_DEG, PIXEL_SIZE_DEG));

        ProductIO.writeProduct(product, file, "GeoTIFF", false);
    }
}
