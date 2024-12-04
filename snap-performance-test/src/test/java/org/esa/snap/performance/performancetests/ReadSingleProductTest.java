package org.esa.snap.performance.performancetests;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StopWatch;
import org.esa.snap.performance.util.TestResult;
import org.esa.snap.performance.util.Parameters;
import org.esa.snap.performance.util.TestUtils;
import org.esa.snap.performance.util.Unit;

import java.io.File;
import java.io.IOException;

public class ReadSingleProductTest extends AbstractPerformanceTest {

    public ReadSingleProductTest(Parameters params) {
        super("read-single-product", params, Unit.MS);
    }

    @Override
    public void execute() throws IOException {
        System.out.println("Executing " + getTestName() + " test for product: " + getProductName());

        int numExecutions = getNumExecutionsForAverageOperations();
        if (isDiscardingFirstMeasure()) {
            numExecutions++;
        }
        long[] timesDiMap = new long[numExecutions];
        long[] timesZNAP = new long[numExecutions];

        File productFileDimap = TestUtils.buildProductPath(getTestDataDir(), getProductName() + ".dim");
        File productFileZNAP = TestUtils.buildProductPath(getTestDataDir(), getProductName() + ".znap");

        for (int ii = 0; ii < numExecutions; ii++) {
            timesDiMap[ii] = executeTest(productFileDimap);
            timesZNAP[ii] = executeTest(productFileZNAP);
        }

        timesDiMap = TestUtils.reduceResultArray(isDiscardingFirstMeasure(), timesDiMap);
        timesZNAP = TestUtils.reduceResultArray(isDiscardingFirstMeasure(), timesZNAP);
        double resultDiMap = TestUtils.calculateArithmeticMean(timesDiMap);
        double resultZNAP = TestUtils.calculateArithmeticMean(timesZNAP);

        setResult(new TestResult(
                getTestName(),
                getProductName(),
                getThreading(),
                getUnit(),
                resultDiMap,
                resultZNAP
        ));

        System.out.println("Test completed");
    }

    private long executeTest(File productFile) throws IOException {

        // TODO implement threading!!!
        // this is single threaded!!!
        StopWatch watch = new StopWatch();
        watch.start();
        Product product = ProductIO.readProduct(productFile);
        watch.stop();
        product.dispose();
        return watch.getTimeDiff();
    }
}
