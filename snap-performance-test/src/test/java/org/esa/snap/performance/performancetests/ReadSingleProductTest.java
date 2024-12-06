package org.esa.snap.performance.performancetests;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StopWatch;
import org.esa.snap.performance.util.*;

import java.io.File;
import java.io.IOException;

public class ReadSingleProductTest extends AbstractPerformanceTest {

    public ReadSingleProductTest(String testName, Parameters params) {
        super(testName, params);
    }

    @Override
    public CalculationContainer executeTest(String format) throws IOException {
        int numExecutions = getNumExecutionsForAverageOperations();
        if (isDiscardingFirstMeasure()) {
            numExecutions++;
        }
        long[] times = new long[numExecutions];
        long maxMemoryConsumption = 0;

        String fullFilePath = TestUtils.buildProductPathString(getTestDataDir(), getProductName());
        File productFile;
        File dataFile;

        if (format.equals(TestUtils.FORMAT_DIMAP)) {
            productFile = new File(fullFilePath + TestUtils.EXTENSION_DIMAP);
            dataFile = new File(fullFilePath + TestUtils.EXTENSION_DIMAP_DATA);
        } else {
            productFile = new File(fullFilePath + TestUtils.EXTENSION_ZNAP_UNZIPPED);
            dataFile = productFile;
        }

        for (int ii = 0; ii < numExecutions; ii++) {
            long memoryBefore = TestUtils.getUsedMemory();
            StopWatch watch = new StopWatch();

            watch.start();
            Product product = ProductIO.readProduct(productFile);
            watch.stop();

            long memoryAfter = TestUtils.getUsedMemory();
            long memoryConsumption = memoryAfter - memoryBefore;
            if (memoryConsumption > maxMemoryConsumption) {
                maxMemoryConsumption = memoryConsumption;
            }

            product.dispose();
            times[ii] = watch.getTimeDiff();
        }

        return new CalculationContainer(times, new File[] {dataFile}, maxMemoryConsumption / (1024.0 * 1024.0));
    }
}
