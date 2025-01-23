package org.esa.snap.tests;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.performance.util.ExcelWriter;
import org.esa.snap.performance.util.PerformanceTestResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class ExcelWriterTest {

    private ExcelWriter excelWriter;
    private Path tempDir;
    private List<PerformanceTestResult> results;

    @Before
    public void setUp() throws Exception {
        this.excelWriter = new ExcelWriter();
        this.tempDir = Files.createTempDirectory("excelTest");

        File resultsDir = new File(tempDir.toFile(), "Results");
        if (!resultsDir.exists() && !resultsDir.mkdirs()) {
            throw new IOException("Failed to create results directory for test setup");
        }

        PerformanceTestResult mockResult1 = new PerformanceTestResult(
                "Test1", "Product1", "Product2", "format1", "format2",
                List.of("Time"), List.of(100.0), List.of(120.0), List.of("ms")
        );

        PerformanceTestResult mockResult2 = new PerformanceTestResult(
                "Test2", "Product3", "Product4", "format1", "format2",
                List.of("Memory"), List.of(50.0), List.of(45.0), List.of("MB")
        );

        this.results = List.of(mockResult1, mockResult2);
    }

    @After
    public void tearDown() throws Exception {
        deleteDirectory(tempDir.toFile());
    }

    @Test
    @STTM("SNAP-3712")
    public void testWriteResultsCreatesExcelFile() throws Exception {

        excelWriter.writeResults(tempDir.toString(), results);

        File resultsDir = new File(tempDir.toFile(), "Results");
        File[] files = resultsDir.listFiles((dir, name) -> name.startsWith("performance-results_") && name.endsWith(".xlsx"));

        assertNotNull(files);
        assertEquals(1, files.length);

        File writtenFile = files[0];
        assertTrue(writtenFile.exists());
    }

    private void deleteDirectory(File directory) throws Exception {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                deleteDirectory(file);
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete file or directory: " + directory.getAbsolutePath());
        }
    }
}
