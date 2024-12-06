package org.esa.snap.performance.util;

import java.util.ArrayList;
import java.util.List;

public class TestResult {

    private final String testName;
    private final String productName;
    private final List<IndividualTestResult> results;

    public TestResult(String testName, String productName) {
        this.testName = testName;
        this.productName = productName;
        this.results = new ArrayList<>();
    }

    public String getTestName() {
        return testName;
    }

    public String getProductName() {
        return productName;
    }

    public List<IndividualTestResult> getResults() {
        return results;
    }

    public void addIndividualResult(IndividualTestResult individualTestResult) {
        this.results.add(individualTestResult);
    }
}
