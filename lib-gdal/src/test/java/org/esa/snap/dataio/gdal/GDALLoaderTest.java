package org.esa.snap.dataio.gdal;

import org.esa.lib.gdal.AbstractGDALTest;
import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.gdal.drivers.GDAL;
import org.esa.snap.dataio.gdal.drivers.GDALConst;
import org.esa.snap.dataio.gdal.drivers.GDALConstConstants;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class GDALLoaderTest extends AbstractGDALTest {

    private static final String TEST_FILE = "U_1005A.NTF";

    private static final GDALLoader TEST_GDAL_LOADER = GDALLoader.getInstance();
    private static final GDALVersion TEST_GDAL_VERSION = GDALVersion.getGDALVersion();

    private Path testFilePath;

    private static URLClassLoader getExpectedGDALVersionLoader() throws Exception {
        return new URLClassLoader(new URL[]{TEST_GDAL_VERSION.getJNILibraryFilePath().toUri().toURL(), GDALVersion.getLoaderLibraryFilePath().toUri().toURL()}, GDALLoader.class.getClassLoader());
    }

    private static int getExpectedGDALDataType(int bandDataType) {
        switch (bandDataType) {
            case ProductData.TYPE_INT8:
            case ProductData.TYPE_UINT8:
                return GDALConstConstants.gdtByte();
            case ProductData.TYPE_INT16:
                return GDALConstConstants.gdtInt16();
            case ProductData.TYPE_UINT16:
                return GDALConstConstants.gdtUint16();
            case ProductData.TYPE_INT32:
                return GDALConstConstants.gdtInt32();
            case ProductData.TYPE_UINT32:
                return GDALConstConstants.gdtUint32();
            case ProductData.TYPE_FLOAT32:
                return GDALConstConstants.gdtFloat32();
            case ProductData.TYPE_FLOAT64:
                return GDALConstConstants.gdtFloat64();
            default:
                return -1;
        }
    }

    private static int getExpectedBandDataType(int gdalDataType) {
        if (gdalDataType == GDALConstConstants.gdtByte()) {
            return ProductData.TYPE_UINT8;
        } else if (gdalDataType == GDALConstConstants.gdtInt16()) {
            return ProductData.TYPE_INT16;
        } else if (gdalDataType == GDALConstConstants.gdtUint16()) {
            return ProductData.TYPE_UINT16;
        } else if (gdalDataType == GDALConstConstants.gdtInt32()) {
            return ProductData.TYPE_INT32;
        } else if (gdalDataType == GDALConstConstants.gdtUint32()) {
            return ProductData.TYPE_UINT32;
        } else if (gdalDataType == GDALConstConstants.gdtFloat32()) {
            return ProductData.TYPE_FLOAT32;
        } else if (gdalDataType == GDALConstConstants.gdtFloat64()) {
            return ProductData.TYPE_FLOAT64;
        } else {
            return -1;
        }
    }

    @Before
    public void setUpLocal() {
        testFilePath = this.libGDALTestsFolderPath.resolve(TEST_FILE);
        assumeTrue(Files.exists(testFilePath));
    }

    @Test
    public void testGetInstance() {
        assertNotNull(GDALLoader.getInstance());
    }

    @Test
    public void testInitGDAL() {
        try {
            assertNotNull(TEST_GDAL_LOADER);
            GDALLoader.ensureGDALInitialised();
            assertTrue(Files.exists(getExpectedNativeLibrariesRootFolderPath()));
            assertTrue(GDALInstallInfo.INSTANCE.isPresent());
            assertNotNull(GDAL.open(testFilePath.toString(), GDALConst.gaReadonly()));
        } catch (Exception e) {
            fail("Error on testInitGDAL(): " + e.getMessage());
        }
    }

    @Test
    public void testGetGDALVersionLoader() {
        try {
            assertNotNull(TEST_GDAL_LOADER);
            GDALLoader.ensureGDALInitialised();
            final URLClassLoader expectedURLClassLoader = getExpectedGDALVersionLoader();
            assertArrayEquals(expectedURLClassLoader.getURLs(), TEST_GDAL_LOADER.getGDALVersionLoader().getURLs());
            assertEquals(expectedURLClassLoader.getParent(), TEST_GDAL_LOADER.getGDALVersionLoader().getParent());
        } catch (Exception e) {
            fail("Error on testGetGDALVersionLoader(): " + e.getMessage());
        }
    }

    @Test
    public void testGetGDALDataType() {
        try {
            assertNotNull(TEST_GDAL_LOADER);
            GDALLoader.ensureGDALInitialised();
            assertEquals(getExpectedGDALDataType(ProductData.TYPE_INT8), TEST_GDAL_LOADER.getGDALDataType(ProductData.TYPE_INT8));
            assertEquals(getExpectedGDALDataType(ProductData.TYPE_UINT8), TEST_GDAL_LOADER.getGDALDataType(ProductData.TYPE_UINT8));
            assertEquals(getExpectedGDALDataType(ProductData.TYPE_INT16), TEST_GDAL_LOADER.getGDALDataType(ProductData.TYPE_INT16));
            assertEquals(getExpectedGDALDataType(ProductData.TYPE_UINT16), TEST_GDAL_LOADER.getGDALDataType(ProductData.TYPE_UINT16));
            assertEquals(getExpectedGDALDataType(ProductData.TYPE_INT32), TEST_GDAL_LOADER.getGDALDataType(ProductData.TYPE_INT32));
            assertEquals(getExpectedGDALDataType(ProductData.TYPE_UINT32), TEST_GDAL_LOADER.getGDALDataType(ProductData.TYPE_UINT32));
            assertEquals(getExpectedGDALDataType(ProductData.TYPE_FLOAT32), TEST_GDAL_LOADER.getGDALDataType(ProductData.TYPE_FLOAT32));
            assertEquals(getExpectedGDALDataType(ProductData.TYPE_FLOAT64), TEST_GDAL_LOADER.getGDALDataType(ProductData.TYPE_FLOAT64));
        } catch (Exception e) {
            fail("Error on testGetGDALDataType(): " + e.getMessage());
        }
    }

    @Test
    public void testGetBandDataType() {
        try {
            assertNotNull(TEST_GDAL_LOADER);
            GDALLoader.ensureGDALInitialised();
            assertEquals(getExpectedBandDataType(GDALConstConstants.gdtByte()), TEST_GDAL_LOADER.getBandDataType(GDALConstConstants.gdtByte()));
            assertEquals(getExpectedBandDataType(GDALConstConstants.gdtInt16()), TEST_GDAL_LOADER.getBandDataType(GDALConstConstants.gdtInt16()));
            assertEquals(getExpectedBandDataType(GDALConstConstants.gdtUint16()), TEST_GDAL_LOADER.getBandDataType(GDALConstConstants.gdtUint16()));
            assertEquals(getExpectedBandDataType(GDALConstConstants.gdtInt32()), TEST_GDAL_LOADER.getBandDataType(GDALConstConstants.gdtInt32()));
            assertEquals(getExpectedBandDataType(GDALConstConstants.gdtUint32()), TEST_GDAL_LOADER.getBandDataType(GDALConstConstants.gdtUint32()));
            assertEquals(getExpectedBandDataType(GDALConstConstants.gdtFloat32()), TEST_GDAL_LOADER.getBandDataType(GDALConstConstants.gdtFloat32()));
            assertEquals(getExpectedBandDataType(GDALConstConstants.gdtFloat64()), TEST_GDAL_LOADER.getBandDataType(GDALConstConstants.gdtFloat64()));
        } catch (Exception e) {
            fail("Error on testGetBandDataType(): " + e.getMessage());
        }
    }

    @Test
    public void testGetGDALVersion() {
        try {
            assertNotNull(TEST_GDAL_LOADER);
            GDALLoader.ensureGDALInitialised();
            assertEquals(TEST_GDAL_VERSION, TEST_GDAL_LOADER.getGdalVersion());
        } catch (Exception e) {
            fail("Error on testGetGDALVersion(): " + e.getMessage());
        }
    }

}
