package org.esa.lib.gdal.activator;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.gdal.GDALLoader;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class GDALDriverInfoTest {

    private static final String EXTENSION_NAME = ".mock";
    private static final String DRIVER_NAME = "MOCK";
    private static final String DRIVER_DISPLAY_NAME = "MOCK Driver";
    private static final String CREATION_DATA_TYPES = "Byte UInt16 Int16 UInt32 Int32 Float32 Float64 CInt16 CInt32 CFloat32";
    private static final String WRITER_PLUGIN_FORMAT_NAME = "GDAL-" + DRIVER_NAME + "-WRITER";


    private static final GDALLoader TEST_GDAL_LOADER = GDALLoader.getInstance();

    GDALDriverInfo mockDriverInfo;

    @Before
    public void setUp() {
        mockDriverInfo = new GDALDriverInfo(EXTENSION_NAME, DRIVER_NAME, DRIVER_DISPLAY_NAME, CREATION_DATA_TYPES);
        final Path gdalLockFile = SystemUtils.getAuxDataPath().resolve("gdal.lock");
        try {
            if (Files.exists(gdalLockFile)) {
                System.out.println("[GDAL-DIT debug]: setUp delete lock file: " + gdalLockFile);
                Files.delete(gdalLockFile);
            }
        } catch (IOException e) {
            fail("fail to delete lockfile: " + gdalLockFile);
        }
    }

    @Test
    public void testGetDriverDisplayName() {
        System.out.println("[GDAL-DIT debug]: testGetDriverDisplayName start");
        assertEquals(DRIVER_DISPLAY_NAME, mockDriverInfo.getDriverDisplayName());
        System.out.println("[GDAL-DIT debug]: testGetDriverDisplayName end");
    }

    @Test
    public void testGetExtensionName() {
        System.out.println("[GDAL-DIT debug]: testGetExtensionName start");
        assertEquals(EXTENSION_NAME, mockDriverInfo.getExtensionName());
        System.out.println("[GDAL-DIT debug]: testGetExtensionName end");
    }

    @Test
    public void testGetDriverName() {
        System.out.println("[GDAL-DIT debug]: testGetDriverName start");
        assertEquals(DRIVER_NAME, mockDriverInfo.getDriverName());
        System.out.println("[GDAL-DIT debug]: testGetDriverName end");
    }

    @Test
    public void testGetCreationDataTypes() {
        System.out.println("[GDAL-DIT debug]: testGetCreationDataTypes start");
        assertEquals(CREATION_DATA_TYPES, mockDriverInfo.getCreationDataTypes());
        System.out.println("[GDAL-DIT debug]: testGetCreationDataTypes end");
    }

    @Test
    public void testCanExportProduct() {
        System.out.println("[GDAL-DIT debug]: testCanExportProduct start");
        assertNotNull(TEST_GDAL_LOADER);
        GDALLoader.ensureGDALInitialised();
        System.out.println("[GDAL-DIT debug]: testCanExportProduct ensureGDALInitialised");
        assertTrue(mockDriverInfo.canExportProduct(1));
        assertTrue(mockDriverInfo.canExportProduct(2));
        assertTrue(mockDriverInfo.canExportProduct(3));
        assertTrue(mockDriverInfo.canExportProduct(4));
        assertTrue(mockDriverInfo.canExportProduct(5));
        assertTrue(mockDriverInfo.canExportProduct(6));
        assertTrue(mockDriverInfo.canExportProduct(7));
        assertTrue(mockDriverInfo.canExportProduct(8));
        assertTrue(mockDriverInfo.canExportProduct(9));
        assertTrue(mockDriverInfo.canExportProduct(10));
        assertFalse(mockDriverInfo.canExportProduct(11));
        System.out.println("[GDAL-DIT debug]: testCanExportProduct end");
    }

    @Test
    public void testGetWriterPluginFormatName() {
        System.out.println("[GDAL-DIT debug]: testGetWriterPluginFormatName start");
        assertEquals(WRITER_PLUGIN_FORMAT_NAME, mockDriverInfo.getWriterPluginFormatName());
        System.out.println("[GDAL-DIT debug]: testGetWriterPluginFormatName end");
    }

}
