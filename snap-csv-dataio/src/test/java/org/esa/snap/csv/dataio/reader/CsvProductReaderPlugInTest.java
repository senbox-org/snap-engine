/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.csv.dataio.reader;

import com.bc.ceres.core.runtime.internal.UrlHelper;
import org.esa.snap.core.dataio.DecodeQualification;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductReaderPlugInTest {


    private CsvProductReaderPlugIn csvProductReaderPlugIn;

    @Before
    public void setUp() throws Exception {
        csvProductReaderPlugIn = new CsvProductReaderPlugIn();
    }

    @Test
    public void testGetDecodeQualification() throws Exception {
        Path validFile = Path.of(getResourceFilePath("simple_format_example.txt"));
        Path validFileWithoutProperties = Path.of(getResourceFilePath("simple_format_no_properties.txt"));
        Path invalidProperties = Path.of(getResourceFilePath("simple_format_invalid_properties.txt"));
        Path invalidHeader = Path.of(getResourceFilePath("simple_format_no_header.txt"));
        Path invalidType = Path.of(getResourceFilePath("simple_format_invalid_type.txt"));

        assertEquals(DecodeQualification.SUITABLE, csvProductReaderPlugIn.getDecodeQualification(validFile));
        assertEquals(DecodeQualification.SUITABLE, csvProductReaderPlugIn.getDecodeQualification(validFileWithoutProperties));
        assertEquals(DecodeQualification.UNABLE, csvProductReaderPlugIn.getDecodeQualification(invalidProperties));
        assertEquals(DecodeQualification.UNABLE, csvProductReaderPlugIn.getDecodeQualification(invalidHeader));
        assertEquals(DecodeQualification.UNABLE, csvProductReaderPlugIn.getDecodeQualification(invalidType));
    }

    @Test
    public void testGetFormatNames() throws Exception {
        assertEquals("CSV", csvProductReaderPlugIn.getFormatNames()[0]);
    }

    @Test
    public void testGetDescription() throws Exception {
        assertEquals("CSV products", csvProductReaderPlugIn.getDescription(null));
    }

    private String getResourceFilePath (String name) {
        URL url = getClass().getResource(name);
        assertNotNull(url);
        final Path file = UrlHelper.urlToFile(url);
        assertNotNull(file);
        return file.toAbsolutePath().toString();
    }
}
