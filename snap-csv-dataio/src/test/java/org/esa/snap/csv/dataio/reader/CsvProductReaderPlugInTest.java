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

import org.esa.snap.core.dataio.DecodeQualification;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.*;

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
        File validFile = new File(getResourceFilePath("simple_format_example.txt"));
        File validFileWithoutProperties = new File(getResourceFilePath("simple_format_no_properties.txt"));
        File invalidProperties = new File(getResourceFilePath("simple_format_invalid_properties.txt"));
        File invalidHeader = new File(getResourceFilePath("simple_format_no_header.txt"));
        File invalidType = new File(getResourceFilePath("simple_format_invalid_type.txt"));

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

    private String getResourceFilePath (String name) throws URISyntaxException {
        URL url = getClass().getResource(name);
        URI uri = new URI(url.toString());
        return uri.getPath();
    }
}
