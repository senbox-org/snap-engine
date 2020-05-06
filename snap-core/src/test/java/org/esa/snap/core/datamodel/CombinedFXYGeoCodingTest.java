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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CombinedFXYGeoCodingTest {

    private CombinedFXYGeoCoding _srcGeoCoding;

    @Before
    public void setUp() throws Exception {
        final CombinedFXYGeoCoding.CodingWrapper[] codingWrappers = new CombinedFXYGeoCoding.CodingWrapper[2];
        final FXYGeoCoding geoCoding = new FXYGeoCoding(0, 0, 1, 1,
                                                        new FXYSum.Linear(new double[]{1, 2, 3}),
                                                        new FXYSum.Linear(new double[]{1, 2, 3}),
                                                        new FXYSum.Linear(new double[]{1, 2, 3}),
                                                        new FXYSum.Linear(new double[]{1, 2, 3}),
                                                        Datum.WGS_84);
        codingWrappers[0] = new CombinedFXYGeoCoding.CodingWrapper(geoCoding, 0, 0, 4, 5);
        codingWrappers[1] = new CombinedFXYGeoCoding.CodingWrapper(geoCoding, 4, 0, 6, 5);
        _srcGeoCoding = new CombinedFXYGeoCoding(codingWrappers);
    }

    @After
    public void tearDown() throws Exception {
        _srcGeoCoding = null;
    }

    @Test
    public void testTransferGeoCodingWithSubSampling() {
        final Scene srcScene = SceneFactory.createScene(new Band("srcTest", ProductData.TYPE_UINT8, 10, 10));
        final Scene destScene = SceneFactory.createScene(new Band("destTest", ProductData.TYPE_UINT8, 4, 4));
        final ProductSubsetDef subsetDef = new ProductSubsetDef("testSubset");
        subsetDef.setSubSampling(3, 3);


        _srcGeoCoding.transferGeoCoding(srcScene, destScene, subsetDef);


        final GeoCoding actGeoCoding = destScene.getGeoCoding();
        assertNotNull(actGeoCoding);

        final PixelPos srcPixelPos = new PixelPos();
        final PixelPos destPixelPos = new PixelPos();

        srcPixelPos.setLocation(0, 0);
        destPixelPos.setLocation(0, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(3, 0);
        destPixelPos.setLocation(1, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(6, 0);
        destPixelPos.setLocation(2, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(9, 0);
        destPixelPos.setLocation(3, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));
    }

    @Test
    public void testTransferGeoCodingWithRegion() {
        final Scene srcScene = SceneFactory.createScene(new Band("srcTest", ProductData.TYPE_UINT8, 10, 10));
        final Scene destScene = SceneFactory.createScene(new Band("destTest", ProductData.TYPE_UINT8, 4, 3));
        final ProductSubsetDef subsetDef = new ProductSubsetDef("testSubset");
        subsetDef.setSubsetRegion(new PixelSubsetRegion(3, 1, 4, 3, 0));


        _srcGeoCoding.transferGeoCoding(srcScene, destScene, subsetDef);


        final GeoCoding actGeoCoding = destScene.getGeoCoding();
        assertNotNull(actGeoCoding);

        final PixelPos srcPixelPos = new PixelPos();
        final PixelPos destPixelPos = new PixelPos();

        srcPixelPos.setLocation(3, 1);
        destPixelPos.setLocation(0, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(5, 1);
        destPixelPos.setLocation(2, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(6, 2);
        destPixelPos.setLocation(3, 1);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(4, 3);
        destPixelPos.setLocation(1, 2);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));
    }

    @Test
    public void testTransferGeoCodingWithRegionAndSubsampling() {
        final Scene srcScene = SceneFactory.createScene(new Band("srcTest", ProductData.TYPE_UINT8, 10, 10));
        final Scene destScene = SceneFactory.createScene(new Band("destTest", ProductData.TYPE_UINT8, 2, 2));
        final ProductSubsetDef subsetDef = new ProductSubsetDef("testSubset");
        subsetDef.setSubsetRegion(new PixelSubsetRegion(3, 1, 4, 3, 0));
        subsetDef.setSubSampling(2, 2);


        _srcGeoCoding.transferGeoCoding(srcScene, destScene, subsetDef);


        final GeoCoding actGeoCoding = destScene.getGeoCoding();
        assertNotNull(actGeoCoding);

        final PixelPos srcPixelPos = new PixelPos();
        final PixelPos destPixelPos = new PixelPos();

        srcPixelPos.setLocation(3, 1);
        destPixelPos.setLocation(0, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(5, 1);
        destPixelPos.setLocation(1, 0);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(3, 3);
        destPixelPos.setLocation(0, 1);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));

        srcPixelPos.setLocation(5, 3);
        destPixelPos.setLocation(1, 1);
        assertEquals(_srcGeoCoding.getGeoPos(srcPixelPos, null), actGeoCoding.getGeoPos(destPixelPos, null));
    }

    @Test
    public void testCanClone() {
        assertTrue(_srcGeoCoding.canClone());
    }

    @Test
    public void testClone() {
        GeoPos geoPos = _srcGeoCoding.getGeoPos(new PixelPos(8.5, 2.5), null);
        assertEquals(17.5, geoPos.lon, 1e-8);
        assertEquals(17.5, geoPos.lat, 1e-8);

        PixelPos pixelPos = _srcGeoCoding.getPixelPos(new GeoPos(11.8, 22.3), null);
        assertEquals(95.5, pixelPos.getX(), 1e-8);
        assertEquals(91.5, pixelPos.getY(), 1e-8);

        final GeoCoding clone = _srcGeoCoding.clone();

        geoPos = clone.getGeoPos(new PixelPos(8.5, 2.5), null);
        assertEquals(17.5, geoPos.lon, 1e-8);
        assertEquals(17.5, geoPos.lat, 1e-8);

        pixelPos = _srcGeoCoding.getPixelPos(new GeoPos(11.8, 22.3), null);
        assertEquals(95.5, pixelPos.getX(), 1e-8);
        assertEquals(91.5, pixelPos.getY(), 1e-8);
    }

    @Test
    public void testClone_dispose() {
        GeoPos geoPos = _srcGeoCoding.getGeoPos(new PixelPos(7.5, 1.5), null);
        assertEquals(12.5, geoPos.lon, 1e-8);
        assertEquals(12.5, geoPos.lat, 1e-8);

        PixelPos pixelPos = _srcGeoCoding.getPixelPos(new GeoPos(10.8, 21.3), null);
        assertEquals(90.5, pixelPos.getX(), 1e-8);
        assertEquals(86.5, pixelPos.getY(), 1e-8);

        final GeoCoding clone = _srcGeoCoding.clone();
        _srcGeoCoding.dispose();

        geoPos = clone.getGeoPos(new PixelPos(7.5, 1.5), null);
        assertEquals(12.5, geoPos.lon, 1e-8);
        assertEquals(12.5, geoPos.lat, 1e-8);

        pixelPos = clone.getPixelPos(new GeoPos(10.8, 21.3), null);
        assertEquals(90.5, pixelPos.getX(), 1e-8);
        assertEquals(86.5, pixelPos.getY(), 1e-8);

        clone.dispose();
    }

    @Test
    public void testClone_codingWrapper() {
        final FXYGeoCoding fxyGeoCoding = mock(FXYGeoCoding.class);

        final CombinedFXYGeoCoding.CodingWrapper codingWrapper = new CombinedFXYGeoCoding.CodingWrapper(fxyGeoCoding, 0, 2, 3, 4);

        final CombinedFXYGeoCoding.CodingWrapper clone = codingWrapper.clone();
        assertNotNull(clone);
        assertEquals(0, clone.getRegion().x);
        assertEquals(2, clone.getRegion().y);
        assertEquals(3, clone.getRegion().width);
        assertEquals(4, clone.getRegion().height);

        verify(fxyGeoCoding, times(1)).clone();
        verifyNoMoreInteractions(fxyGeoCoding);
    }
}
