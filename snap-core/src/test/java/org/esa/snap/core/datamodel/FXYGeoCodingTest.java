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


import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.math.FXYSum;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
public class FXYGeoCodingTest {

    private FXYGeoCoding _geoCoding;

    @Before
    public void setUp() {
        final FXYSum.Linear xFunc = new FXYSum.Linear(new double[]{0, 0, 1});
        final FXYSum.Linear yFunc = new FXYSum.Linear(new double[]{0, 1, 0});
        final FXYSum.Linear latFunc = new FXYSum.Linear(new double[]{0, 0, 1});
        final FXYSum.Linear lonFunc = new FXYSum.Linear(new double[]{0, 1, 0});

        _geoCoding = new FXYGeoCoding(0, 0, 1, 1,
                                      xFunc, yFunc,
                                      latFunc, lonFunc,
                                      Datum.WGS_84);

    }

    @Test
    public void testThatReverseIsInvOfForward() {
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos(12.5f, 349.1f);
        _geoCoding.getGeoPos(pixelPos, geoPos);

        final PixelPos pixelPosRev = new PixelPos();
        _geoCoding.getPixelPos(geoPos, pixelPosRev);

        assertEquals(pixelPos.x, pixelPosRev.x, 1e-4);
        assertEquals(pixelPos.y, pixelPosRev.y, 1e-4);
    }

    @Test
    public void testFXYSumsRevAndForwAreEqual() {
        // values are taken from a AVNIR-2 product
        final FXYSum.Cubic funcLat = new FXYSum.Cubic(new double[]{
                38.500063158199914,
                -1.5864380827666764E-5,
                -8.87076135137345E-5,
                -5.4715650494309404E-11,
                1.7664837083366042E-11,
                -2.117514813932369E-12,
                3.064408168899423E-17,
                1.585962164097323E-16,
                -6.098694274571135E-17,
                5.84863124300545E-18
        });
        final FXYSum.Cubic funcLon = new FXYSum.Cubic(new double[]{
                140.13684646307198,
                1.128828545988631E-4,
                -2.018789097584942E-5,
                -2.3783153647830587E-11,
                -1.3475060752719392E-10,
                2.3779980524035797E-11,
                -8.964042454477968E-17,
                1.603005243179082E-16,
                2.689055849423117E-16,
                -5.342672795186944E-17
        });
        final FXYSum.Cubic funcX = new FXYSum.Cubic(new double[]{
                -1004274.164152284,
                -8438.725369567894,
                1931.015292199648,
                480.2628768865113,
                -51.80353463858226,
                79.50343943972683,
                -1.170751563256245,
                -2.461732799965884,
                0.43455217130283397,
                -0.24909748244716065
        });
        final FXYSum.Cubic funcY = new FXYSum.Cubic(new double[]{
                129893.51986530663,
                -10247.4132114523,
                3492.9557841930427,
                -35.089200336275994,
                -12.449311003384066,
                8.925552458488387,
                -0.13026608164787684,
                0.34836844379127946,
                0.03006361443561656,
                -0.13394457012591415
        });

        final double x = 10.5;
        final double y = 15.5;
        final double lat = funcLat.computeZ(x, y);
        final double lon = funcLon.computeZ(x, y);

        final double x2 = funcX.computeZ(lat, lon);
        final double y2 = funcY.computeZ(lat, lon);

        assertEquals(x, x2, 1e-1);
        assertEquals(y, y2, 1e-2);
    }

    @Test
    public void testTransferGeoCodingWithSubset() {
        final Band srcNode = new Band("srcDummy", ProductData.TYPE_INT8, 10, 20);
        srcNode.setGeoCoding(_geoCoding);
        final Scene srcScene = SceneFactory.createScene(srcNode);
        final ProductSubsetDef subset = new ProductSubsetDef("subset");
        subset.setSubsetRegion(new PixelSubsetRegion(10, 10, 50, 50, 0));
        subset.setSubSampling(2, 3);
        final Band destNode = new Band("destDummy", ProductData.TYPE_INT8, 10, 20);
        final Scene destScene = SceneFactory.createScene(destNode);

        srcScene.transferGeoCodingTo(destScene, subset);

        assertFXYGeoCodingIsCopied((FXYGeoCoding) destNode.getGeoCoding(), subset);
    }

