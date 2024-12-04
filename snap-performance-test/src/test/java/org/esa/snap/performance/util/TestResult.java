package org.esa.snap.performance.util;

public class TestResult {

    private final String testName;
    private final String productName;
    private final Threading threading;
    private final Unit unit;
    private final double resultDiMap;
    private final double resultZNAP;

    public TestResult(String testName, String productName, Threading threading, Unit unit, double resultDiMap, double resultZNAP) {
        this.testName = testName;
        this.productName = productName;
        this.threading = threading;
        this.unit = unit;
        this.resultDiMap = resultDiMap;
        this.resultZNAP = resultZNAP;
    }

    public String getTestName() {
        return testName;
    }

    public String getProductName() {
        return productName;
    }

    public Threading getThreading() {
        return threading;
    }

    public Unit getUnit() {
        return unit;
    }

    public double getResultDiMap() {
        return resultDiMap;
    }

    public double getResultZNAP() {
        return resultZNAP;
    }
}
