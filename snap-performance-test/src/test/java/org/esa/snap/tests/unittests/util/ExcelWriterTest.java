package org.esa.snap.tests.unittests.util;

import org.esa.snap.performance.util.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ExcelWriterTest {

    @Test
    public void testWriteExcelFile() throws IOException {
        Path tempDir = Files.createTempDirectory("excel_writer_test");
        String outputDir = tempDir.toAbsolutePath().toString();

        Path resultsDir = tempDir.resolve(TestUtils.RESULTS_DIR);
        Files.createDirectory(resultsDir);

        TestResult result = new TestResult(
                "xy",
                "My Product",
                Threading.MULTI,
                Unit.MB_S,
                24.3,
                12.7
        );

        List<TestResult> results = new ArrayList<>();
        results.add(result);

        ExcelWriter.writeExcelFile(outputDir, results);

        assertTrue("Results directory should exist", Files.exists(resultsDir));
        assertTrue("Results directory should be a directory", Files.isDirectory(resultsDir));

        long fileCount = Files.list(resultsDir).count();
        assertEquals("Exactly One file should have been created in Results directory", 1, fileCount);

        deleteDirectory(tempDir);
    }


    private void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
                .sorted((a,b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete directory: " + path, e);
                    }
                });
    }
}