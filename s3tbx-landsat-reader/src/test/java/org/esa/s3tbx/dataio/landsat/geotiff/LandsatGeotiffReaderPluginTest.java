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

package org.esa.s3tbx.dataio.landsat.geotiff;

import org.esa.snap.core.dataio.ProductReader;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class LandsatGeotiffReaderPluginTest {

    private LandsatGeotiffReaderPlugin plugin;

    @Before
    public void setUp() throws Exception {
        plugin = new LandsatGeotiffReaderPlugin();
    }

    @Test
    public void testGetInputTypes() throws Exception {
        assertArrayEquals(new Class[]{String.class, File.class}, plugin.getInputTypes());
    }

    @Test
    public void testCreateReaderInstance() throws Exception {
        ProductReader productReader = plugin.createReaderInstance();
        assertNotNull(productReader);
        assertTrue(productReader instanceof LandsatGeotiffReader);
    }

    @Test
    public void testGetFormatNames() throws Exception {
        assertArrayEquals(new String[]{"LandsatGeoTIFF"}, plugin.getFormatNames());
    }

    @Test
    public void testGetDefaultFileExtensions() throws Exception {
        assertArrayEquals(new String[]{".txt", ".TXT", ".gz", ".tgz"}, plugin.getDefaultFileExtensions());
    }

    @Test
    public void testGetDescription() throws Exception {
        assertEquals("Landsat Data Products (GeoTIFF)", plugin.getDescription(null));
    }

    @Test
    public void testGetProductFileFilter() throws Exception {
        assertNotNull(plugin.getProductFileFilter());
    }

    @Test
    public void testIsMetadataFilename() throws Exception {
        assertTrue(LandsatGeotiffReaderPlugin.isMetadataFilename("test_L8_MTL.txt"));
        assertTrue(LandsatGeotiffReaderPlugin.isMetadataFilename("test_legacy_L5_WithTrailingWhiteSpace_MTL.txt"));
        assertFalse(LandsatGeotiffReaderPlugin.isMetadataFilename("test_MTL_L7.txt"));
    }

    @Test
    public void testIsMetadataFile() throws Exception {
        InputStream positiveFile1 = getClass().getResourceAsStream("test_L8_MTL.txt");
        assertTrue(LandsatGeotiffReaderPlugin.isMetadataFile(positiveFile1));

        InputStream positiveFile2 = getClass().getResourceAsStream("test_legacy_L5_WithTrailingWhiteSpace_MTL.txt");
        assertTrue(LandsatGeotiffReaderPlugin.isMetadataFile(positiveFile2));
    }
}
