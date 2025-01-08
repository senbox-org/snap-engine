package org.esa.snap.performance.util;

import org.esa.snap.performance.testImplementation.PerformanceTest;

import java.util.ArrayList;
import java.util.List;

public class PerformanceTestRunner {

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

        System.out.println("Run Performance tests...");
        for (PerformanceTest test : tests) {
            try {
                test.execute();
                PerformanceTestResult result = test.fetchResults();
                results.add(result);

                // TODO: implement switch as global config parameter to select if output should be deleted after every iteration
                TestUtils.deleteTestOutputs(this.outputDirectory);
            } catch (Throwable e) {
                System.out.println("Test execution failed:");
                System.out.println("Test will be skipped: " + test.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
        this.results = results;
        System.out.println("Performance tests completed successfully!");
    }

    public List<PerformanceTestResult> collectResults() {
        return this.results;
    }

}
