package org.esa.snap.performance.util;

import org.esa.snap.performance.performancetests.AbstractPerformanceTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestExecuter {

    public static void executeTests(List<AbstractPerformanceTest> tests) {
        tests.forEach(test -> {
            try {
                test.execute();
            } catch (IOException e) {
                throw new RuntimeException("Failed to execute test: " + test.getTestName(), e);
            }
        });
    }

    public static List<TestResult> fetchAllTestResults(List<AbstractPerformanceTest> tests) {
        List<TestResult> results = new ArrayList<>();
        for (AbstractPerformanceTest test : tests) {
            results.add(test.getResult());
        }
        return  results;
    }
}
