package org.esa.beam.dataio.landsat;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class TestUtil {

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
        File testArchiveFile = new File("./beam-landsat-reader/src/test/resources/org/esa/beam/dataio/landsat/" + file);
        if (!testArchiveFile.exists()) {
            testArchiveFile = new File("./src/test/resources/org/esa/beam/dataio/landsat/" + file);
        }
        return testArchiveFile;
    }
}
