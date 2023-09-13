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
package org.esa.snap.core.dataio.dimap;

import org.esa.snap.GlobalTestConfig;
import org.esa.snap.GlobalTestTools;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Product;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class DimapProductWriterPlugInTest {

    private final static DimapProductWriterPlugIn _plugIn = new DimapProductWriterPlugIn();
    private ProductWriter _productWriter;

    @Before
    public void setUp() {
        _productWriter = _plugIn.createWriterInstance();
    }

    @After
    public void tearDown() {
        _productWriter = null;
        GlobalTestTools.deleteTestDataOutputDirectory();
    }

    @Test
    public void testPlugInInfoQuery() {
        assertNotNull(_plugIn.getFormatNames());
        assertEquals(1, _plugIn.getFormatNames().length);
        assertEquals(DimapProductConstants.DIMAP_FORMAT_NAME, _plugIn.getFormatNames()[0]);

        assertNotNull(_plugIn.getOutputTypes());
        assertEquals(2, _plugIn.getOutputTypes().length);

        assertNotNull(_plugIn.getDescription(null));
    }

    @Test
    public void testCreatedProductWriterInstance() {
        assertNotNull(_productWriter);
        assertTrue(_productWriter instanceof DimapProductWriter);
    }

    @Test
    public void testWriteProductNodes() {
        Product product = new Product("test", "TEST", 10, 10);
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile("DIMAP/test.dim");
        try {
            _productWriter.writeProductNodes(product, outputFile);
        } catch (IOException e) {
            fail("unexpected IOException: " + e.getMessage());
        }
    }
}
