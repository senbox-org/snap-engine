package org.esa.snap.tests;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.performance.util.OutputDirectoryInitializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class OutputDirectoryInitializerTest {

    private static final String TEST_OUTPUT_DIR = "test-output-dir";
    private static final String RESULTS_DIR = "Results";

    private File testOutputDir;
    private File resultsDir;

    @Before
    public void setUp() {
        testOutputDir = new File(TEST_OUTPUT_DIR);
        resultsDir = new File(testOutputDir, RESULTS_DIR);
    }

    @After
    public void tearDown() throws IOException {
        if (testOutputDir.exists()) {
            OutputDirectoryInitializer.deleteDirectory(testOutputDir);
        }
    }

    @Test
    @STTM("SNAP-3712")
    public void testInitializeCreatesOutputDirAndResultsDir() throws IOException {
        OutputDirectoryInitializer.initialize(TEST_OUTPUT_DIR);

        assertTrue("Output directory should exist", testOutputDir.exists());
        assertTrue("Results directory should exist", resultsDir.exists());
    }

    @Test
    @STTM("SNAP-3712")
    public void testInitializeCleansOutputDirExceptResultsDir() throws IOException {
        testOutputDir.mkdirs();
        resultsDir.mkdirs();

        File dummyFile = new File(testOutputDir, "dummy.txt");
        File dummyDir = new File(testOutputDir, "dummy-dir");
        dummyFile.createNewFile();
        dummyDir.mkdirs();

        OutputDirectoryInitializer.initialize(TEST_OUTPUT_DIR);

        assertTrue("Results directory should still exist", resultsDir.exists());
        assertFalse("Dummy file should be deleted", dummyFile.exists());
        assertFalse("Dummy directory should be deleted", dummyDir.exists());
    }

    @Test
    @STTM("SNAP-3712")
    public void testInitializeThrowsExceptionForNullOutputDir() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> OutputDirectoryInitializer.initialize(null));
        assertEquals("Output directory path is not defined in configuration ('outputDir'). Please check your configuration file.",
                exception.getMessage());
    }

}