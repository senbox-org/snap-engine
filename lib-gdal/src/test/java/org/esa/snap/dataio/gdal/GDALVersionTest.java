package org.esa.snap.dataio.gdal;

import com.bc.ceres.annotation.STTM;
import org.esa.lib.gdal.AbstractGDALTest;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.esa.snap.dataio.gdal.GDALVersion.DIR_NAME;
import static org.esa.snap.dataio.gdal.GDALVersion.GDAL_JNI_LIBRARY_FILE;
import static org.esa.snap.dataio.gdal.GDALVersion.GDAL_NATIVE_LIBRARIES_SRC;
import static org.esa.snap.dataio.gdal.GDALVersion.VERSION_NAME;
import static org.esa.snap.dataio.gdal.GDALVersion.ZIP_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GDALVersionTest {

    private static final GDALVersion TEST_VERSION = GDALVersion.getInternalVersion();

    private static String getExpectedDirName(String name) {
        return DIR_NAME.replace(VERSION_NAME, name);
    }

    private static String getExpectedZipName(String name) {
        return ZIP_NAME.replace(VERSION_NAME, name);
    }

    static Path getExpectedGDALVersionLocation(GDALVersion gdalVersion) {
        return AbstractGDALTest.getExpectedNativeLibrariesRootFolderPath().resolve(getExpectedDirName(gdalVersion.name));
    }

    private static String getExpectedLocation() {
        return getExpectedGDALVersionLocation(TEST_VERSION).toString();
    }

    private static URL getExpectedZipFileURLFromSources() {
        return GDALVersion.class.getClassLoader().getResource(GDAL_NATIVE_LIBRARIES_SRC + "/" + AbstractGDALTest.getExpectedDirectory() + "/" + getExpectedZipName(TEST_VERSION.name).replace(File.separator, "/"));
    }

    private static Path getExpectedZipFilePath() {
        return getExpectedGDALVersionLocation(TEST_VERSION).resolve(getExpectedZipName(TEST_VERSION.name));
    }

    private Path getExpectedJNILibraryFilePath() {
        return getExpectedGDALVersionLocation(TEST_VERSION).resolve(GDAL_JNI_LIBRARY_FILE);
    }

    @Test
    public void testGetGDALVersion() {
        assertEquals(TEST_VERSION, GDALVersion.getGDALVersion());
    }

    @Test
    public void testGetInternalVersion() {
        assertEquals(TEST_VERSION.id, GDALVersion.getInternalVersion().id);
    }

    @Test
    public void testGetId() {
        assertEquals(TEST_VERSION.id, TEST_VERSION.getId());
    }

    @Test
    public void testSetId() {
        final String prevId = TEST_VERSION.id;
        TEST_VERSION.setId("mock");
        assertEquals("mock", TEST_VERSION.id);
        TEST_VERSION.setId(prevId);
    }

    @Test
    public void testGetLocation() {
        assertEquals(getExpectedLocation(), TEST_VERSION.location);
        assertEquals(TEST_VERSION.location, TEST_VERSION.getLocation());
    }

    @Test
    public void testIsCOGCapable() {
        assertTrue(TEST_VERSION.isCOGCapable());
    }

    @Test
    public void testGetZipFileURLFromSources() {
        assertEquals(getExpectedZipFileURLFromSources(), TEST_VERSION.getZipFileURLFromSources());
        try {
            assertTrue(Files.exists(Paths.get(Objects.requireNonNull(TEST_VERSION.getZipFileURLFromSources()).toURI())));
        } catch (Exception e) {
            fail("Error on testGetZipFileURLFromSources(): " + e.getMessage());
        }
    }

    @Test
    public void testGetZipFilePath() {
        assertEquals(getExpectedZipFilePath(), TEST_VERSION.getZipFilePath());
        assertFalse(Files.exists(TEST_VERSION.getZipFilePath()));
    }

    @Test
    @STTM("SNAP-3523")
    public void testGetNativeLibrariesRootFolderPath() {
        assertEquals(AbstractGDALTest.getExpectedNativeLibrariesRootFolderPath(), GDALVersion.getNativeLibrariesRootFolderPath());
    }

    @Test
    public void testGetJNILibraryFilePath() {
        assertEquals(getExpectedJNILibraryFilePath(), TEST_VERSION.getJNILibraryFilePath());
    }
}
