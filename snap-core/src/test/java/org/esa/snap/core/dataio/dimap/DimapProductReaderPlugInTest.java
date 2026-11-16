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

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DimapProductReaderPlugInTest {

    private final static DimapProductReaderPlugIn _plugIn = new DimapProductReaderPlugIn();
    private ProductReader _productReader;

    @Before
    public void setUp() {
        _productReader = _plugIn.createReaderInstance();
    }

    @After
    public void tearDown() {
        _productReader = null;
    }

    @Test
    public void testPlugInInfoQuery() {
        assertNotNull(_plugIn.getFormatNames());
        assertEquals(1, _plugIn.getFormatNames().length);
        assertEquals(DimapProductConstants.DIMAP_FORMAT_NAME, _plugIn.getFormatNames()[0]);

        assertNotNull(_plugIn.getInputTypes());
        assertEquals(2, _plugIn.getInputTypes().length);

        assertNotNull(_plugIn.getDescription(null));
    }

    @Test
    public void testCanDecodeInput() {
        Path file = GlobalTestConfig.getSnapTestDataOutputFile("DIMAP/test2.dim");
        try {
            IOHelper.createDirectories(file.getParent());
            IOHelper.createFile(file);
            final Writer writer = Files.newBufferedWriter(file);
            writer.write("This file must contain the String '<Dimap_Document' to ensure this is a correct file format");
            writer.close();
            assertEquals(DecodeQualification.INTENDED, _plugIn.getDecodeQualification(file));
            assertEquals(DecodeQualification.INTENDED, _plugIn.getDecodeQualification(file.toString()));
            if (!IOHelper.delete(file)) {
                IOHelper.deleteOnExit(file);
            }
            if (!IOHelper.delete(file.getParent())) {
                IOHelper.deleteOnExit(file.getParent());
            }
        } catch (IOException e) {
            fail("DimapProductReaderPlugInTest: failed to create test file " + file);
        }
    }

    @Test
    public void testCreatedProductReaderInstance() {
        assertNotNull(_productReader);
        assertTrue(_productReader instanceof DimapProductReader);
    }

    @Test
    public void testReadProductNodes() {
        Path inputFile = GlobalTestConfig.getSnapTestDataOutputFile("DIMAP/test2.dim");
        Product product = null;
        try {
            product = _productReader.readProductNodes(inputFile, null);
        } catch (IllegalFileFormatException e) {
            fail("unexpected IllegalFileFormatException: " + e.getMessage());
        } catch (IOException e) {
            // expected exception
        }
        assertNull(product);
    }
}
