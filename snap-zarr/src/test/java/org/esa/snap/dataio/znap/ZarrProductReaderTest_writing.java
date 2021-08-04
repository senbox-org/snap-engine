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
import org.esa.snap.runtime.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;

import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.DEFAULT_USE_ZIP_ARCHIVE;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE;
import static org.junit.Assert.assertTrue;


public class ZarrProductReaderTest_writing {

    private Path testPath;

    @Before
    public void setUp() throws Exception {
        testPath = Files.createTempDirectory("ZarrProductReaderTest_writing");
        Files.createDirectories(testPath);
    }

    @After
    public void tearDown() throws IOException {
        TreeDeleter.deleteDir(testPath);
    }

    @Test
    public void testFileExtension() throws IOException {
        final Product dummy = createDummyProduct();

        ZarrProductWriter writer = new ZarrProductWriter(new ZarrProductWriterPlugIn());
        writer.writeProductNodes(dummy, testPath.resolve("filename"));

        String expectedExtension = getExpectedExtension();

        assertTrue(testPath.resolve("filename" + expectedExtension).toFile().exists());

    }

    private String getExpectedExtension() {
        String expectedExtension = ZnapConstantsAndUtils.SNAP_ZARR_CONTAINER_EXTENSION;
        if (isUsingZipArchive()) {
            expectedExtension = ZnapConstantsAndUtils.SNAP_ZARR_ZIP_CONTAINER_EXTENSION;
        }
        return expectedExtension;
    }

    private boolean isUsingZipArchive() {
        final Preferences preferences = Config.instance("snap").load().preferences();
        return preferences.getBoolean(PROPERTY_NAME_USE_ZIP_ARCHIVE, DEFAULT_USE_ZIP_ARCHIVE);
    }

    private Product createDummyProduct() {
        final Product targetProduct = new Product("name", "type");
        Band band = new Band("band", ProductData.TYPE_INT32, 2, 2);
        band.setData(ProductData.createInstance(new int[]{12, 13, 14, 15}));
        targetProduct.addBand(band);
        return targetProduct;
    }
}