    @Test
    public void testTransferGeoCodingWithoutSubset() {
        final Band srcNode = new Band("srcDummy", ProductData.TYPE_INT8, 10, 20);
        srcNode.setGeoCoding(_geoCoding);
        final Scene srcScene = SceneFactory.createScene(srcNode);
        final Band destNode = new Band("destDummy", ProductData.TYPE_INT8, 10, 20);
        final Scene destScene = SceneFactory.createScene(destNode);

        srcScene.transferGeoCodingTo(destScene, null);

        assertFXYGeoCodingIsCopied((FXYGeoCoding) destNode.getGeoCoding(), null);
    }

    @Test
    public void testCanClone() {
        assertTrue(_geoCoding.canClone());
    }

    @Test
    public void testClone() {
        GeoPos geoPos = _geoCoding.getGeoPos(new PixelPos(13, 21), null);
        assertEquals(13.0, geoPos.lon, 1e-8);
        assertEquals(21.0, geoPos.lat, 1e-8);

        PixelPos pixelPos = _geoCoding.getPixelPos(new GeoPos(51.3, 3.445), null);
        assertEquals(3.445, pixelPos.x, 1e-8);
        assertEquals(51.3, pixelPos.y, 1e-8);

        final GeoCoding clone = _geoCoding.clone();

        geoPos = clone.getGeoPos(new PixelPos(13, 21), null);
        assertEquals(13.0, geoPos.lon, 1e-8);
        assertEquals(21.0, geoPos.lat, 1e-8);

        pixelPos = clone.getPixelPos(new GeoPos(51.3, 3.445), null);
        assertEquals(3.445, pixelPos.x, 1e-8);
        assertEquals(51.3, pixelPos.y, 1e-8);
    }

    @Test
    public void testClone_dispose() {
        GeoPos geoPos = _geoCoding.getGeoPos(new PixelPos(14, 22), null);
        assertEquals(14.0, geoPos.lon, 1e-8);
        assertEquals(22.0, geoPos.lat, 1e-8);

        PixelPos pixelPos = _geoCoding.getPixelPos(new GeoPos(51.4, 3.455), null);
        assertEquals(3.455, pixelPos.x, 1e-8);
        assertEquals(51.4, pixelPos.y, 1e-8);

        final GeoCoding clone = _geoCoding.clone();
        _geoCoding.dispose();

        geoPos = clone.getGeoPos(new PixelPos(13, 21), null);
        assertEquals(13.0, geoPos.lon, 1e-8);
        assertEquals(21.0, geoPos.lat, 1e-8);

        pixelPos = clone.getPixelPos(new GeoPos(51.4, 3.455), null);
        assertEquals(3.455, pixelPos.x, 1e-8);
        assertEquals(51.4, pixelPos.y, 1e-8);

        clone.dispose();
    }

    private void assertFXYGeoCodingIsCopied(final FXYGeoCoding subsetGeoCoding, ProductSubsetDef subset) {
        assertNotSame(_geoCoding, subsetGeoCoding);

        if (subset == null) {
            subset = new ProductSubsetDef("s");
            subset.setSubsetRegion(new PixelSubsetRegion(0, 0, 100, 100, 0));
            subset.setSubSampling(1, 1);
        }
        assertEquals(_geoCoding.getPixelOffsetX() + subset.getRegion().getX(), subsetGeoCoding.getPixelOffsetX(), 1.e-6);
        assertEquals(_geoCoding.getPixelOffsetY() + subset.getRegion().getY(), subsetGeoCoding.getPixelOffsetY(), 1.e-6);
        assertEquals(_geoCoding.getPixelSizeX() * subset.getSubSamplingX(), subsetGeoCoding.getPixelSizeX(), 1.e-6);
        assertEquals(_geoCoding.getPixelSizeY() * subset.getSubSamplingY(), subsetGeoCoding.getPixelSizeY(), 1.e-6);

        assertTrue(Arrays.equals(_geoCoding.getPixelXFunction().getCoefficients(),
                                 subsetGeoCoding.getPixelXFunction().getCoefficients()));
        assertTrue(Arrays.equals(_geoCoding.getPixelYFunction().getCoefficients(),
                                 subsetGeoCoding.getPixelYFunction().getCoefficients()));
        assertTrue(Arrays.equals(_geoCoding.getLatFunction().getCoefficients(),
                                 subsetGeoCoding.getLatFunction().getCoefficients()));
        assertTrue(Arrays.equals(_geoCoding.getLonFunction().getCoefficients(),
                                 subsetGeoCoding.getLonFunction().getCoefficients()));
    }
}
