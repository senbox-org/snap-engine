package org.esa.lib.gdal;

import org.junit.Before;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class AbstractGDALTest {

    public static final String PROPERTY_NAME_DATA_DIR = "snap.gdal.tests.data.dir";
    private static final String TEST_FOLDER_NAME = "_lib_gdal";

    protected Path libGDALTestsFolderPath;

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
