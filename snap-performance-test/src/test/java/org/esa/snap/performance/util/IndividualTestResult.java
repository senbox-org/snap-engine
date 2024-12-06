package org.esa.snap.performance.util;

public class IndividualTestResult {

    private final String testName;
    private final String productName;
    private final Unit unit;
    private final double resultDiMap;
    private final double resultZNAP;

    public IndividualTestResult(String testName, String productName, Unit unit, double resultDiMap, double resultZNAP) {
        this.testName = testName;
        this.productName = productName;
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
