package org.esa.snap.performance.util;

import org.esa.snap.performance.testImplementation.PerformanceTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PerformanceTestRunner {

    private List<PerformanceTestResult> results;

    public void runTests(List<PerformanceTest> tests) {
        List<PerformanceTestResult> results = new ArrayList<>();

        System.out.println("Run Performance tests...");
        for (PerformanceTest test : tests) {
            try {
                test.execute();
                PerformanceTestResult result = test.fetchResults();
                results.add(result);
            } catch (IOException e) {
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

//    public void printAllResults() {
//        for (PerformanceTestResult result : this.results) {
//            System.out.println(result.getTestName());
//            System.out.println("description: " + result.getDescriptions());
//            System.out.println("product1 " + result.getProduct1());
//            System.out.println("product2 " + result.getProduct2());
//            System.out.println("results1 " + result.getResult1());
//            System.out.println("results2 " + result.getResult2());
//            System.out.println("units " + result.getUnits());
//        }
//    }
}
