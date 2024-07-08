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

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.esa.snap.stac.StacItem;
import org.esa.snap.stac.extensions.DateTime;
import org.esa.snap.stac.extensions.EO;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestSnapToStac {

    @Test
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

        System.out.println(stacItem.getJSON());
        JSONObject json = stacItem.getJSON();

        assertEquals("test", json.get(StacItem.ID));
        assertTrue(json.containsKey(StacItem.BBOX));
        assertTrue(json.containsKey(StacItem.GEOMETRY));

        JSONObject properties = (JSONObject)json.get(StacItem.PROPERTIES);
        assertEquals("2008-05-10T20:30:46.890683Z", properties.get(DateTime.start_datetime));
        assertEquals("2008-05-10T20:35:46.890683Z", properties.get(DateTime.end_datetime));

        JSONArray eoBands = (JSONArray)properties.get(EO.bands);
    }
}
