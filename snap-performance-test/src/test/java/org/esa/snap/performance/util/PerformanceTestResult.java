package org.esa.snap.performance.util;

import java.util.List;

public class PerformanceTestResult {

    private final String testName;
    private final String product1;
    private final String product2;
    private final String format1;
    private final String format2;
    private final List<String> descriptions;
    private final List<Double> result1;
    private final List<Double> result2;
    private final List<String> units;

    public PerformanceTestResult(String testName, String product1, String product2, String format1, String format2, List<String> descriptions, List<Double> result1, List<Double> result2, List<String> units) {
        this.testName = testName;
        this.product1 = product1;
        this.product2 = product2;
        this.format1 = format1;
        this.format2 = format2;
        this.descriptions = descriptions;
        this.result1 = result1;
        this.result2 = result2;
        this.units = units;
    }

    public String getTestName() {
        return testName;
    }

    public String getProduct1() {
        return product1;
    }

    public String getProduct2() {
        return product2;
    }

    public String getFormat1() {
        return format1;
    }

    public String getFormat2() {
        return format2;
    }

    public List<String> getDescriptions() {
        return descriptions;
    }

    public List<Double> getResult1() {
        return result1;
    }

    public List<Double> getResult2() {
        return result2;
    }

    public List<String> getUnits() {
        return units;
    }
}
