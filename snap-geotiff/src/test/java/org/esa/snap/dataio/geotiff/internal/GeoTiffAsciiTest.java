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

package org.esa.snap.dataio.geotiff.internal;

import org.junit.Test;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * TiffAscii Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>02/21/2005</pre>
 */

public class GeoTiffAsciiTest {

    @Test
    public void testCreation() {
        new GeoTiffAscii("Alois und Sepp");
    }

    @Test
    public void testGetValue() {
        final GeoTiffAscii tiffAscii = new GeoTiffAscii("Alois und Sepp");
        assertEquals("Alois und Sepp|\u0000", tiffAscii.getValue());
    }

    @Test
    public void testWrite() throws Exception {
        final GeoTiffAscii tiffAscii = new GeoTiffAscii("Alois und Sepp");

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(os);
        tiffAscii.write(ios);
        ios.flush();
        assertEquals("Alois und Sepp|\u0000", os.toString());
    }

    @Test
    public void testGetSizeInBytes() {
        final String value = "Hedi und Fredi";
        final GeoTiffAscii geoTiffAscii = new GeoTiffAscii(value);
        assertEquals(value.length() + 2, geoTiffAscii.getSizeInBytes());
    }
}
