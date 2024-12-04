package org.esa.snap.performance.util;

import org.esa.snap.performance.performancetests.AbstractPerformanceTest;
import org.esa.snap.performance.performancetests.PerformanceTestFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TestInitialisationOperations {

    public static List<AbstractPerformanceTest> initialize(ConfigLoader config) throws IOException {
        initializeOutputDirectory(config);
        List<Parameters> parametersList = initializeParams(config);

        return initializeTests(parametersList);
    }

    private static List<AbstractPerformanceTest> initializeTests(List<Parameters> parameterList) {
        List<AbstractPerformanceTest> tests = new ArrayList<>();
        for (Parameters params : parameterList) {
            AbstractPerformanceTest test = PerformanceTestFactory.createPerformanceTest(params);
            tests.add(test);
        }
        return tests;
    }

    private static List<Parameters> initializeParams(ConfigLoader config) {
        List<Parameters> parameterList = new ArrayList<>();
        String[] testNames = config.get("testNames").split(",");

        for (String name : testNames) {
            String testName = name.trim();

            Parameters params = new Parameters(
                    config.get(testName + ".testImplementation"),
                    config.get(testName + ".productName"),
                    config.get("testDataDir"),
                    Boolean.parseBoolean(config.get("discardFirstMeasure")),
                    Threading.matchStringToEnum(config.get(testName + ".threading")),
                    Integer.parseInt(config.get("numExecutionsForAverageOperations"))
            );
            parameterList.add(params);
        }
        return parameterList;
    }

    private static void initializeOutputDirectory(ConfigLoader config) throws IOException {
        String outputDirPath = config.get("outputDir");
        if (outputDirPath == null) {
            throw new IllegalArgumentException("Output directory path is not defined in configuration ('outputDir'). Please check your configuration file.");
        }

        File outputDir = new File(outputDirPath);
        File resultsDir = new File(outputDir, TestUtils.RESULTS_DIR);

        if (outputDir.exists()) {
            for (File file : outputDir.listFiles()) {
                if (file.isDirectory() && file.getName().equals(TestUtils.RESULTS_DIR)) {
                    continue;
                }
                if (file.isDirectory()) {
                    deleteDirectory(file.toPath());
                } else {
                    file.delete();
                }
            }
        } else {
            if (!outputDir.mkdirs()) {
                throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
            }
        }

        if (!resultsDir.exists() && !resultsDir.mkdirs()) {
            throw new IOException("Failed to create results directory: " + resultsDir.getAbsolutePath());
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
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
