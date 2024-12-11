package org.esa.snap.oldImpl.performance;

import org.esa.snap.oldImpl.performance.performancetests.AbstractPerformanceTest;
import org.esa.snap.oldImpl.performance.util.*;

import java.io.IOException;
import java.util.List;

public class PerformanceTestMain {

    public static void main(String[] args) {

        ConfigLoader config;
        List<AbstractPerformanceTest> tests;

        try {
            config = new ConfigLoader();
            tests = TestInitialisationOperations.initialize(config);
        } catch (IOException e) {
            throw new RuntimeException("The Test initialisation failed.", e);
        }

        TestExecuter.executeTests(tests);
        List<TestResult> results = TestExecuter.fetchAllTestResults(tests);

        MyExcelWriter.writeExcelFile(config.get("outputDir"), results);
    }
}