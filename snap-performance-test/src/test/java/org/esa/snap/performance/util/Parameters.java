package org.esa.snap.performance.util;

public class Parameters {

    private final String testName;
    private final String productName;
    private final String testDirectory;
    private final Threading threading;
    private final boolean discardFirstMeasure;
    private final int numExecutionsForAverageOperations;

    public Parameters(String testName, String productName, String testDirectory, boolean discardFirstMeasure, Threading threading, int numExecutionsForAverageOperations) {
        this.testName = testName;
        this.productName = productName;
        this.testDirectory = testDirectory;
        this.threading = threading;
        this.discardFirstMeasure = discardFirstMeasure;
        this.numExecutionsForAverageOperations = numExecutionsForAverageOperations;
    }

    public String getTestName() {
        return this.testName;
    }

    public String getProductName() {
        return this.productName;
    }

    public String getTestDirectory() {
        return this.testDirectory;
    }

    public Threading getThreading() {
        return this.threading;
    }

    public boolean isDiscardingFirstMeasure() {
        return this.discardFirstMeasure;
    }

    public int getNumExecutionsForAverageOperations() {
        return this.numExecutionsForAverageOperations;
    }
}
