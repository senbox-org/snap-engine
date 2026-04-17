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

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
public class TestGeoCodingSupport {

    @Test
    public void testAddGeoCoding() {
        Product product = new Product("test", "test", 1000, 1000);

        final JSONArray boundingBox = new JSONArray();
        boundingBox.add(-105.48165974621406);   // ll lon
        boundingBox.add(40.18137892682935);     // ll lat
        boundingBox.add(-105.46985016301836);   // ur lon
        boundingBox.add(40.19043640067074);     // ur lat

        GeoCodingSupport.addGeoCoding(product, boundingBox);

        GeoCoding geoCoding = product.getSceneGeoCoding();
        GeoPos ulPos = geoCoding.getGeoPos(new PixelPos(0,0), null);
        assertEquals(40.19043640067074, ulPos.getLat(),1e-5);
        assertEquals(-105.48165974621406, ulPos.getLon(), 1e-5);

        GeoPos lrPos = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth(),product.getSceneRasterHeight()), null);
        assertEquals(40.18137892682935, lrPos.getLat(),1e-5);
        assertEquals(-105.46985016301836, lrPos.getLon(),1e-5);

        GeoPos llPos = geoCoding.getGeoPos(new PixelPos(0,product.getSceneRasterHeight()), null);
        assertEquals(40.18137892682935, llPos.getLat(),1e-5);
        assertEquals(-105.48165974621406, llPos.getLon(),1e-5);

        GeoPos urPos = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth(),0), null);
        assertEquals(40.19043640067074, urPos.getLat(),1e-5);
        assertEquals(-105.46985016301836, urPos.getLon(),1e-5);
    }

    @Test
    public void testAddGeoCodingWithJSONArrayBbox() {
        Product product = new Product("test", "test", 500, 500);

        final JSONArray boundingBox = new JSONArray();
        JSONArray ul = new JSONArray();
        ul.add(-10.0);
        ul.add(50.0);
        JSONArray lr = new JSONArray();
        lr.add(-9.0);
        lr.add(49.0);
        boundingBox.add(ul);
        boundingBox.add(lr);

        GeoCodingSupport.addGeoCoding(product, boundingBox);
        assertNotNull(product.getSceneGeoCoding());
    }

    @Test
    public void testToWKTPolygon() {
        JSONObject geometry = new JSONObject();
        geometry.put("type", "Polygon");
        JSONArray coordinates = new JSONArray();
        JSONArray ring = new JSONArray();

        JSONArray p1 = new JSONArray(); p1.add(-121.94); p1.add(38.27); ring.add(p1);
        JSONArray p2 = new JSONArray(); p2.add(-121.94); p2.add(38.24); ring.add(p2);
        JSONArray p3 = new JSONArray(); p3.add(-121.91); p3.add(38.24); ring.add(p3);
        JSONArray p4 = new JSONArray(); p4.add(-121.91); p4.add(38.27); ring.add(p4);
        JSONArray p5 = new JSONArray(); p5.add(-121.94); p5.add(38.27); ring.add(p5);

        coordinates.add(ring);
        geometry.put("coordinates", coordinates);

        String wkt = GeoCodingSupport.toWKT(geometry);
        assertNotNull(wkt);
        assertTrue(wkt.startsWith("POLYGON(("));
        assertTrue(wkt.endsWith("))"));
        assertTrue(wkt.contains("-121.94 38.27"));
        assertTrue(wkt.contains("-121.91 38.24"));
    }

    @Test
    public void testToWKTClosedRing() {
        JSONObject geometry = new JSONObject();
        geometry.put("type", "Polygon");
        JSONArray coordinates = new JSONArray();
        JSONArray ring = new JSONArray();

        JSONArray p1 = new JSONArray(); p1.add(0.0); p1.add(0.0); ring.add(p1);
        JSONArray p2 = new JSONArray(); p2.add(1.0); p2.add(0.0); ring.add(p2);
        JSONArray p3 = new JSONArray(); p3.add(1.0); p3.add(1.0); ring.add(p3);
        JSONArray p4 = new JSONArray(); p4.add(0.0); p4.add(0.0); ring.add(p4);

        coordinates.add(ring);
        geometry.put("coordinates", coordinates);

        String wkt = GeoCodingSupport.toWKT(geometry);
        assertEquals("POLYGON((0.0 0.0,1.0 0.0,1.0 1.0,0.0 0.0))", wkt);
    }
}
