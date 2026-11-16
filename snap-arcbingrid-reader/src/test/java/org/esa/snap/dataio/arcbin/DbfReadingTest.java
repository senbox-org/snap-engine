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

package org.esa.snap.dataio.arcbin;

import com.bc.ceres.core.runtime.internal.UrlHelper;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DbfReadingTest {

    @Test
    public void testFirstCreateDbfDescriptionMap() throws IOException {
        final URL fileURL = getClass().getResource("test_legend_1.dbf");
        assertNotNull(fileURL);
        final Path file = UrlHelper.urlToFile(fileURL);
        final Map<Integer, String> map = LegendFile.createDbfDescriptionMap(file);
        assertEquals("Tropical Broadleaved Forest", map.get(1));
        assertEquals("Fragmented Tropical Broadleav", map.get(2));
        assertEquals("Mangroves", map.get(5));
    }

    @Test
    public void testSecondCreateDbfDescriptionMap() throws IOException {
        final URL fileURL = getClass().getResource("test_legend_2.dbf");
        assertNotNull(fileURL);
        final Path file = UrlHelper.urlToFile(fileURL);
        final Map<Integer, String> map = LegendFile.createDbfDescriptionMap(file);
        assertEquals("Unclassified", map.get(0));
        assertEquals("Mosaic Forest / shrub cover", map.get(9));
        assertEquals("", map.get(11));
        assertEquals("Artificial surfaces", map.get(22));
    }

    @Test
    public void testLegend1() throws IOException {
        final URL fileURL = getClass().getResource("test_legend_1.dbf");
        assertNotNull(fileURL);
        final Path file = UrlHelper.urlToFile(fileURL);
        assertNotNull(file);
        final ReadableByteChannel channel = Files.newByteChannel(file);
        final DbaseFileReader reader = new DbaseFileReader(channel, true, Charset.defaultCharset());

        final DbaseFileHeader dbfHeader = reader.getHeader();
        final int fieldCount = dbfHeader.getNumFields();
        assertEquals(5, fieldCount); // last three are empty
        assertEquals(Integer.class, dbfHeader.getFieldClass(0));
        assertEquals(String.class, dbfHeader.getFieldClass(1));

        try {
            testRow(reader, 0, "No Data");
            testRow(reader, 1, "Tropical Broadleaved Forest");
            testRow(reader, 2, "Fragmented Tropical Broadleav");
            testRow(reader, 3, "Cultivated and Managed");
            testRow(reader, 4, "Water");
            testRow(reader, 5, "Mangroves");
        } finally {
            reader.close();
            channel.close();
        }
    }

    @Test
    public void testLegend2() throws IOException {
        final URL fileURL = getClass().getResource("test_legend_2.dbf");
        assertNotNull(fileURL);
        final Path file = UrlHelper.urlToFile(fileURL);
        assertNotNull(file);
        final ReadableByteChannel channel = Files.newByteChannel(file);
        final DbaseFileReader reader = new DbaseFileReader(channel, true, Charset.defaultCharset());

        final DbaseFileHeader dbfHeader = reader.getHeader();
        final int fieldCount = dbfHeader.getNumFields();
        assertEquals(5, fieldCount); // last three are empty
        assertEquals(Integer.class, dbfHeader.getFieldClass(0));
        assertEquals(String.class, dbfHeader.getFieldClass(1));

        try {
            testRow(reader, 0, "Unclassified");
            testRow(reader, 1, "Evergreen forest");
            testRow(reader, 2, "");
            testRow(reader, 3, "");
            testRow(reader, 4, "");
            testRow(reader, 5, "");
        } finally {
            reader.close();
            channel.close();
        }
    }

    private void testRow(DbaseFileReader reader, int value, String name) throws IOException {
        DbaseFileReader.Row row = reader.readRow();
        final Object actual = row.read(0);
        assertEquals(Integer.class, actual.getClass());
        assertEquals(value, ((Integer) actual).intValue());
        assertEquals(name, (String) row.read(1));
    }
}
