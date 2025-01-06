package org.esa.snap.performance.util;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Parameters {

    private final List<String> products;
    private final String testDir;
    private final String outputDir;
    private final String threading;
    private final boolean useZip;
    private final boolean discardFirstMeasure;
    private final int numExecutionsForAverageOperations;
    private final List<String> outputFormats;
    private final boolean useTimeAverage;
    private final boolean useMaxMemoryConsumption;
    private final boolean useThroughput;

    public Parameters(List<String> products, String testDir, String outputDir, String threading, boolean useZip, boolean discardFirstMeasure, int numExecutionsForAverageOperations, List<String> outputFormats, boolean useTimeAverage, boolean useMaxMemoryConsumption, boolean useThroughput) {
        this.products = products;
        this.testDir = testDir;
        this.outputDir = outputDir;
        this.threading = threading;
        this.useZip = useZip;
        this.discardFirstMeasure = discardFirstMeasure;
        this.numExecutionsForAverageOperations = numExecutionsForAverageOperations;
        this.outputFormats = outputFormats;
        this.useTimeAverage = useTimeAverage;
        this.useMaxMemoryConsumption = useMaxMemoryConsumption;
        this.useThroughput = useThroughput;
    }

    public static Parameters parseParameters(String testName, String outputDir, Properties props) {
        String products = props.getProperty(testName + ".products");
        List<String> productsList;
        if (products == null) {
            throw new IllegalArgumentException("Missing property: '" + testName + ".products'. Please define at least one input product in the configuration.");
        } else {
            productsList = Arrays.asList(products.split(","));
        }

        String testDataDir = props.getProperty("testDataDir");
        if (testDataDir == null) {
            throw new IllegalArgumentException("Missing property: 'testDataDir'. Please define the location of your test data in the configuration.");
        }

        String threading = props.getProperty(testName + ".threading");
        if (threading == null) {
            threading = Threading.SINGLE.getName();
        }

        String useZipStr = props.getProperty(testName + ".useZip");
        boolean useZip;
        if (useZipStr == null) {
            useZip = true;
        } else {
            useZip = Boolean.parseBoolean(useZipStr);
        }

        String discardFirstMeasureStr = props.getProperty(testName + ".discardFirstMeasure");
        boolean discardFirstMeasure;
        if (discardFirstMeasureStr == null) {
            discardFirstMeasure = true;
        } else {
            discardFirstMeasure = Boolean.parseBoolean(discardFirstMeasureStr);
        }

        String numExecutionsForAverageOperationsStr = props.getProperty(testName + ".numExecutionsForAverageOperations");
        if (numExecutionsForAverageOperationsStr == null) {
            throw new IllegalArgumentException("Missing property: '" + testName + ".numExecutionsForAverageOperations'. Please define it in the configuration.");
        }
        int numExecutionsForAverageOperations;
        try {
            numExecutionsForAverageOperations = Integer.parseInt(numExecutionsForAverageOperationsStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format for property: '" + testName + ".numExecutionsForAverageOperations'. Expected a positive integer.", e);
        }

        String outputFormats = props.getProperty(testName + ".outputFormats");
        List<String> outputFormatsList;
        if (outputFormats == null) {
            outputFormatsList = null;
        } else {
            outputFormatsList = Arrays.asList(outputFormats.split(","));
        }

        String[] actions = props.getProperty(testName + ".actions").split(",");
        boolean isTimeAverage = parseAction(actions, "timeAverage");
        boolean isMaxMemoryConsumption = parseAction(actions, "maxMemoryConsumption");
        boolean isThroughput = parseAction(actions, "throughput");

        return new Parameters(
                productsList,
                testDataDir,
                outputDir,
                threading,
                useZip,
                discardFirstMeasure,
                numExecutionsForAverageOperations,
                outputFormatsList,
                isTimeAverage,
                isMaxMemoryConsumption,
                isThroughput
                );
    }

    public static boolean parseAction(String[] actions, String targetAction) {
        for (String action : actions) {
            if (action.equals(targetAction)) {
                return true;
            }
        }
        return false;
    }


    public List<String> getProducts() {
        return this.products;
    }

    public String getTestDir() {
        return testDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public String getThreading() {
        return this.threading;
    }

    public boolean isUseZip() {
        return useZip;
    }

    public boolean isDiscardFirstMeasure() {
        return this.discardFirstMeasure;
    }

    public int getNumExecutionsForAverageOperations() {
        return this.numExecutionsForAverageOperations;
    }

    public List<String> getOutputFormats() {
        return this.outputFormats;
    }

    public boolean isUseTimeAverage() {
        return this.useTimeAverage;
    }

    public boolean isUseMaxMemoryConsumption() {
        return this.useMaxMemoryConsumption;
    }

    public boolean isUseThroughput() {
        return this.useThroughput;
    }
}
