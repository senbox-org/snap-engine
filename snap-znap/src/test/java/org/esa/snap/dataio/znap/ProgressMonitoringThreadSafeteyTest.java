/*
 * Copyright (c) 2022.  Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.TreeDeleter;
import org.esa.snap.runtime.Config;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE;


/**
 * @author Marco Peters
 */
@Ignore("Should only be used locally to test issues with threading. Enable num executes in Idea to execute the test multiple times.")
public class ProgressMonitoringThreadSafeteyTest {
    private static Path baseTestPath;
    private Product dummy;
    private static String useZipArchiveBackup;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final String tmpDir = System.getProperty("java.io.tmpdir");
        // deleting temp directory in @After or @AfterClass method didn't work reliable. Probably sometimes some
        // files were still in use at the time delete was called. So we try, delete on exit with a shutdown hook.
        // We have one common test dir. Each test creates its own folder
        baseTestPath = Paths.get(tmpDir, ZnapProductWriterTest_writeToArchiveOrFolder.class.getCanonicalName());
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
        final Preferences prefs = Config.instance("snap").load().preferences();
        useZipArchiveBackup = prefs.get(PROPERTY_NAME_USE_ZIP_ARCHIVE, null);
        prefs.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");

    }

    @After
    public void tearDown() {
        try {
            TreeDeleter.deleteDir(baseTestPath);
        } catch (IOException ignore) {
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        final Preferences prefs = Config.instance("snap").load().preferences();
        if (useZipArchiveBackup == null) {
            prefs.remove(PROPERTY_NAME_USE_ZIP_ARCHIVE);
        } else {
            prefs.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, useZipArchiveBackup);
        }
    }

    @Test
    public void testMethod() throws IOException {
        final Path testDir = baseTestPath.resolve("writeToFolder");
        ProductIO.writeProduct(dummy, testDir.resolve("testName").toFile(), "ZNAP", false);
    }

    private Product createDummyProduct() {
        final Product targetProduct = new Product("name", "type");
        for (int i = 0; i < 25; i++) {
            Band band = new Band("band_0" + i, ProductData.TYPE_INT32, 5000, 2000);
            band.setSourceImage(ConstantDescriptor.create(5000f, 2000f, new Integer[]{i}, null));
            targetProduct.addBand(band);
        }
        for (int i = 0; i < 50; i++) {
            Band band = new Band("band_1" + i, ProductData.TYPE_INT32, 1000, 200);
            band.setSourceImage(ConstantDescriptor.create(1000f, 200f, new Integer[]{i}, null));
            targetProduct.addBand(band);
        }
        for (int i = 0; i < 25; i++) {
            Band band = new Band("band_2" + i, ProductData.TYPE_INT32, 5000, 2000);
            band.setSourceImage(ConstantDescriptor.create(5000f, 2000f, new Integer[]{i}, null));
            targetProduct.addBand(band);
        }
        return targetProduct;
    }
}