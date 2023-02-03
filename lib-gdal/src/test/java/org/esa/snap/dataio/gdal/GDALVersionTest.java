package org.esa.snap.dataio.gdal;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.apache.commons.lang.SystemUtils.*;
import static org.esa.snap.dataio.gdal.GDALLoaderConfig.*;
import static org.esa.snap.dataio.gdal.GDALVersion.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class GDALVersionTest {

    private static final boolean USE_INSTALLED_GDAL_LIBRARY = true;
    private static final boolean USE_INTERNAL_GDAL_LIBRARY = false;

    private static final String NONE_SELECTED_INSTALLED_GDAL_LIBRARY = "none";

    private static final GDALVersion TEST_VERSION_JNI = GDALVersion.GDAL_32X_JNI;
    private static final GDALVersion TEST_VERSION = GDALVersion.GDAL_321_FULL;

    private Boolean currentUseInstalledGDALLibrary;
    private String currentSelectedInstalledGDALLibrary;

    private Preferences fetchPreferences() {
        return Config.instance(CONFIG_NAME).load().preferences();
    }

    private static OSCategory getExpectedOSCategory() {
        if (IS_OS_LINUX) {
            return OSCategory.LINUX_64;
        } else if (IS_OS_MAC_OSX) {
            return OSCategory.MAC_OS_X;
        } else if (IS_OS_WINDOWS) {
            final String sysArch = System.getProperty("os.arch").toLowerCase();
            if (sysArch.contains("amd64") || sysArch.contains("x86_x64")) {
                return OSCategory.WIN_64;
            } else {
                return OSCategory.WIN_32;
            }
        } else {
            return OSCategory.UNSUPPORTED;
        }
    }

    private static String getExpectedDirName(boolean jni, String name) {
        if (jni) {
            return DIR_NAME.replace(VERSION_NAME, name).replace(JNI_NAME, "-jni");
        } else {
            return DIR_NAME.replace(VERSION_NAME, name).replace(JNI_NAME, "");
        }
    }

    private static String getExpectedZipName(boolean jni, String name) {
        if (jni) {
            return ZIP_NAME.replace(VERSION_NAME, name).replace(JNI_NAME, "-jni");
        } else {
            return ZIP_NAME.replace(VERSION_NAME, name).replace(JNI_NAME, "");
        }
    }

    private static String getExpectedDirectory() {
        OSCategory osCategory = getExpectedOSCategory();
        return osCategory.getOperatingSystemName() + "/" + osCategory.getArchitecture();
    }

    private static Map<String, GDALVersion> retrieveExpectedInstalledVersions() {
        final Map<String, GDALVersion> gdalVersions = new LinkedHashMap<>();
        final OSCategory osCategory = getExpectedOSCategory();
        final String[] installedVersionsPaths = osCategory.getExecutableLocations(GDALINFIO_EXECUTABLE_NAME);
        if (installedVersionsPaths.length < 1) {
            return null;
        }
        Arrays.stream(installedVersionsPaths).forEach(installedVersionLocation -> {
            try {
                final String result = fetchProcessOutput(Runtime.getRuntime().exec(new String[]{installedVersionLocation + File.separator + GDALINFIO_EXECUTABLE_NAME, GDALINFO_EXECUTABLE_ARGS}));
                final String versionId = result.replaceAll("[\\s\\S]*?(\\d*\\.\\d*\\.\\d*)[\\s\\S]*$", "$1");
                final String version = versionId.replaceAll("(\\d*\\.\\d*)[\\s\\S]*$", "$1.x");
                final GDALVersion InstalledGDALVersion = JNI_VERSIONS.get(version);
                if (InstalledGDALVersion != null) {
                    InstalledGDALVersion.setId(versionId);
                    InstalledGDALVersion.setOsCategory(osCategory);
                    gdalVersions.putIfAbsent(version, InstalledGDALVersion);
                }
            } catch (IOException e) {
                //ignore
            }
        });
        return gdalVersions;
    }

    static Path getExpectedGDALVersionLocation(GDALVersion gdalVersion) {
        return getExpectedNativeLibrariesRootFolderPath().resolve(getExpectedDirName(gdalVersion.jni, gdalVersion.name));
    }

    private static String retrieveExpectedInternalVersionLocation() {
        return getExpectedGDALVersionLocation(GDAL_321_FULL).toString();
    }

    private static String retrieveExpectedInstalledVersionLocation(GDALVersion gdalVersion) {
        return Arrays.stream(getExpectedOSCategory().getExecutableLocations(GDALINFIO_EXECUTABLE_NAME)).filter(installedVersionLocation -> {
            try {
                return JNI_VERSIONS.get(fetchProcessOutput(Runtime.getRuntime().exec(new String[]{installedVersionLocation + File.separator + GDALINFIO_EXECUTABLE_NAME, GDALINFO_EXECUTABLE_ARGS})).replaceAll("[\\s\\S]*?(\\d*\\.\\d*\\.\\d*)[\\s\\S]*$", "$1").replaceAll("(\\d*\\.\\d*)[\\s\\S]*$", "$1.x")) == gdalVersion;
            } catch (IOException e) {
                return false;
            }
        }).findFirst().orElse(null);
    }

    private static String getExpectedLocation(GDALVersion gdalVersion) {
        if (gdalVersion.name.equals(GDAL_321_FULL.name)) {
            return retrieveExpectedInternalVersionLocation();
        } else {
            return retrieveExpectedInstalledVersionLocation(gdalVersion);
        }
    }

    static String getExpectedEnvironmentVariablesFileName() {
        return System.mapLibraryName(getExpectedOSCategory().getEnvironmentVariablesFileName());
    }

    private static String getExpectedEnvironmentVariablesDirectory() {
        return GDAL_NATIVE_LIBRARIES_SRC + "/" + getExpectedDirectory() + "/" + getExpectedEnvironmentVariablesFileName();
    }

    private static URL getExpectedZipFileURLFromSources(GDALVersion gdalVersion) {
        return GDALVersion.class.getClassLoader().getResource(GDAL_NATIVE_LIBRARIES_SRC + "/" + getExpectedDirectory() + "/" + getExpectedZipName(gdalVersion.jni, gdalVersion.name).replace(File.separator, "/"));
    }

    private static Path getExpectedZipFilePath(GDALVersion gdalVersion) {
        return getExpectedGDALVersionLocation(gdalVersion).resolve(getExpectedZipName(gdalVersion.jni, gdalVersion.name));
    }

    private static URL getExpectedEnvironmentVariablesFilePathFromSources() {
        return GDALVersion.class.getClassLoader().getResource(getExpectedEnvironmentVariablesDirectory().replace(File.separator, "/"));
    }

    static Path getExpectedEnvironmentVariablesFilePath() {
        return getExpectedNativeLibrariesRootFolderPath().resolve(getExpectedEnvironmentVariablesFileName());
    }

    static Path getExpectedNativeLibrariesRootFolderPath() {
        return SystemUtils.getAuxDataPath().resolve(GDAL_NATIVE_LIBRARIES_ROOT);
    }

    private Path getExpectedJNILibraryFilePath(GDALVersion gdalVersion) {
        return getExpectedGDALVersionLocation(gdalVersion).resolve(GDAL_JNI_LIBRARY_FILE);
    }

    @Before
    public void setUp() {
        final Preferences preferences = fetchPreferences();
        currentUseInstalledGDALLibrary = preferences.getBoolean(PREFERENCE_KEY_USE_INSTALLED_GDAL, true);
        currentSelectedInstalledGDALLibrary = preferences.get(PREFERENCE_KEY_SELECTED_INSTALLED_GDAL, PREFERENCE_NONE_VALUE_SELECTED_INSTALLED_GDAL);
        TEST_VERSION_JNI.setOsCategory(OSCategory.getOSCategory());
    }

    @After
    public void cleanUp() {
        try {
            final Preferences preferences = fetchPreferences();
            final GDALLoaderConfig gdalLoaderConfig = GDALLoaderConfig.getInstance();
            if (currentUseInstalledGDALLibrary != null) {
                preferences.putBoolean(PREFERENCE_KEY_USE_INSTALLED_GDAL, currentUseInstalledGDALLibrary);
                gdalLoaderConfig.setUseInstalledGDALLibrary(currentUseInstalledGDALLibrary);
            }
            if (currentSelectedInstalledGDALLibrary != null) {
                preferences.put(PREFERENCE_KEY_SELECTED_INSTALLED_GDAL, currentSelectedInstalledGDALLibrary);
                gdalLoaderConfig.setSelectedInstalledGDALLibrary(currentSelectedInstalledGDALLibrary);
            }
            preferences.flush();

        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetGDALVersion() {
        final Map<String, GDALVersion> installedVersions = retrieveExpectedInstalledVersions();
        GDALLoaderConfig.getInstance().setUseInstalledGDALLibrary(USE_INSTALLED_GDAL_LIBRARY);
        if (installedVersions == null || installedVersions.isEmpty()) {
            assertFalse(GDALVersion.getGDALVersion().jni);
        } else {
            assertTrue(GDALVersion.getGDALVersion().jni);
        }
        GDALLoaderConfig.getInstance().setUseInstalledGDALLibrary(USE_INTERNAL_GDAL_LIBRARY);
        assertFalse(GDALVersion.getGDALVersion().jni);
    }

    @Test
    public void testGetSelectedInstalledVersion() {
        final Map<String, GDALVersion> installedVersions = retrieveExpectedInstalledVersions();
        if (installedVersions == null || installedVersions.isEmpty()) {
            assertNull(GDALVersion.getSelectedInstalledVersion());
        } else {
            final GDALVersion selectedGDALVersion = installedVersions.values().iterator().next();
            GDALLoaderConfig.getInstance().setSelectedInstalledGDALLibrary(selectedGDALVersion.name);
            assertNotNull(GDALVersion.getSelectedInstalledVersion());
            assertEquals(selectedGDALVersion.id, GDALVersion.getSelectedInstalledVersion().id);
            GDALLoaderConfig.getInstance().setSelectedInstalledGDALLibrary(NONE_SELECTED_INSTALLED_GDAL_LIBRARY);
            assertNotNull(GDALVersion.getSelectedInstalledVersion());
        }
    }

    @Test
    public void getInstalledVersionsTest() {
        assertEquals(retrieveExpectedInstalledVersions(), getInstalledVersions());
    }

    @Test
    public void testGetInternalVersion() {
        assertEquals(TEST_VERSION.id, GDALVersion.getInternalVersion().id);
    }

    @Test
    public void testGetId() {
        assertEquals(TEST_VERSION.id, TEST_VERSION.getId());
        assertEquals(TEST_VERSION_JNI.id, TEST_VERSION_JNI.getId());
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
        assertEquals(getExpectedLocation(TEST_VERSION), TEST_VERSION.location);
        assertEquals(getExpectedLocation(TEST_VERSION_JNI), TEST_VERSION_JNI.location);
        assertEquals(TEST_VERSION.location, TEST_VERSION.getLocation());
        assertEquals(TEST_VERSION_JNI.location, TEST_VERSION_JNI.getLocation());
    }

    @Test
    public void testGetOsCategory() {
        assertEquals(getExpectedOSCategory(), TEST_VERSION.osCategory);
        assertEquals(getExpectedOSCategory(), TEST_VERSION_JNI.osCategory);
        assertEquals(TEST_VERSION.osCategory, TEST_VERSION.getOsCategory());
        assertEquals(TEST_VERSION_JNI.osCategory, TEST_VERSION_JNI.getOsCategory());
    }

    @Test
    public void testIsJni() {
        assertEquals(TEST_VERSION.jni, TEST_VERSION.isJni());
        assertEquals(TEST_VERSION_JNI.jni, TEST_VERSION_JNI.isJni());
    }

    @Test
    public void testIsCOGCapable() {
        assertEquals(TEST_VERSION.cogCapable, TEST_VERSION.isCOGCapable());
        assertEquals(TEST_VERSION_JNI.cogCapable, TEST_VERSION_JNI.isCOGCapable());
    }

    @Test
    public void testGetZipFileURLFromSources() {
        assertEquals(getExpectedZipFileURLFromSources(TEST_VERSION), TEST_VERSION.getZipFileURLFromSources());
        assertEquals(getExpectedZipFileURLFromSources(TEST_VERSION_JNI), TEST_VERSION_JNI.getZipFileURLFromSources());
        try {
            assertTrue(Files.exists(Paths.get(Objects.requireNonNull(TEST_VERSION.getZipFileURLFromSources()).toURI())));
            assertTrue(Files.exists(Paths.get(Objects.requireNonNull(TEST_VERSION_JNI.getZipFileURLFromSources()).toURI())));
        } catch (Exception e) {
            fail("Error on testGetZipFileURLFromSources(): " + e.getMessage());
        }
    }

    @Test
    public void testGetZipFilePath() {
        assertEquals(getExpectedZipFilePath(TEST_VERSION), TEST_VERSION.getZipFilePath());
        assertEquals(getExpectedZipFilePath(TEST_VERSION_JNI), TEST_VERSION_JNI.getZipFilePath());
        assertFalse(Files.exists(TEST_VERSION.getZipFilePath()));
        assertFalse(Files.exists(TEST_VERSION_JNI.getZipFilePath()));
    }

    @Test
    public void testGetEnvironmentVariablesFilePathFromSources() {
        try {
            assertEquals(getExpectedEnvironmentVariablesFilePathFromSources(), TEST_VERSION.getEnvironmentVariablesFilePathFromSources());
            assertEquals(getExpectedEnvironmentVariablesFilePathFromSources(), TEST_VERSION_JNI.getEnvironmentVariablesFilePathFromSources());
            assertTrue(Files.exists(Paths.get(Objects.requireNonNull(TEST_VERSION.getEnvironmentVariablesFilePathFromSources()).toURI())));
            assertTrue(Files.exists(Paths.get(Objects.requireNonNull(TEST_VERSION_JNI.getEnvironmentVariablesFilePathFromSources()).toURI())));
        } catch (Exception e) {
            fail("Error on testGetEnvironmentVariablesFilePathFromSources(): " + e.getMessage());
        }
    }

    @Test
    public void testGetEnvironmentVariablesFilePath() {
        assertEquals(getExpectedEnvironmentVariablesFilePath(), TEST_VERSION.getEnvironmentVariablesFilePath());
        assertEquals(getExpectedEnvironmentVariablesFilePath(), TEST_VERSION_JNI.getEnvironmentVariablesFilePath());
        assertTrue(Files.exists(TEST_VERSION.getEnvironmentVariablesFilePath()));
        assertTrue(Files.exists(TEST_VERSION_JNI.getEnvironmentVariablesFilePath()));
    }

    @Test
    public void testGetNativeLibrariesRootFolderPath() {
        assertEquals(getExpectedNativeLibrariesRootFolderPath(), TEST_VERSION.getNativeLibrariesRootFolderPath());
        assertEquals(getExpectedNativeLibrariesRootFolderPath(), TEST_VERSION_JNI.getNativeLibrariesRootFolderPath());
        assertTrue(Files.exists(TEST_VERSION.getNativeLibrariesRootFolderPath()));
        assertTrue(Files.exists(TEST_VERSION_JNI.getNativeLibrariesRootFolderPath()));
    }

    @Test
    public void testGetJNILibraryFilePath() {
        assertEquals(getExpectedJNILibraryFilePath(TEST_VERSION), TEST_VERSION.getJNILibraryFilePath());
        assertEquals(getExpectedJNILibraryFilePath(TEST_VERSION_JNI), TEST_VERSION_JNI.getJNILibraryFilePath());
    }
}
