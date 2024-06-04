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
package org.esa.stac.internal;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.json.simple.JSONArray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
        System.out.println("urPos = " + ulPos);
        assertEquals(40.19043640067074, ulPos.getLat(),1e-5);
        assertEquals(-105.48165974621406, ulPos.getLon(), 1e-5);

        GeoPos lrPos = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth(),product.getSceneRasterHeight()), null);
        System.out.println("lrPos = " + lrPos);
        assertEquals(40.18137892682935, lrPos.getLat(),1e-5);
        assertEquals(-105.46985016301836, lrPos.getLon(),1e-5);

        GeoPos llPos = geoCoding.getGeoPos(new PixelPos(0,product.getSceneRasterHeight()), null);
        System.out.println("llPos = " + llPos);
        assertEquals(40.18137892682935, llPos.getLat(),1e-5);
        assertEquals(-105.48165974621406, llPos.getLon(),1e-5);

        GeoPos urPos = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth(),0), null);
        System.out.println("urPos = " + urPos);
        assertEquals(40.19043640067074, urPos.getLat(),1e-5);
        assertEquals(-105.46985016301836, urPos.getLon(),1e-5);
    }
}
