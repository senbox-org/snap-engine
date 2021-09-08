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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ZNAP_CONTAINER_EXTENSION;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ZNAP_ZIP_CONTAINER_EXTENSION;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE;
import static org.junit.Assert.assertTrue;


public class ZnapProductWriterTest_writeToArchiveOrFolder {

    private static Path baseTestPath;
    private Product dummy;
    private ZnapProductWriter writer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final String tmpDir = System.getProperty("java.io.tmpdir");
        // deleting temp directory in @After or @AfterClass method didn't work reliable. Probably sometimes some
        // files were still in use at the time delete was called. So we try, delete on exit with a shutdown hook.
        // We have one common test dir. Each test creates its own folder
        baseTestPath = Paths.get(tmpDir, ZnapProductWriteAndReadTest_allAvailableBinaryWriters.class.getCanonicalName());
        deleteRemainingsOfPreviousRun();
        Files.createDirectories(baseTestPath);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                TreeDeleter.deleteDir(baseTestPath);
            } catch (IOException ignore) {
            }
        }));
    }

    private static void deleteRemainingsOfPreviousRun() throws IOException {
        if (Files.isDirectory(baseTestPath)) {
            TreeDeleter.deleteDir(baseTestPath);
        }
    }

    @Before
    public void setUp() throws Exception {
        dummy = createDummyProduct();
        writer = new ZnapProductWriter(new ZnapProductWriterPlugIn());
    }

    @Test
    public void testFileExtension_writeToFolder() throws IOException {
        //preparation
        final Properties properties = new Properties();
        properties.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");
        writer.setPreferencesForTestPurposesOnly(properties);
        final Path testDir = baseTestPath.resolve("writeToFolder");

        //execution
        writer.writeProductNodes(dummy, testDir.resolve("filename"));

        //verification
        assertTrue(Files.isDirectory(testDir.resolve("filename" + ZNAP_CONTAINER_EXTENSION)));
    }

    @Test
    public void testFileExtension_writeToZipArchive() throws IOException {
        //preparation
        final Properties properties = new Properties();
        properties.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "true");
        writer.setPreferencesForTestPurposesOnly(properties);
        final Path testDir = baseTestPath.resolve("writeToZipArchive");

        //execution
        writer.writeProductNodes(dummy, testDir.resolve("filename"));

        //verification
        assertTrue(Files.isRegularFile(testDir.resolve("filename" + ZNAP_ZIP_CONTAINER_EXTENSION)));
    }

    private Product createDummyProduct() {
        final Product targetProduct = new Product("name", "type");
        Band band = new Band("band", ProductData.TYPE_INT32, 2, 2);
        band.setData(ProductData.createInstance(new int[]{12, 13, 14, 15}));
        targetProduct.addBand(band);
        return targetProduct;
    }
}