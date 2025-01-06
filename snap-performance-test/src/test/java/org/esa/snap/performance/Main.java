package org.esa.snap.performance;

import org.esa.snap.performance.testImplementation.PerformanceTest;
import org.esa.snap.performance.util.*;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            System.out.println("Initializing tests");

            // step 1: parse configuration and load test definitions
            ConfigParser configParser = new ConfigParser("config.properties");
            List<PerformanceTestDefinition> testDefinitions = configParser.parse();

            // step 2: initialize outputDirectory
            String outputDirectory = configParser.getOutputDirectory();
            OutputDirectoryInitializer.initialize(outputDirectory);

            // step 3: create performance tests from definitions
            // TODO adjust unit test when implementation details are more clear
            List<PerformanceTest> tests = PerformanceTestFactory.createPerformanceTests(testDefinitions);


            // step 4: execute all tests and collect results
            PerformanceTestRunner testRunner = new PerformanceTestRunner();
            testRunner.runTests(tests);
            List<PerformanceTestResult> allResults = testRunner.collectResults();

//            // TEMPORARY
//            testRunner.printAllResults();

            // step 5: write results to an Excel file
            ExcelWriter excelWriter = new ExcelWriter();
            excelWriter.writeResults(configParser.getOutputDirectory(), allResults);

        } catch (IOException e) {
                System.err.println("Error during execution: " + e.getMessage());
        }
    }
}
