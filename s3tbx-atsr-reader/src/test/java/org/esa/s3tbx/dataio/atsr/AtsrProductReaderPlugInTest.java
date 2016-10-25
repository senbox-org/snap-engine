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
package org.esa.s3tbx.dataio.atsr;

import org.esa.snap.core.dataio.ProductReader;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AtsrProductReaderPlugInTest {

    private AtsrProductReaderPlugIn _plugIn = null;

    @Before
    public void setUp() {
        _plugIn = new AtsrProductReaderPlugIn();
        assertNotNull(_plugIn);
    }

    @Test
    public void testFormatNames() {
        String[] actualNames = null;
        String[] expectedNames = new String[]{AtsrConstants.ATSR_FORMAT_NAME};

        actualNames = _plugIn.getFormatNames();
        assertNotNull(actualNames);

        for (int n = 0; n < expectedNames.length; n++) {
            assertEquals(expectedNames[n], actualNames[n]);
        }
    }

    @Test
    public void testGetDescrition() {
        assertEquals(AtsrConstants.DESCRIPTION, _plugIn.getDescription(null));
    }

    @Test
    public void testGetInputTypes() {
        Class[] expected = new Class[]{String.class, File.class, ImageInputStream.class};
        Class[] actual = null;

        actual = _plugIn.getInputTypes();
        assertNotNull(actual);

        for (int n = 0; n < expected.length; n++) {
            assertEquals(expected[n], actual[n]);
        }
    }

    @Test
    public void testCreateInstance() {
        ProductReader reader = null;

        reader = _plugIn.createReaderInstance();
        assertNotNull(reader);
        assertTrue(reader instanceof AtsrProductReader);
    }
}
