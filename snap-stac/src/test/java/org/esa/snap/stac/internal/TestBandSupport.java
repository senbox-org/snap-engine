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

import org.esa.snap.stac.StacItem;
import org.esa.snap.stac.extensions.Assets;
import org.esa.snap.stac.extensions.EO;
import org.esa.snap.stac.extensions.Proj;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.awt.*;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("unchecked")
public class TestBandSupport {

    @Test
    public void testGetMaxDimensionFromProjShape() throws Exception {
        StacItem item = createStacItem("proj-shape-test");
        JSONArray shape = new JSONArray();
        shape.add(7981L);  // height
        shape.add(7881L);  // width
        item.getProperties().put(Proj.shape, shape);

        Dimension dim = BandSupport.getMaxDimension(item);
        assertNotNull(dim);
        assertEquals(7881, dim.width);
        assertEquals(7981, dim.height);
    }

    @Test
    public void testGetMaxDimensionFromEOBands() throws Exception {
        StacItem item = createStacItem("eo-bands-test");
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

        item.getProperties().put(EO.bands, bandsArray);

        Dimension dim = BandSupport.getMaxDimension(item);
        assertNotNull(dim);
        assertEquals(400, dim.width);
        assertEquals(300, dim.height);
    }

    @Test
    public void testGetMaxDimensionFromAssets() throws Exception {
        StacItem item = createStacItem("assets-test");

        // Add an asset with proj:shape directly in its JSON
        JSONObject assetJSON = new JSONObject();
        assetJSON.put(Assets.href, "https://example.com/band.tif");
        JSONArray shape = new JSONArray();
        shape.add(500L);
        shape.add(600L);
        assetJSON.put(Proj.shape, shape);
        item.getAssets().put("band1", assetJSON);

        Dimension dim = BandSupport.getMaxDimension(item);
        assertNotNull(dim);
        assertEquals(600, dim.width);
        assertEquals(500, dim.height);
    }

    @Test
    public void testGetMaxDimensionEmptyItem() throws Exception {
        StacItem item = createStacItem("empty-test");
        Dimension dim = BandSupport.getMaxDimension(item);
        assertNull(dim);
    }

    @Test
    public void testGetMaxDimensionEmptyEOBands() throws Exception {
        StacItem item = createStacItem("empty-eo-test");
        item.getProperties().put(EO.bands, new JSONArray());

        Dimension dim = BandSupport.getMaxDimension(item);
        assertNull(dim);
    }

    @Test
    public void testGetMaxDimensionProjShapeTakesPrecedence() throws Exception {
        StacItem item = createStacItem("precedence-test");

        // Set proj:shape on properties (should be used)
        JSONArray shape = new JSONArray();
        shape.add(500L);
        shape.add(600L);
        item.getProperties().put(Proj.shape, shape);

        // Also set EO bands with different dimensions (should be ignored)
        JSONArray bandsArray = new JSONArray();
        JSONObject band = new JSONObject();
        JSONArray bandShape = new JSONArray();
        bandShape.add(100L);
        bandShape.add(200L);
        band.put(Proj.shape, bandShape);
        bandsArray.add(band);
        item.getProperties().put(EO.bands, bandsArray);

        Dimension dim = BandSupport.getMaxDimension(item);
        assertNotNull(dim);
        assertEquals(600, dim.width);
        assertEquals(500, dim.height);
    }

    @Test
    public void testGetMaxDimensionSingleElementShape() throws Exception {
        StacItem item = createStacItem("single-shape-test");
        JSONArray shape = new JSONArray();
        shape.add(100L);
        item.getProperties().put(Proj.shape, shape);

        Dimension dim = BandSupport.getMaxDimension(item);
        assertNull(dim);
    }

    private StacItem createStacItem(String id) throws IOException {
        return new StacItem(id);
    }
}
