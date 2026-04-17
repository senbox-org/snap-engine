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
package org.esa.snap.stac.internal;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.esa.snap.stac.StacItem;
import org.esa.snap.stac.extensions.DateTime;
import org.esa.snap.stac.extensions.EO;
import org.esa.snap.stac.extensions.Raster;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestSnapToStac {

    @Test
    @STTM("SNAP-4186")
    public void testSnapToStac() throws Exception {
        final Product srcProduct = TestUtils.createProduct("type", 10, 10);
        TestUtils.createBand(srcProduct, "b1", 10, 10);
        TestUtils.createBand(srcProduct, "b2", 10, 10);

        final StacItem stacItem = new StacItem("test");
        SnapToStac snapToStac = new SnapToStac(stacItem, srcProduct);

        snapToStac.writeBoundingBox();
        snapToStac.writeTimes();
        snapToStac.writeCoordinates();
        snapToStac.writeBands();

        JSONObject json = stacItem.getJSON();

        assertEquals("test", json.get(StacItem.ID));
        assertTrue(json.containsKey(StacItem.BBOX));
        assertTrue(json.containsKey(StacItem.GEOMETRY));

        JSONObject properties = (JSONObject)json.get(StacItem.PROPERTIES);
        assertEquals("2008-05-10T20:30:46.890683Z", properties.get(DateTime.start_datetime));
        assertEquals("2008-05-10T20:35:46.890683Z", properties.get(DateTime.end_datetime));
        assertNotNull(properties.get(DateTime.datetime));
        assertNotNull(properties.get(DateTime.created));

        JSONArray eoBands = (JSONArray)properties.get(EO.bands);
        assertNotNull(eoBands);
        assertEquals(2, eoBands.size());

        JSONObject band1 = (JSONObject) eoBands.get(0);
        assertEquals("b1", band1.get(EO.name));
        assertNotNull(band1.get(Raster.data_type));
    }

    @Test
    public void testWriteBoundingBox() throws Exception {
        final Product srcProduct = TestUtils.createProduct("type", 10, 10);

        final StacItem stacItem = new StacItem("bbox-test");
        SnapToStac snapToStac = new SnapToStac(stacItem, srcProduct);
        snapToStac.writeBoundingBox();

        JSONArray bbox = (JSONArray) stacItem.getJSON().get(StacItem.BBOX);
        assertNotNull(bbox);
        assertEquals(4, bbox.size());
    }

    @Test
    public void testWriteCoordinates() throws Exception {
        final Product srcProduct = TestUtils.createProduct("type", 10, 10);

        final StacItem stacItem = new StacItem("coords-test");
        SnapToStac snapToStac = new SnapToStac(stacItem, srcProduct);
        snapToStac.writeCoordinates();

        JSONObject geometry = (JSONObject) stacItem.getJSON().get(StacItem.GEOMETRY);
        assertNotNull(geometry);
        assertEquals("Polygon", geometry.get(StacItem.TYPE));
        assertTrue(geometry.containsKey(StacItem.COORDINATES));

        JSONArray coordinates = (JSONArray) geometry.get(StacItem.COORDINATES);
        assertNotNull(coordinates);
        assertEquals(1, coordinates.size());

        JSONArray ring = (JSONArray) coordinates.get(0);
        assertEquals(5, ring.size());  // 4 corners + closing point
    }

    @Test
    public void testWriteTimes() throws Exception {
        final Product srcProduct = TestUtils.createProduct("type", 10, 10);

        final StacItem stacItem = new StacItem("times-test");
        SnapToStac snapToStac = new SnapToStac(stacItem, srcProduct);
        snapToStac.writeTimes();

        JSONObject properties = stacItem.getProperties();
        assertNotNull(properties.get(DateTime.datetime));
        assertNotNull(properties.get(DateTime.start_datetime));
        assertNotNull(properties.get(DateTime.end_datetime));
        assertNotNull(properties.get(DateTime.created));
    }

    @Test
    public void testWriteBands() throws Exception {
        final Product srcProduct = TestUtils.createProduct("type", 10, 10);
        TestUtils.createBand(srcProduct, "red", 10, 10);
        TestUtils.createBand(srcProduct, "green", 10, 10);
        TestUtils.createBand(srcProduct, "blue", 10, 10);

        final StacItem stacItem = new StacItem("bands-test");
        SnapToStac snapToStac = new SnapToStac(stacItem, srcProduct);
        snapToStac.writeBands();

        JSONArray bands = (JSONArray) stacItem.getProperties().get(EO.bands);
        assertNotNull(bands);
        assertEquals(3, bands.size());

        for (int i = 0; i < bands.size(); i++) {
            JSONObject band = (JSONObject) bands.get(i);
            assertNotNull(band.get(EO.name));
            assertNotNull(band.get(EO.common_name));
            assertNotNull(band.get(Raster.data_type));
        }
    }
}
