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
import org.esa.snap.dataio.bigtiff.BigGeoTiffProductWriterPlugIn;
import org.esa.snap.dataio.envi.EnviProductWriterPlugIn;
import org.esa.snap.dataio.geotiff.GeoTiffProductWriterPlugIn;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfNetCdf4WriterPlugIn;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;
import org.junit.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.prefs.Preferences;

import static com.bc.ceres.core.ProgressMonitor.NULL;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ZNAP_CONTAINER_EXTENSION;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_BINARY_FORMAT;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class ZnapProductWriteAndReadTest_allAvailableBinaryWriters {

    private static Path baseTestPath;
    private Product dummy;
    private String oldValue;
    private Preferences preferences;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final String tmpDir = System.getProperty("java.io.tmpdir");
        Engine.start();
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

        preferences = Config.instance("snap").load().preferences();
        oldValue = preferences.get(PROPERTY_NAME_USE_ZIP_ARCHIVE, "true");
    }

    @After
    public void tearDown() {
        preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, oldValue);
    }

    @Test
    @Ignore("Related to NativeLibraryUtils")
    public void testWriteAndRead_withBinaryWriter_ENVI() throws IOException {
        //preparation
        final Properties properties = new Properties();
        properties.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");
        properties.put(PROPERTY_NAME_BINARY_FORMAT, EnviProductWriterPlugIn.FORMAT_NAME);
        final ZnapProductWriter writer = new ZnapProductWriter(new ZnapProductWriterPlugIn(), properties);
        writer.setPreferencesForTestPurposesOnly(properties);

        preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");

        final Band band = dummy.getBand("band");

        final Path testDir = baseTestPath.resolve("ENVI");

        //execution
        try {
            writer.writeProductNodes(dummy, testDir.resolve("filename"));
            writer.writeBandRasterData(band, 0, 0, 2, 2, band.getData(), NULL);
        } finally {
            writer.close();
        }

        //verification
        final Path rootDir = testDir.resolve("filename" + ZNAP_CONTAINER_EXTENSION);
        assertTrue(Files.isDirectory(rootDir));
        assertTrue(Files.isDirectory(rootDir.resolve("band")));
        assertTrue(Files.isDirectory(rootDir.resolve("band").resolve("band")));
        assertTrue(Files.isRegularFile(rootDir.resolve("band").resolve("band").resolve("data.img")));
        assertTrue(Files.isRegularFile(rootDir.resolve("band").resolve("band").resolve("data.hdr")));

        final ZnapProductReader reader = new ZnapProductReader(new ZnapProductReaderPlugIn());
        try {
            final Product product = reader.readProductNodes(rootDir, null);
            final Band readInBand = product.getBand("band");
            readInBand.readRasterDataFully();
            final int[] ints = (int[]) readInBand.getData().getElems();
            assertNotNull(ints);
            assertArrayEquals(new int[]{12, 13, 14, 15}, ints);
        } finally {
            reader.close();
        }
    }

    @Test
    @Ignore("Related to NativeLibraryUtils")
    public void testWriteAndRead_withBinaryWriter_GeoTIFF() throws IOException {
        //preparation
        final Properties properties = new Properties();
        properties.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");
        properties.put(PROPERTY_NAME_BINARY_FORMAT, GeoTiffProductWriterPlugIn.GEOTIFF_FORMAT_NAME);
        final ZnapProductWriter writer = new ZnapProductWriter(new ZnapProductWriterPlugIn(), properties);
        writer.setPreferencesForTestPurposesOnly(properties);

        preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");

        final Band band = dummy.getBand("band");

        //execution
        final Path testDir = baseTestPath.resolve("GeoTiff");
        try {
            writer.writeProductNodes(dummy, testDir.resolve("filename"));
            writer.writeBandRasterData(band, 0, 0, 2, 2, band.getData(), NULL);
        } finally {
            writer.close();
        }

        //verification
        final Path rootDir = testDir.resolve("filename" + ZNAP_CONTAINER_EXTENSION);
        assertTrue(Files.isDirectory(rootDir));
        assertTrue(Files.isDirectory(rootDir.resolve("band")));
        assertTrue(Files.isRegularFile(rootDir.resolve("band").resolve("band.tif")));

        final ZnapProductReader reader = new ZnapProductReader(new ZnapProductReaderPlugIn());
        try {
            final Product product = reader.readProductNodes(rootDir, null);
            final Band readInBand = product.getBand("band");
            readInBand.readRasterDataFully();
            final int[] ints = (int[]) readInBand.getData().getElems();
            assertNotNull(ints);
            assertArrayEquals(new int[]{12, 13, 14, 15}, ints);
        } finally {
            reader.close();
        }
    }

    @Test
    @Ignore("Related to NativeLibraryUtils")
    public void testWriteAndRead_withBinaryWriter_GeoTIFF_BigTIFF() throws IOException {
        //preparation
        final Properties properties = new Properties();
        properties.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");
        properties.put(PROPERTY_NAME_BINARY_FORMAT, BigGeoTiffProductWriterPlugIn.FORMAT_NAME);
        final ZnapProductWriter writer = new ZnapProductWriter(new ZnapProductWriterPlugIn(), properties);
        writer.setPreferencesForTestPurposesOnly(properties);

        preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");

        final Band band = dummy.getBand("band");
        final Path testDir = baseTestPath.resolve("GeoTIFF_BigTIFF");

        //execution
        try {
            writer.writeProductNodes(dummy, testDir.resolve("filename"));
            writer.writeBandRasterData(band, 0, 0, 2, 2, band.getData(), NULL);
        } finally {
            writer.close();
        }

        //verification
        final Path rootDir = testDir.resolve("filename" + ZNAP_CONTAINER_EXTENSION);
        assertTrue(Files.isDirectory(rootDir));
        assertTrue(Files.isDirectory(rootDir.resolve("band")));
        assertTrue(Files.isRegularFile(rootDir.resolve("band").resolve("band.tif")));

        final ZnapProductReader reader = new ZnapProductReader(new ZnapProductReaderPlugIn());
        try {
            final Product product = reader.readProductNodes(rootDir, null);
            final Band readInBand = product.getBand("band");
            readInBand.readRasterDataFully();
            final int[] ints = (int[]) readInBand.getData().getElems();
            assertNotNull(ints);
            assertArrayEquals(new int[]{12, 13, 14, 15}, ints);
        } finally {
            reader.close();
        }
    }

    @Test
    @Ignore("Related to NativeLibraryUtils")
    public void testWriteAndRead_withBinaryWriter_NetCDF4_CF() throws IOException {
        //preparation
        final Properties properties = new Properties();
        properties.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");
        properties.put(PROPERTY_NAME_BINARY_FORMAT, new CfNetCdf4WriterPlugIn().getFormatNames()[0]);
        final ZnapProductWriter writer = new ZnapProductWriter(new ZnapProductWriterPlugIn(), properties);
        writer.setPreferencesForTestPurposesOnly(properties);

        preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");

        final Band band = dummy.getBand("band");

        //execution
        final Path testDir = baseTestPath.resolve("NetCDF4_CF");
        try {
            writer.writeProductNodes(dummy, testDir.resolve("filename"));
            writer.writeBandRasterData(band, 0, 0, 2, 2, band.getData(), NULL);
        } finally {
            writer.close();
        }

        //verification
        final Path rootDir = testDir.resolve("filename" + ZNAP_CONTAINER_EXTENSION);
        assertTrue(Files.isDirectory(rootDir));
        assertTrue(Files.isDirectory(rootDir.resolve("band")));
        assertTrue(Files.isRegularFile(rootDir.resolve("band").resolve("band.nc")));

        final ZnapProductReader reader = new ZnapProductReader(new ZnapProductReaderPlugIn());
        try {
            final Product product = reader.readProductNodes(rootDir, null);
            final Band readInBand = product.getBand("band");
            readInBand.readRasterDataFully();
            final int[] ints = (int[]) readInBand.getData().getElems();
            assertNotNull(ints);
            assertArrayEquals(new int[]{12, 13, 14, 15}, ints);
        } finally {
            reader.close();
        }
    }

    private Product createDummyProduct() {
        final Product targetProduct = new Product("name", "type");
        Band band = new Band("band", ProductData.TYPE_INT32, 2, 2);
        band.setData(ProductData.createInstance(new int[]{12, 13, 14, 15}));
        targetProduct.addBand(band);
        return targetProduct;
    }

}