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
package org.esa.snap.stac.extensions;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestEO {

    @Test
    public void testGetCommonName() {
        Assert.assertEquals(EO.coastal, EO.getCommonName("coastal"));
        assertEquals(EO.red, EO.getCommonName("red"));
        assertEquals(EO.green, EO.getCommonName("green"));
        assertEquals(EO.blue, EO.getCommonName("blue"));
        assertEquals(EO.yellow, EO.getCommonName("yellow"));
        assertEquals(EO.rededge, EO.getCommonName("rededge"));
        assertEquals(EO.rededge, EO.getCommonName("red-edge"));
        assertEquals(EO.nir, EO.getCommonName("nir"));
        assertEquals(EO.nir, EO.getCommonName("nearinfrared"));
        assertEquals(EO.nir, EO.getCommonName("near-infrared"));
        assertEquals(EO.nir08, EO.getCommonName("nir08"));
        assertEquals(EO.nir09, EO.getCommonName("nir09"));
        assertEquals(EO.pan, EO.getCommonName("pan"));
        assertEquals(EO.pan, EO.getCommonName("panchromatic"));

        assertEquals(EO.cirrus, EO.getCommonName("cirrus"));
        assertEquals(EO.swir16, EO.getCommonName("swir"));
        assertEquals(EO.swir16, EO.getCommonName("swir1"));
        assertEquals(EO.swir16, EO.getCommonName("shortwave-infrared1"));
        assertEquals(EO.swir22, EO.getCommonName("swir2"));
        assertEquals(EO.swir22, EO.getCommonName("shortwave-infrared2"));
        assertEquals(EO.lwir, EO.getCommonName("lwir"));
        assertEquals(EO.lwir, EO.getCommonName("longwave-infrared"));
        assertEquals(EO.lwir11, EO.getCommonName("longwave-infrared11"));
        assertEquals(EO.lwir12, EO.getCommonName("longwave-infrared12"));

        assertEquals("xyz", EO.getCommonName("xyz"));
    }

    @Test
    public void testWriteBand() throws Exception {
        Band band = new Band("bandName", ProductData.TYPE_INT16, 100, 100);
        band.setDescription("description");
        band.setNoDataValue(-1);
        band.setUnit("unit");

        final String expected = "{\"unit\":\"unit\",\"full_width_half_max\":0.0,\"center_wavelength\":0.0,\"name\":\"bandName\",\"data_type\":\"int16\",\"description\":\"description\",\"common_name\":\"bandName\"}";

        JSONObject json = EO.writeBand(band);

        System.out.println(json);
       //assertTrue(JSON.equals(json, expected));
    }
}
