package org.esa.snap.performance.performancetests;

import org.esa.snap.performance.util.*;

import java.io.IOException;

public abstract class AbstractPerformanceTest {

    private final String testName;
    private final Parameters params;
    private TestResult testResult;

    public AbstractPerformanceTest(String testName, Parameters params) {
        this.testName = testName;
        this.params = params;
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

    public String getOutputDir() {
        return this.params.getOutputDir();
    }

    public boolean isDiscardingFirstMeasure() {
        return this.params.isDiscardingFirstMeasure();
    }

    public Threading getThreading() {
        return this.params.getThreading();
    }

    public int getNumExecutionsForAverageOperations() {
        return this.params.getNumExecutionsForAverageOperations();
    }

    public TestResult getTestResult() {
        return this.testResult;
    }

    public void setTestResult(TestResult result) {
        this.testResult = result;
    }

    public void execute() throws IOException {
        System.out.println("Executing " + getTestName() + " test for product: " + getProductName());

        CalculationContainer calculationsDiMap = executeTest(TestUtils.FORMAT_DIMAP);
        CalculationContainer calculationsZNAP = executeTest(TestUtils.FORMAT_ZNAP);

        long[] timesDiMap = calculationsDiMap.getTimes();
        long[] timesZNAP = calculationsZNAP.getTimes();

        timesDiMap = TestUtils.reduceResultArray(isDiscardingFirstMeasure(), timesDiMap);
        timesZNAP = TestUtils.reduceResultArray(isDiscardingFirstMeasure(), timesZNAP);

        double timeAverageResultDiMap = TestUtils.calculateArithmeticMean(timesDiMap);
        double timeAverageResultZNAP = TestUtils.calculateArithmeticMean(timesZNAP);

        double throughputResultDiMap = TestUtils.calculateThroughput(timesDiMap, calculationsDiMap.getFiles());
        double throughputResultZNAP = TestUtils.calculateThroughput(timesZNAP, calculationsZNAP.getFiles());

        IndividualTestResult averageTimeToReadResult = new IndividualTestResult("Average-Time-to-Read", getProductName(), Unit.MS, timeAverageResultDiMap, timeAverageResultZNAP);
        IndividualTestResult maxMemoryTestResult = new IndividualTestResult("Max. Memory Consumption", getProductName(), Unit.MB, calculationsDiMap.getMaxMemoryConsumptionInMB(), calculationsZNAP.getMaxMemoryConsumptionInMB());
        IndividualTestResult throughputTestResult = new IndividualTestResult("Throughput", getProductName(), Unit.MB_PER_S, throughputResultDiMap, throughputResultZNAP);

        TestResult allResults = new TestResult(getTestName(), getProductName());
        allResults.addIndividualResult(averageTimeToReadResult);
        allResults.addIndividualResult(maxMemoryTestResult);
        allResults.addIndividualResult(throughputTestResult);

        setTestResult(allResults);

        System.out.println("Test completed");
    }

    public abstract CalculationContainer executeTest(String format) throws IOException;
}
