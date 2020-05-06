/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.TiePointGeoCodingLongTest.TestSet;
import org.esa.snap.core.transform.AffineTransform2D;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.Debug;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.esa.snap.core.datamodel.TiePointGeoCodingLongTest.createMerisRRTestSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TiePointGeoCodingTest {
    private static final int S = 4;
    private static final int GW = 3;
    private static final int GH = 5;
    private static final int PW = (GW - 1) * S + 1;
    private static final int PH = (GH - 1) * S + 1;
    private static final float LAT_1 = 53.0f;
    private static final float LAT_2 = 50.0f;
    private static final float LON_1 = 10.0f;
    private static final float LON_2 = 15.0f;

    @Test
    public void testSelf() {
        TestSet ts = createMerisRRTestSet(0, +180, 0, true);
        assertEquals(+170, ts.gp[TestSet.UL].lon, 1.e-5f);
        assertEquals(+180, ts.gp[TestSet.UC].lon, 1.e-5f);
        assertEquals(-170, ts.gp[TestSet.UR].lon, 1.e-5f);
    }

    @Test
    public void testTransferGeoCoding() {
        final Scene srcScene = SceneFactory.createScene(createProduct());
        final Scene destScene = SceneFactory.createScene(new Product("test2", "test2", PW, PH));

        final boolean transferred = srcScene.transferGeoCodingTo(destScene, null);
        assertTrue(transferred);
        final GeoCoding destGeoCoding = destScene.getGeoCoding();
        assertNotNull(destGeoCoding);
        assertTrue(destGeoCoding instanceof TiePointGeoCoding);

        final PixelPos pixelPos = new PixelPos(PW/2.0f, PH/2.0f);
        final GeoPos srcGeoPos = srcScene.getGeoCoding().getGeoPos(pixelPos, null);
        final GeoPos destGeoPos = destScene.getGeoCoding().getGeoPos(pixelPos, null);
        assertEquals(srcGeoPos, destGeoPos);
    }

    @Test
    public void testTransferGeoCoding_nonStandardGridParameters() {
        final Scene srcScene = SceneFactory.createScene(createProduct(-1.5f, 2.5f, 2.5f, 2.5f));
        final Scene destScene = SceneFactory.createScene(new Product("test2", "test2", PW + 2, PH - 2));

        final boolean transferred = srcScene.transferGeoCodingTo(destScene, null);
        assertTrue(transferred);
        final GeoCoding destGeoCoding = destScene.getGeoCoding();
        assertNotNull(destGeoCoding);
        assertTrue(destGeoCoding instanceof TiePointGeoCoding);

        final PixelPos pixelPos = new PixelPos(PW/2.0f, PH/2.0f);
        final GeoPos srcGeoPos = srcScene.getGeoCoding().getGeoPos(pixelPos, null);
        final GeoPos destGeoPos = destScene.getGeoCoding().getGeoPos(pixelPos, null);
        assertEquals(srcGeoPos, destGeoPos);
    }

    @Test
    public void testTransferGeoCoding_WithSpatialSubset() throws IOException {
        final Scene srcScene = SceneFactory.createScene(createProduct());
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setSubsetRegion(new PixelSubsetRegion(2, 2, PW - 4, PH - 4, 0));
        subsetDef.setSubSampling(1,2);
        final Product destProduct = ProductSubsetBuilder.createProductSubset(new Product("test2", "test2", PW, PH),
                                                                             subsetDef, "test2", "");
        final Scene destScene = SceneFactory.createScene(destProduct);

        final boolean transferred = srcScene.transferGeoCodingTo(destScene, subsetDef);
        assertTrue(transferred);
        final GeoCoding destGeoCoding = destScene.getGeoCoding();
        assertTrue(destGeoCoding instanceof TiePointGeoCoding);

        final GeoPos srcGeoPos = srcScene.getGeoCoding().getGeoPos(new PixelPos(4.5f, 6.5f), null);
        final PixelPos destPixelPos = destScene.getGeoCoding().getPixelPos(srcGeoPos, null);
        assertEquals(2.06, destPixelPos.getX(), 1.0e-2);
        assertEquals(4.42, destPixelPos.getY(), 1.0e-1);
    }

    @Test
    public void testDetermineWarpParameters() {
        int[] warpParameters = TiePointGeoCoding.determineWarpParameters(100, 100);
        assertEquals(25, warpParameters[0]);
        assertEquals(34, warpParameters[1]);
        assertEquals(4, warpParameters[2]);
        assertEquals(3, warpParameters[3]);

        warpParameters = TiePointGeoCoding.determineWarpParameters(39, 2728);
        assertEquals(20, warpParameters[0]);
        assertEquals(39, warpParameters[1]);
        assertEquals(2, warpParameters[2]);
        assertEquals(70, warpParameters[3]);
    }

    @Test
    public void testCanClone() {
        final Product product = createProduct();
        final GeoCoding geoCoding = product.getSceneGeoCoding();

        assertTrue(geoCoding.canClone());
    }

    @Test
    public void testClone() {
        final Product product = createProduct();
        final GeoCoding geoCoding = product.getSceneGeoCoding();

        final PixelPos pixelPos = new PixelPos(4, 3);

        GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
        assertEquals(11.11328125, geoPos.lon, 1e-8);
        assertEquals(52.33203172683716, geoPos.lat, 1e-8);

        final GeoCoding clone = geoCoding.clone();
        geoPos = clone.getGeoPos(pixelPos, null);
        assertEquals(11.11328125, geoPos.lon, 1e-8);
        assertEquals(52.33203172683716, geoPos.lat, 1e-8);
    }

    @Test
    public void testClone_disposeOriginal() {
        final Product product = createProduct();
        final GeoCoding geoCoding = product.getSceneGeoCoding();

        final PixelPos pixelPos = new PixelPos(4, 3);

        GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
        assertEquals(11.11328125, geoPos.lon, 1e-8);
        assertEquals(52.33203172683716, geoPos.lat, 1e-8);

        final GeoCoding clone = geoCoding.clone();
        geoCoding.dispose();

        geoPos = clone.getGeoPos(pixelPos, null);
        assertEquals(11.11328125, geoPos.lon, 1e-8);
        assertEquals(52.33203172683716, geoPos.lat, 1e-8);

        clone.dispose();
    }

    private Product createProduct() {
        return createProduct(0.5f, 0.5f, S , S);
    }

    private Product createProduct(float tiePointGridOffsetX, float tiePointGridOffsetY, float subSamplingX,
                                  float subSamplingY) {
        Product product = new Product("test", "test", PW, PH);

        TiePointGrid latGrid = new TiePointGrid("latGrid", GW, GH, tiePointGridOffsetX, tiePointGridOffsetY,
                subSamplingX, subSamplingY, createLatGridData());
        TiePointGrid lonGrid = new TiePointGrid("lonGrid", GW, GH, tiePointGridOffsetX, tiePointGridOffsetY,
                subSamplingX, subSamplingY, createLonGridData());

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);

        Band latBand = product.addBand("latBand", ProductData.TYPE_FLOAT32);
        Band lonBand = product.addBand("lonBand", ProductData.TYPE_FLOAT32);

        latBand.setRasterData(ProductData.createInstance(createBandData(latGrid)));
        lonBand.setRasterData(ProductData.createInstance(createBandData(lonGrid)));

        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));

        return product;
    }

    private float[] createLatGridData() {
        return createGridData(LAT_1, LAT_2);
    }

    private float[] createLonGridData() {
        return createGridData(LON_1, LON_2);
    }

    private static float[] createBandData(TiePointGrid grid) {
        float[] floats = new float[PW * PH];
        for (int y = 0; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                floats[y * PW + x] = grid.getPixelFloat(x, y);
            }
        }
        return floats;
    }

    private static float[] createGridData(float lon0, float lon1) {
        float[] floats = new float[GW * GH];

        for (int j = 0; j < GH; j++) {
            for (int i = 0; i < GW; i++) {
                double x = i / (GW - 1f);
                double y = j / (GH - 1f);
                floats[j * GW + i] = (float)(lon0 + (lon1 - lon0) * x * x + 0.1f * (lon1 - lon0) * y * y);
            }
        }

        return floats;
    }


}
