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

import org.esa.snap.stac.extensions.EO;
import org.esa.snap.stac.extensions.Proj;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("unchecked")
public class TestBandSupport {

    @Test
    public void testGetMaxDimensionFromProjShape() {
        JSONObject properties = new JSONObject();
        JSONArray shape = new JSONArray();
        shape.add(7981L);  // height
        shape.add(7881L);  // width
        properties.put(Proj.shape, shape);

        Dimension dim = BandSupport.getMaxDimension(properties);
        assertNotNull(dim);
        assertEquals(7881, dim.width);
        assertEquals(7981, dim.height);
    }

    @Test
    public void testGetMaxDimensionFromEOBands() {
        JSONObject properties = new JSONObject();
        JSONArray bandsArray = new JSONArray();

        JSONObject band1 = new JSONObject();
        JSONArray shape1 = new JSONArray();
        shape1.add(100L);
        shape1.add(200L);
        band1.put(Proj.shape, shape1);
        bandsArray.add(band1);

        JSONObject band2 = new JSONObject();
        JSONArray shape2 = new JSONArray();
        shape2.add(300L);
        shape2.add(400L);
        band2.put(Proj.shape, shape2);
        bandsArray.add(band2);

        properties.put(EO.bands, bandsArray);

        Dimension dim = BandSupport.getMaxDimension(properties);
        assertNotNull(dim);
        assertEquals(400, dim.width);
        assertEquals(300, dim.height);
    }

    @Test
    public void testGetMaxDimensionEmptyProperties() {
        JSONObject properties = new JSONObject();
        Dimension dim = BandSupport.getMaxDimension(properties);
        assertNull(dim);
    }

    @Test
    public void testGetMaxDimensionEmptyEOBands() {
        JSONObject properties = new JSONObject();
        properties.put(EO.bands, new JSONArray());

        Dimension dim = BandSupport.getMaxDimension(properties);
        assertNull(dim);
    }

    @Test
    public void testGetMaxDimensionProjShapeTakesPrecedence() {
        JSONObject properties = new JSONObject();

        JSONArray shape = new JSONArray();
        shape.add(500L);
        shape.add(600L);
        properties.put(Proj.shape, shape);

        JSONArray bandsArray = new JSONArray();
        JSONObject band = new JSONObject();
        JSONArray bandShape = new JSONArray();
        bandShape.add(100L);
        bandShape.add(200L);
        band.put(Proj.shape, bandShape);
        bandsArray.add(band);
        properties.put(EO.bands, bandsArray);

        Dimension dim = BandSupport.getMaxDimension(properties);
        assertNotNull(dim);
        assertEquals(600, dim.width);
        assertEquals(500, dim.height);
    }

    @Test
    public void testGetMaxDimensionSingleElementShape() {
        JSONObject properties = new JSONObject();
        JSONArray shape = new JSONArray();
        shape.add(100L);
        properties.put(Proj.shape, shape);

        Dimension dim = BandSupport.getMaxDimension(properties);
        assertNull(dim);
    }
}
