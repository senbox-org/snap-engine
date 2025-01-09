package org.esa.snap.performance.testImplementation;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.performance.actions.Action;
import org.esa.snap.performance.actions.MultipleExecutionsAction;
import org.esa.snap.performance.actions.ReadProductToMemoryAction;
import org.esa.snap.performance.actions.WriteAction;
import org.esa.snap.performance.util.Parameters;
import org.esa.snap.performance.util.TestUtils;

import java.util.List;
import java.util.logging.Logger;

public class WriteFromMemoryPerformanceTest extends PerformanceTest {

    private final Logger logger = Logger.getLogger(WriteFromMemoryPerformanceTest.class.getName());

    public WriteFromMemoryPerformanceTest(String testName, Parameters parameters) {
        super(testName, parameters);
    }

    @Override
    public void execute() throws Throwable {
        logger.info("Execution of " + getTestName() + " started....");

        String productName = getParameters().getProducts().get(0);
        String testDataDir = getParameters().getTestDir();

        String outputDir = getParameters().getOutputDir();
        String productNameWithoutExtension = TestUtils.cutExtensionFromFileName(productName);
        String fullOutputDir = TestUtils.createOutputDir(outputDir, getTestName(), productNameWithoutExtension, getParameters().getThreading());

        List<String> outputFormats = getParameters().getOutputFormats();
        String format1 = outputFormats.get(0);
        String format2 = outputFormats.get(1);

        Action readProductToMemory = new ReadProductToMemoryAction(productName, testDataDir);
        readProductToMemory.execute();
        Product product = (Product) readProductToMemory.fetchResults().get(0).getValue();

        TestUtils.setFlavour(getParameters().isUseZip());

        String threading = getParameters().getThreading();
        Action baseAction1 = new WriteAction(product, fullOutputDir, format1, threading);
        Action baseAction2 = new WriteAction(product, fullOutputDir, format2, threading);

        Action measurementActions1 = TestUtils.constructMeasurementActionsPipeline(baseAction1, getParameters());
        Action measurementActions2 = TestUtils.constructMeasurementActionsPipeline(baseAction2, getParameters());

        Action multipleExecutions1 = new MultipleExecutionsAction(measurementActions1, getParameters());
        Action multipleExecutions2 = new MultipleExecutionsAction(measurementActions2, getParameters());

        multipleExecutions1.execute();
        multipleExecutions2.execute();

        setResult1(multipleExecutions1.fetchResults());
        setResult2(multipleExecutions2.fetchResults());

        logger.info("Execution of " + getTestName() + " finished :)");
    }
}
