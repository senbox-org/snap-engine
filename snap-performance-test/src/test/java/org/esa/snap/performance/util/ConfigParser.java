package org.esa.snap.performance.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class ConfigParser {

    private final String propertiesFileName;
    private String outputDirectory;
    private boolean deleteOutput = false;

    public ConfigParser(String propertiesFileName) {
            this.propertiesFileName = propertiesFileName;
    }

    public void parse(List<PerformanceTestDefinition> testDefinitions) throws IOException, IllegalArgumentException {

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(this.propertiesFileName)) {
            if (input == null) {
                throw new IOException("Configuration file '" + this.propertiesFileName + "' not found in classpath.");
            }
            Properties properties = new Properties();
            properties.load(input);

            String outputDir = properties.getProperty("outputDir");
            if (outputDir == null) {
                throw new IllegalArgumentException("Missing property: 'outputDir'. Please define at a output Directory in the configuration.");
            } else {
                this.outputDirectory = outputDir;
            }

            String deleteOutput = properties.getProperty("deleteOutput");
            if (deleteOutput == null) {
                this.deleteOutput = false;
            } else {
                this.deleteOutput = Boolean.parseBoolean(deleteOutput);
            }

            String testNamesStr = properties.getProperty("testNames");
            String[] testNames;
            if (testNamesStr == null) {
                throw new IllegalArgumentException("Missing property: 'testName'. Please define at least one test in the configuration.");
            } else {
                testNames = testNamesStr.split(",");
            }

            for (String name : testNames) {
                String testName = name.trim();
                String testImplementation = properties.getProperty(testName + ".testImplementation");

                if (testImplementation == null) {
                    throw new IllegalArgumentException("Missing property: '" + testName + ".testImplementation'. Please define it in the configuration.");
                }
                Parameters params = Parameters.parseParameters(testName, outputDir, properties);

                PerformanceTestDefinition testDefinition = new PerformanceTestDefinition(testName, testImplementation, params);
                testDefinitions.add(testDefinition);
            }
        }
    }

    public String getOutputDirectory() {
        return this.outputDirectory;
    }

    public boolean isDeleteOutput() {
        return this.deleteOutput;
    }
}
