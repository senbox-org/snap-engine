package org.esa.snap.performance.performancetests;

import org.esa.snap.performance.util.Parameters;
import org.esa.snap.performance.util.TestResult;
import org.esa.snap.performance.util.Unit;

import java.io.IOException;

public class WriteSingleProductTest extends AbstractPerformanceTest {

    public WriteSingleProductTest(Parameters params) {
        super("write-single-product", params, Unit.MS);
    }

    @Override
    public void execute() throws IOException {
        System.out.println("Executing " + getTestName() + " test for product: " + getProductName());

        // TODO add implementation for single and multi threads

        setResult(new TestResult(
                getTestName(),
                getProductName(),
                getThreading(),
                getUnit(),
                24,
                15
        ));

        System.out.println("Test completed");
    }
}
