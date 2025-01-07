package org.esa.snap.performance.util;

import org.esa.snap.performance.testImplementation.*;

import java.util.ArrayList;
import java.util.List;

public class PerformanceTestFactory {

    public static List<PerformanceTest> createPerformanceTests(List<PerformanceTestDefinition> testDefinitions) {
        System.out.println("Initializing tests...");

        List<PerformanceTest> performanceTestList = new ArrayList<>();

        for (PerformanceTestDefinition testDef : testDefinitions) {
            String implementation = testDef.getTestImplementation();
            String testName = testDef.getTestName();
            Parameters params = testDef.getParameters();

            if (implementation.equals("write-from-memory-test")) {
                performanceTestList.add(new WriteFromMemoryPerformanceTest(testName, params));
            } else if (implementation.equals("write-from-reader-test")) {
                performanceTestList.add(new WriteFromReaderPerformanceTest(testName, params));
            } else if (implementation.equals("read-test")) {
                performanceTestList.add(new ReadPerformanceTest(testName, params));
            }
        }

        return performanceTestList;
    }
}
