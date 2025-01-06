package org.esa.snap.performance.testImplementation;

import org.esa.snap.core.util.StringUtils;
import org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants;
import org.esa.snap.performance.actions.*;
import org.esa.snap.performance.util.Parameters;
import org.esa.snap.performance.util.TestUtils;
import org.esa.snap.runtime.Config;

import java.io.IOException;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.DEFAULT_USE_ZIP_ARCHIVE;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE;

public class WriteFromReaderPerformanceTest extends PerformanceTest {

    public WriteFromReaderPerformanceTest(String testName, Parameters parameters) {
        super(testName, parameters);
    }

    @Override
    public void execute() throws IOException {
        System.out.println("Execution of " + getTestName() + " started....");

        String productName = getParameters().getProducts().get(0);
        String testDataDir = getParameters().getTestDir();

        String outputDir = getParameters().getOutputDir();
        String productNameWithoutExtension = TestUtils.cutExtensionFromFileName(productName);
        String fullOutputDir = TestUtils.createOutputDir(outputDir, getTestName(), productNameWithoutExtension, getParameters().getThreading());

        List<String> outputFormats = getParameters().getOutputFormats();
        String format1 = outputFormats.get(0);
        String format2 = outputFormats.get(1);

        TestUtils.setFlavour(getParameters().isUseZip());

        String threading = getParameters().getThreading();
        Action baseAction1 = new WriteAction(null, fullOutputDir, format1, threading);
        Action baseAction2 = new WriteAction(null, fullOutputDir, format2, threading);

        Action measurementActions1 = TestUtils.constructMeasurementActionsPipeline(baseAction1, getParameters());
        Action measurementActions2 = TestUtils.constructMeasurementActionsPipeline(baseAction2, getParameters());

        Action readBeforeExecutionAction1 = new ReadBeforeExecutionAction(measurementActions1, productName, testDataDir);
        Action readBeforeExecutionAction2 = new ReadBeforeExecutionAction(measurementActions2, productName, testDataDir);

        Action multipleExecutions1 = new MultipleExecutionsAction(readBeforeExecutionAction1, getParameters());
        Action multipleExecutions2 = new MultipleExecutionsAction(readBeforeExecutionAction2, getParameters());

        multipleExecutions1.execute();
        multipleExecutions2.execute();

        setResult1(multipleExecutions1.fetchResults());
        setResult2(multipleExecutions2.fetchResults());

        System.out.println("Execution of " + getTestName() + " finished :)");
    }

    /*private void setFlavour(boolean useZip) {
        System.out.println(useZip);

        Preferences preferences = Config.instance("snap").load().preferences();

        // TODO use static variables from ZNAPPreferencesConstants
        if (!useZip) {
            preferences.put(ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE, String.valueOf(false));
            preferences.put(ZnapPreferencesConstants.PROPERTY_NAME_COMPRESSOR_ID, "zlib");
//            System.setProperty("znap.use.zip.archive", "false");
//            System.setProperty("znap.compressor.id", "zlib");
        } else {
            preferences.remove(ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE);
            preferences.remove(ZnapPreferencesConstants.PROPERTY_NAME_COMPRESSOR_ID);
//            System.clearProperty("znap.use.zip.archive");
//            System.clearProperty("znap.compressor.id");
        }

        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            System.out.println("Config preferences for Flavour could not be stored.");
        }



    }*/
}
