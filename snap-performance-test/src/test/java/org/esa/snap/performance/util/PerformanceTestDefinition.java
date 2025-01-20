package org.esa.snap.performance.util;

public class PerformanceTestDefinition {

    private final String testName;
    private final String testImplementation;
    private final Parameters parameters;

    public PerformanceTestDefinition(String testName, String testImplementation, Parameters parameters) {
        this.testName = testName;
        this.testImplementation = testImplementation;
        this.parameters = parameters;
    }

    public String getTestName() {
        return testName;
    }

    public String getTestImplementation() {
        return testImplementation;
    }

    public Parameters getParameters() {
        return parameters;
    }
}
