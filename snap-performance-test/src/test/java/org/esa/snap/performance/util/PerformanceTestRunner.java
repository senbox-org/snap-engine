package org.esa.snap.performance.util;

import org.esa.snap.performance.testImplementation.PerformanceTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PerformanceTestRunner {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    List<PerformanceTest> tests;
    private List<PerformanceTestResult> results;
    private String outputDirectory;

    public PerformanceTestRunner(List<PerformanceTest> tests, String outputDirectory) {
        this.tests = tests;
        this.results = new ArrayList<>();
        this.outputDirectory = outputDirectory;
    }

    public void runTests() {
        List<PerformanceTestResult> results = new ArrayList<>();

        logger.info("Starting performance tests...");
        for (PerformanceTest test : tests) {
            try {
                test.execute();
                PerformanceTestResult result = test.fetchResults();
                results.add(result);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Test execution failed:");
                logger.log(Level.SEVERE, "Test will be skipped: " + test.getClass().getSimpleName() + " - " + e.getMessage(), e);
            }

            // TODO: implement switch as global config parameter to select if output should be deleted after every iteration
            try {
                TestUtils.deleteTestOutputs(this.outputDirectory);
            } catch (IOException e) {
                logger.log(Level.INFO, "Performance test output files could not be deleted: " + e.getMessage(), e);
            }

            System.gc();
        }

        this.results = results;
        logger.info("Performance tests completed successfully!");
    }

    public List<PerformanceTestResult> collectResults() {
        return this.results;
    }

}
