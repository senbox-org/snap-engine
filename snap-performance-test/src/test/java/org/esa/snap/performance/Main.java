package org.esa.snap.performance;

import org.esa.snap.performance.testImplementation.PerformanceTest;
import org.esa.snap.performance.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        String[] configs = getConfigs(args);

        for (String config : configs) {

            ConfigParser configParser = new ConfigParser(config);
            List<PerformanceTestDefinition> testDefinitions = new ArrayList<>();

            try {
                // step 1: parse configuration and load test definitions
                configParser.parse(testDefinitions);
                // step 2: initialize outputDirectory
                OutputDirectoryInitializer.initialize(configParser.getOutputDirectory());

            } catch (Exception e) {
                logger.log(Level.SEVERE, "IO Error during initialization: " + e.getMessage(), e);
                continue;
            }

            // step 3: create performance tests from definitions
            List<PerformanceTest> tests = PerformanceTestFactory.createPerformanceTests(testDefinitions);

            // step 4: execute all tests and collect results
            PerformanceTestRunner testRunner = new PerformanceTestRunner(tests, configParser.getOutputDirectory());
            testRunner.runTests();
            List<PerformanceTestResult> allResults = testRunner.collectResults();

            // step 5: write results to an Excel file
            ExcelWriter excelWriter = new ExcelWriter();
            excelWriter.writeResults(configParser.getOutputDirectory(), allResults);
        }
        logger.log(Level.INFO, "All tests finished.");
    }

    private static String[] getConfigs(String[] args) {
        return args.length == 0
                ? new String[] {"config.properties"}
                : args;
    }
}
