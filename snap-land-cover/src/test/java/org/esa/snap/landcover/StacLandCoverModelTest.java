/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.landcover;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.test.LongTestRunner;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.esa.snap.landcover.dataio.ESRILULC.ESRILULC2020ModelDescriptor;
import org.esa.snap.landcover.dataio.LandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.StacLandCoverModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.awt.*;

import static org.junit.Assert.assertEquals;

@RunWith(LongTestRunner.class)
public class StacLandCoverModelTest {
    private StacLandCoverModel model;
    private LandCoverModelDescriptor descriptor;
    private StacLandCoverModel stacLandCoverModel;
    private Resampling resamplingMethod;

    @Before
    public void setUp() {
        descriptor = new ESRILULC2020ModelDescriptor();
        resamplingMethod = Resampling.NEAREST_NEIGHBOUR;
        model = new StacLandCoverModel(descriptor, resamplingMethod);
        stacLandCoverModel = new StacLandCoverModel(descriptor, Resampling.NEAREST_NEIGHBOUR);
    }

    @After
    public void tearDown() {
        if (stacLandCoverModel != null) {
            stacLandCoverModel.dispose();
        }
    }

    @Test
    public void getDescriptorTest() {
        LandCoverModelDescriptor result = model.getDescriptor();

        // Assert
        assertEquals(descriptor, result);
    }

    @Test
    public void getResamplingTest() {
        Resampling result = model.getResampling();

        // Assert
        assertEquals(resamplingMethod, result);
    }

    @Test
    public void testGetLandCover_ValidLocation() throws Exception {
        final Product srcProduct = TestUtils.createProduct("test", 10, 10);
        Dimension rasterDim = new Dimension(srcProduct.getSceneRasterWidth(), srcProduct.getSceneRasterHeight());

        stacLandCoverModel.setAOIGeoCoding(srcProduct.getSceneGeoCoding(), rasterDim);

        final GeoPos point = srcProduct.getSceneGeoCoding().getGeoPos(new PixelPos(5,5), null);

        // Get the land cover value
        // This will trigger the search and download on the first call
        double landCoverValue = stacLandCoverModel.getLandCover(point);

        // Assert the result
        assertEquals("Land cover ", 8.0, landCoverValue, 0.0);
    }

    @Test
    @STTM("SNAP-4042")
    public void testGetLandCover_NoDataLocationInOcean() throws Exception {
        final Product srcProduct = TestUtils.createProduct("test", 10, 10);
        addGeoCoding(srcProduct);
        Dimension rasterDim = new Dimension(srcProduct.getSceneRasterWidth(), srcProduct.getSceneRasterHeight());

        stacLandCoverModel.setAOIGeoCoding(srcProduct.getSceneGeoCoding(), rasterDim);

        // Define a specific coordinate within that AOI
        final GeoPos ocean = new GeoPos(0.0, 0.0);

        // Get the land cover value
        double landCoverValue = stacLandCoverModel.getLandCover(ocean);

        // Assert the result
        // When no tiles are found for an AOI, the method should return 0
        assertEquals("Land cover for an ocean location should be the 'no data' value (0).", 0.0, landCoverValue, 0.0);
    }

    private static void addGeoCoding(final Product product) {
        product.removeTiePointGrid(product.getTiePointGrid(OperatorUtils.TPG_LATITUDE));
        product.removeTiePointGrid(product.getTiePointGrid(OperatorUtils.TPG_LONGITUDE));

        // Define a 4x4 grid of tie points centered around lat=0, lon=0. 🌊
        // This covers a 2x2 degree area from latitude -1 to 1 and longitude -1 to 1.

        // Latitude values for the 16 tie-points (4 rows x 4 columns).
        // Values are constant across each row, spanning from 1.0 to -1.0 degrees.
        final float[] latPoints = new float[]{
                1.0f, 1.0f, 1.0f, 1.0f,
                0.33f, 0.33f, 0.33f, 0.33f,
                -0.33f, -0.33f, -0.33f, -0.33f,
                -1.0f, -1.0f, -1.0f, -1.0f
        };

        // Longitude values for the 16 tie-points.
        // Values are constant down each column, spanning from -1.0 to 1.0 degrees.
        final float[] lonPoints = new float[]{
                -1.0f, -0.33f, 0.33f, 1.0f,
                -1.0f, -0.33f, 0.33f, 1.0f,
                -1.0f, -0.33f, 0.33f, 1.0f,
                -1.0f, -0.33f, 0.33f, 1.0f
        };

        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, 4, 4, 0.5f, 0.5f,
                product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                latPoints);

        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, 4, 4, 0.5f, 0.5f,
                product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                lonPoints);

        // The TiePointGeoCoding uses the two grids to map pixel positions to geo-coordinates.
        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(tpGeoCoding);
    }
}

