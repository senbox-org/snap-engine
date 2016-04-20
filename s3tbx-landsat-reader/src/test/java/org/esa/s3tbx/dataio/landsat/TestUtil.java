package org.esa.s3tbx.dataio.landsat;

import java.io.File;

import static org.junit.Assert.*;

public class TestUtil {

    private static final String[] DIRS_TO_CHECK = new String[]{".", "s3tbx", "s3tbx-landsat-reader", "s3tbx/s3tbx-landsat-reader"};
    private static final String REL_PATH_TEST_RESOURCES = "src/test/resources/org/esa/s3tbx/dataio/landsat/";

    public static File getTestFile(String file) {
        final File testTgz = getTestFileOrDirectory(file);
        assertTrue(testTgz.isFile());
        return testTgz;
    }

    public static File getTestDirectory(String file) {
        final File testTgz = getTestFileOrDirectory(file);
        assertTrue(testTgz.isDirectory());
        return testTgz;
    }

    private static File getTestFileOrDirectory(String file) {
        String relFilePath = REL_PATH_TEST_RESOURCES + file;
        for (String dirPath : DIRS_TO_CHECK) {
            File currentDir = new File(dirPath);
            File currentFile = new File(currentDir, relFilePath);
            if(currentFile.exists()) {
                return currentFile;
            }

        }
        return null;
    }
}
