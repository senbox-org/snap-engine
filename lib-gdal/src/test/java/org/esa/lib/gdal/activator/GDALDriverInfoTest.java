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
    private static final String LOCK_FILE_NAME = "gdal.lock";


    private static final GDALLoader TEST_GDAL_LOADER = GDALLoader.getInstance();

    GDALDriverInfo mockDriverInfo;

    @Before
    public void setUp() {
        mockDriverInfo = new GDALDriverInfo(EXTENSION_NAME, DRIVER_NAME, DRIVER_DISPLAY_NAME, CREATION_DATA_TYPES);
        final Path lockFile = SystemUtils.getAuxDataPath().resolve(LOCK_FILE_NAME);
        try {
            if (Files.exists(lockFile)) {
                Files.delete(lockFile);
            }
        } catch (IOException e) {
            fail("fail to delete lockfile: " + lockFile);
        }
    }

    @Test
    public void testGetDriverDisplayName() {
        assertEquals(DRIVER_DISPLAY_NAME, mockDriverInfo.getDriverDisplayName());
    }

    @Test
    public void testGetExtensionName() {
        assertEquals(EXTENSION_NAME, mockDriverInfo.getExtensionName());
    }

    @Test
    public void testGetDriverName() {
        assertEquals(DRIVER_NAME, mockDriverInfo.getDriverName());
    }

    @Test
    public void testGetCreationDataTypes() {
        assertEquals(CREATION_DATA_TYPES, mockDriverInfo.getCreationDataTypes());
    }

    @Test
    public void testCanExportProduct() {
        assertNotNull(TEST_GDAL_LOADER);
        GDALLoader.ensureGDALInitialised();
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
    }

    @Test
    public void testGetWriterPluginFormatName() {
        assertEquals(WRITER_PLUGIN_FORMAT_NAME, mockDriverInfo.getWriterPluginFormatName());
    }

}
