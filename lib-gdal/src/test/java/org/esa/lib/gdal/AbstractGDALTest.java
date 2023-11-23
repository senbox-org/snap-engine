package org.esa.lib.gdal;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.gdal.OSCategory;
import org.junit.Before;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.commons.lang3.SystemUtils.*;
import static org.esa.snap.dataio.gdal.GDALVersion.GDAL_NATIVE_LIBRARIES_ROOT;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class AbstractGDALTest {

    public static final String PROPERTY_NAME_DATA_DIR = "snap.gdal.tests.data.dir";
    private static final String TEST_FOLDER_NAME = "_lib_gdal";

    protected Path libGDALTestsFolderPath;

    public static String getExpectedDirectory() {
        final OSCategory osCategory = getExpectedOSCategory();
        return osCategory.getOperatingSystemName() + "/" + osCategory.getArchitecture();
    }

    public static OSCategory getExpectedOSCategory() {
        final String sysArch = System.getProperty("os.arch").toLowerCase();
        if (IS_OS_LINUX && sysArch.contains("amd64")) {
            return OSCategory.LINUX_64;
        } else if (IS_OS_MAC_OSX) {
            if (sysArch.contains("amd64") || sysArch.contains("x86_64")) {
                return OSCategory.MAC_OS_X;
            } else if (sysArch.contains("aarch64")) {
                return OSCategory.MAC_OS_X_AARCH64;
            }
        } else if (IS_OS_WINDOWS) {
            if (sysArch.contains("amd64") || sysArch.contains("x86_x64")) {
                return OSCategory.WIN_64;
            } else {
                return OSCategory.WIN_32;
            }
        }
        return OSCategory.UNSUPPORTED;
    }

    public static Path getExpectedNativeLibrariesRootFolderPath() {
        return SystemUtils.getAuxDataPath().resolve(GDAL_NATIVE_LIBRARIES_ROOT);
    }

    private static boolean testDataAvailable() {
        final String testDataDir = System.getProperty(PROPERTY_NAME_DATA_DIR);
        return (testDataDir != null) && !testDataDir.isEmpty() && Files.exists(Paths.get(testDataDir));
    }

    private void checkTestDirectoryExists() {
        final String testDirectoryPathProperty = System.getProperty(PROPERTY_NAME_DATA_DIR);
        assertNotNull("The system property '" + PROPERTY_NAME_DATA_DIR + "' representing the test directory is not set.", testDirectoryPathProperty);
        final Path testFolderPath = Paths.get(testDirectoryPathProperty);
        assertTrue("The test directory path " + testDirectoryPathProperty + " is not valid.", Files.exists(testFolderPath));
        this.libGDALTestsFolderPath = testFolderPath.resolve(TEST_FOLDER_NAME);
        assumeTrue("The lib_gdal test directory path (" + this.libGDALTestsFolderPath + ") is not valid.", Files.exists(this.libGDALTestsFolderPath));
    }

    @Before
    public final void setUp() {
        assumeTrue(testDataAvailable());
        checkTestDirectoryExists();
    }
}
