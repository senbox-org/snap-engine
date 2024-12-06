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

        IndividualTestResult individualResult1 = new IndividualTestResult(
                "xy",
                "My Product",
                Unit.MB_PER_S,
                24.3,
                12.7
        );

        IndividualTestResult individualResult2 = new IndividualTestResult(
                "yz",
                "My Product",
                Unit.MS,
                12.3,
                10.7
        );

        TestResult singleTestResults = new TestResult("ABC", "My Product");
        singleTestResults.addIndividualResult(individualResult1);
        singleTestResults.addIndividualResult(individualResult2);


        List<TestResult> allResults = new ArrayList<>();
        allResults.add(singleTestResults);

        ExcelWriter.writeExcelFile(outputDir, allResults);

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