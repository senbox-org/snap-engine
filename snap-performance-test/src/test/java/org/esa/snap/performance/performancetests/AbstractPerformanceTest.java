package org.esa.snap.performance.performancetests;

import org.esa.snap.performance.util.Parameters;
import org.esa.snap.performance.util.TestResult;
import org.esa.snap.performance.util.Threading;
import org.esa.snap.performance.util.Unit;

import java.io.IOException;

public abstract class AbstractPerformanceTest {

    private final String testName;
    private final Parameters params;
    private final Unit unit;
    private TestResult result;

    public AbstractPerformanceTest(String testName, Parameters params, Unit unit) {
        this.testName = testName;
        this.params = params;
        this.unit = unit;
    }

    public String getTestName() {
        return this.testName;
    }

    public String getProductName() {
        return this.params.getProductName();
    }

    public String getTestDataDir() {
        return this.params.getTestDirectory();
    }

    public boolean isDiscardingFirstMeasure() {
        return this.params.isDiscardingFirstMeasure();
    }

    public Threading getThreading() {
        return this.params.getThreading();
    }

    public Unit getUnit() {
        return unit;
    }

    public int getNumExecutionsForAverageOperations() {
        return this.params.getNumExecutionsForAverageOperations();
    }

    public TestResult getResult() {
        return this.result;
    }

    public void setResult(TestResult result) {
        this.result = result;
    }

    public abstract void execute() throws IOException;
}
