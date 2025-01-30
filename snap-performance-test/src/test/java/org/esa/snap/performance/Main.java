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
                configParser.parse(testDefinitions);
                OutputDirectoryInitializer.initialize(configParser.getOutputDirectory());

            } catch (Exception e) {
                logger.log(Level.SEVERE, "IO Error during initialization: " + e.getMessage(), e);
                continue;
            }

            List<PerformanceTest> tests = PerformanceTestFactory.createPerformanceTests(testDefinitions);
            String outputDir = configParser.getOutputDirectory();
            boolean deleteOutput = configParser.isDeleteOutput();

            PerformanceTestRunner testRunner = new PerformanceTestRunner(tests, outputDir, deleteOutput);
            testRunner.runTests();
            List<PerformanceTestResult> allResults = testRunner.collectResults();

            ExcelWriter excelWriter = new ExcelWriter();
            excelWriter.writeResults(outputDir, allResults);
        }
        logger.log(Level.INFO, "All tests finished.");
    }

    private static String[] getConfigs(String[] args) {
        List<String> configs = new ArrayList<>();
        for (String arg : args) {
            if (arg.endsWith(".properties")) {
                configs.add(arg);
            }
        }

        if (configs.isEmpty()) {
            return new String[] {"config.properties"};
        } else {
            return configs.toArray(new String[0]);
        }
    }
}
