/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package gov.nasa.gsfc.seadas.dataio;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.text.DateFormat;

import static org.junit.Assert.*;

public class SeadasFileReaderTest {

    @Test
    public void testParseUtcDate() throws Exception {
        DateFormat format = ProductData.UTC.createDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        assertNull(SeadasFileReader.parseUtcDate("foo"));

        // ISO
        ProductData.UTC parsed = SeadasFileReader.parseUtcDate("2012-03-05T12:13:14Z");
        assertNotNull(parsed);
        assertEquals("2012-03-05T12:13:14.000Z", format.format(parsed.getAsDate()));
        assertEquals(0, parsed.getMicroSecondsFraction());

        // ISO with micros
        parsed = SeadasFileReader.parseUtcDate("2012-03-05T12:13:14.123Z");
        assertNotNull(parsed);
        assertEquals("2012-03-05T12:13:14.123Z", format.format(parsed.getAsDate()));
        assertEquals(123000, parsed.getMicroSecondsFraction());

        // ISO no-punctation
        parsed = SeadasFileReader.parseUtcDate("20120305T121314Z");
        assertNotNull(parsed);
        assertEquals("2012-03-05T12:13:14.000Z", format.format(parsed.getAsDate()));
        assertEquals(0, parsed.getMicroSecondsFraction());

        // MODIS
        parsed = SeadasFileReader.parseUtcDate("2012-03-05 12:13:14.123456");
        assertNotNull(parsed);
        assertEquals("2012-03-05T12:13:14.123Z", format.format(parsed.getAsDate()));
        assertEquals(123456, parsed.getMicroSecondsFraction());

        // OCTS
        parsed = SeadasFileReader.parseUtcDate("20120305 12:13:14.123456");
        assertNotNull(parsed);
        assertEquals("2012-03-05T12:13:14.123Z", format.format(parsed.getAsDate()));
        assertEquals(123456, parsed.getMicroSecondsFraction());

        parsed = SeadasFileReader.parseUtcDate("2012065121314123");
        assertNotNull(parsed);
        assertEquals("2012-03-05T12:13:14.123Z", format.format(parsed.getAsDate()));
        assertEquals(123000, parsed.getMicroSecondsFraction());

    }
}