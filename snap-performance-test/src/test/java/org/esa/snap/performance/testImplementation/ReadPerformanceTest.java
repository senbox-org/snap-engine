package org.esa.snap.performance.testImplementation;

import org.esa.snap.performance.actions.Action;
import org.esa.snap.performance.actions.MultipleExecutionsAction;
import org.esa.snap.performance.actions.ReadProductFullyAction;
import org.esa.snap.performance.util.Parameters;
import org.esa.snap.performance.util.TestUtils;

public class ReadPerformanceTest extends PerformanceTest {

    public ReadPerformanceTest(String testName, Parameters params) {
        super(testName, params);
    }

    @Override
    public void execute() throws Throwable {
        System.out.println("Execution of " + getTestName() + " started....");

        String productName1 = getParameters().getProducts().get(0);
        String productName2 = getParameters().getProducts().get(1);
        String testDir = getParameters().getTestDir();

        Action baseAction1 = new ReadProductFullyAction(productName1, testDir);
        Action baseAction2 = new ReadProductFullyAction(productName2, testDir);

        Action measurementActions1 = TestUtils.constructMeasurementActionsPipeline(baseAction1, getParameters());
        Action measurementActions2 = TestUtils.constructMeasurementActionsPipeline(baseAction2, getParameters());

        Action multipleExecutions1 = new MultipleExecutionsAction(measurementActions1, getParameters());
        Action multipleExecutions2 = new MultipleExecutionsAction(measurementActions2, getParameters());

        multipleExecutions1.execute();
        multipleExecutions2.execute();

        setResult1(multipleExecutions1.fetchResults());
        setResult2(multipleExecutions2.fetchResults());

        System.out.println("Execution of " + getTestName() + " finished :)");
    }
}
