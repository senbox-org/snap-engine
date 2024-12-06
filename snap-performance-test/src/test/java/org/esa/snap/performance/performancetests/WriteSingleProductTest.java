package org.esa.snap.performance.performancetests;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StopWatch;
import org.esa.snap.performance.util.*;

import java.io.File;
import java.io.IOException;

public abstract class WriteSingleProductTest extends AbstractPerformanceTest {

    public WriteSingleProductTest(String testName, Parameters params) {
        super(testName, params);
    }

    @Override
    public CalculationContainer executeTest(String format) throws IOException {

        String productNameWithoutExtension = TestUtils.cutExtensionFromFileName(getProductName());
        String outputPath = TestUtils.buildProductPathString(getOutputDir(), getTestName(), productNameWithoutExtension, getThreading().getName());
        String outputFilePath = TestUtils.buildProductPathString(outputPath, productNameWithoutExtension);
        new File(outputPath).mkdirs();

        Product sourceProduct = readProduct();

        int numExecutions = getNumExecutionsForAverageOperations();
        if (isDiscardingFirstMeasure()) {
            numExecutions++;
        }
        long[] times = new long[numExecutions];
        long maxMemoryConsumption = 0;
        File[] files = new File[numExecutions];

        for (int ii = 0; ii < numExecutions; ii++) {
            long memoryBefore = TestUtils.getUsedMemory();
            String fullFilePath = outputFilePath + String.format("_%04d", ii);
            StopWatch watch = new StopWatch();

            watch.start();

            ProductIO.writeProduct(sourceProduct, fullFilePath, format);

            watch.stop();

            long memoryAfter = TestUtils.getUsedMemory();
            long memoryConsumption = memoryAfter - memoryBefore;
            if (memoryConsumption > maxMemoryConsumption) {
                maxMemoryConsumption = memoryConsumption;
            }

            times[ii] = watch.getTimeDiff();

            if (format.equals(TestUtils.FORMAT_DIMAP)) {
                files[ii] = new File(fullFilePath + TestUtils.EXTENSION_DIMAP_DATA);
            } else {
                files[ii] = new File(fullFilePath + TestUtils.EXTENSION_ZNAP);
            }
        }

        return new CalculationContainer(times, files, maxMemoryConsumption / (1024.0 * 1024.0));
    }

    public abstract Product readProduct() throws IOException;
}
