/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 *
 */

package org.esa.snap.dataio.znap;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.TreeDeleter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ZNAP_CONTAINER_EXTENSION;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ZNAP_ZIP_CONTAINER_EXTENSION;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE;
import static org.junit.Assert.assertTrue;


public class ZnapProductWriterTest_writeToArchiveOrFolder {

    private Path testPath;
    private Product dummy;
    private ZnapProductWriter writer;

    @Before
    public void setUp() throws Exception {
        testPath = Files.createTempDirectory(getClass().getCanonicalName());
        dummy = createDummyProduct();
        writer = new ZnapProductWriter(new ZnapProductWriterPlugIn());
    }

    @After
    public void tearDown() throws IOException {
        TreeDeleter.deleteDir(testPath);
    }

    @Test
    public void testFileExtension_writeToFolder() throws IOException {
        //preparation
        final Properties properties = new Properties();
        properties.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");
        writer.setPreferencesForTestPurposesOnly(properties);

        //execution
        writer.writeProductNodes(dummy, testPath.resolve("filename"));

        //verification
        assertTrue(Files.isDirectory(testPath.resolve("filename" + ZNAP_CONTAINER_EXTENSION)));
    }

    @Test
    public void testFileExtension_writeToZipArchive() throws IOException {
        //preparation
        final Properties properties = new Properties();
        properties.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "true");
        writer.setPreferencesForTestPurposesOnly(properties);

        //execution
        writer.writeProductNodes(dummy, testPath.resolve("filename"));

        //verification
        assertTrue(Files.isRegularFile(testPath.resolve("filename" + ZNAP_ZIP_CONTAINER_EXTENSION)));
    }

    private Product createDummyProduct() {
        final Product targetProduct = new Product("name", "type");
        Band band = new Band("band", ProductData.TYPE_INT32, 2, 2);
        band.setData(ProductData.createInstance(new int[]{12, 13, 14, 15}));
        targetProduct.addBand(band);
        return targetProduct;
    }
}