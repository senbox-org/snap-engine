package org.esa.snap.core.gpf.execution;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.main.GPT;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class OperatorExecutionsTest {

    private final static String EMPTY_PARAMETERS_PART = "        <parameters/>\n";
    private final static String PARAMETER_PART =
            "            <{parameter_name}>{parameter_value}</{parameter_name}>\n";
    private final static String PARAMETERS_PART =
            "        <parameters>\n" +
                    "{parameter}" +
                    "        </parameters>\n";
    private final static String SINGLE_OPERATOR_GRAPH =
            "<graph id=\"graph\">\n" +
                    "    <version>1.0</version>\n" +
                    "\n" +
                    "    <node id=\"test_operator\">\n" +
                    "        <operator>{operator_name}</operator>\n" +
                    "{parameter_part}" +
                    "    </node>\n" +
                    "\n" +
                    "</graph>";
    private final static String OPERATOR_WITH_BAND_MATHS_GRAPH =
            "<graph id=\"graph\">\n" +
                    "    <version>1.0</version>\n" +
                    "\n" +
                    "    <node id=\"test_operator\">\n" +
                    "        <operator>{operator_name}</operator>\n" +
                    "    </node>\n" +
                    "\n" +
                    "    <node id=\"bandMaths\">\n" +
                    "        <operator>BandMaths</operator>\n" +
                    "        <sources>\n" +
                    "\t    <sourceProduct refid=\"test_operator\"/>\n" +
                    "\t</sources>\n" +
                    "        <parameters>\n" +
                    "            <targetBands>\n" +
                    "\t\t\t\t<targetBand>\n" +
                    "\t\t\t\t\t<name>computed_band1</name>\n" +
                    "\t\t\t\t\t<expression>band1 + 2</expression>\n" +
                    "\t\t\t\t\t<type>uint8</type>\n" +
                    "\t\t\t\t</targetBand>\n" +
                    "\t\t\t</targetBands>\n" +
                    "\n" +
                    "        </parameters>\n" +
                    "    </node>\n" +
                    "\n" +
                    "</graph>";
    private final static String OPERATOR_WITH_BAND_MATHS_GRAPH_2 =
            "<graph id=\"graph\">\n" +
                    "    <version>1.0</version>\n" +
                    "\n" +
                    "    <node id=\"test_operator\">\n" +
                    "        <operator>{operator_name}</operator>\n" +
                    "    </node>\n" +
                    "\n" +
                    "    <node id=\"bandMaths\">\n" +
                    "        <operator>BandMaths</operator>\n" +
                    "        <sources>\n" +
                    "\t    <sourceProduct refid=\"test_operator\"/>\n" +
                    "\t</sources>\n" +
                    "        <parameters>\n" +
                    "            <targetBands>\n" +
                    "\t\t\t\t<targetBand>\n" +
                    "\t\t\t\t\t<name>computed_band1</name>\n" +
                    "\t\t\t\t\t<expression>band1 + 2</expression>\n" +
                    "\t\t\t\t\t<type>uint8</type>\n" +
                    "\t\t\t\t</targetBand>\n" +
                    "\t\t\t\t<targetBand>\n" +
                    "\t\t\t\t\t<name>computed_band2</name>\n" +
                    "\t\t\t\t\t<expression>band2 + 2</expression>\n" +
                    "\t\t\t\t\t<type>uint8</type>\n" +
                    "\t\t\t\t</targetBand>\n" +
                    "\t\t\t</targetBands>\n" +
                    "\n" +
                    "        </parameters>\n" +
                    "    </node>\n" +
                    "\n" +
                    "</graph>";
    private static File outputDirectory;

    @BeforeClass
    public static void setUp() {
        outputDirectory = new File(GlobalTestConfig.getSnapTestDataOutputDirectory(), "OperatorExecutionsTest");
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                fail("Unable to create test output directory");
            }
        }
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new Operators.InitComputeTileOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(
                new Operators.InitDoExecuteComputeTileOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new Operators.InitJAIImageOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(
                new Operators.InitJAIImageDoExecuteOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(
                new Operators.InitDoExecuteSetsJAIImageOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(
                new Operators.InitDoExecuteSetsTargetProductAndJAIImageOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(
                new Operators.InitAndDoExecuteSetNoTargetProductOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(
                new Operators.InitSetsNoTargetProductOperatorSpi());
    }

    @AfterClass
    public static void tearDown() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(new Operators.InitComputeTileOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(
                new Operators.InitDoExecuteComputeTileOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(new Operators.InitJAIImageOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(
                new Operators.InitJAIImageDoExecuteOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(
                new Operators.InitDoExecuteSetsJAIImageOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(
                new Operators.InitDoExecuteSetsTargetProductAndJAIImageOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(
                new Operators.InitAndDoExecuteSetNoTargetProductOperatorSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(
                new Operators.InitSetsNoTargetProductOperatorSpi());
        if (outputDirectory.isDirectory()) {
            if (!FileUtils.deleteTree(outputDirectory)) {
                fail("Unable to delete test directory");
            }
        }
    }

    @Test
    public void testInitComputeTileOperatorWorksWithWriteAndRead() throws IOException {
        Product product = writeAndReadProduct(new Operators.InitComputeTileOperator(), "InitComputeTile");
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitComputeTileOperatorWorksWithOnlyInitialize() {
        Product product = onlyInitializeProduct(new Operators.InitComputeTileOperator());
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitComputeTileOperatorWorksWithRunAsSingleOperatorInGpt() throws Exception {
        Product product = runSingleOperatorInGpt("InitComputeTile", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitComputeTileOperatorWorksWithCreateProductFromGPF() {
        Product product = createProductFromGPF("InitComputeTile", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitComputeTileOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        Product product = runAsSingleOperatorInGraph("InitComputeTile", EMPTY_PARAMETERS_PART);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitComputeTileOperatorWorksWithRunAsConnectedOperatorInGraph() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorks_oneBand("InitComputeTile");
    }

    @Test
    public void testInitDoExecuteComputeTileOperatorWorksWithWriteAndRead() throws IOException {
        Product product = writeAndReadProduct(new Operators.InitDoExecuteComputeTileOperator(), "InitDoExecuteComputeTile");
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteComputeTileOperatorWorksWithOnlyInitialize() {
        Product product = onlyInitializeProduct(new Operators.InitDoExecuteComputeTileOperator());
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteComputeTileOperatorWorksWithRunAsSingleOperatorInGpt() throws Exception {
        Product product = runSingleOperatorInGpt("InitDoExecuteComputeTile", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteComputeTileOperatorWorksWithCreateProductFromGPF() {
        Product product = createProductFromGPF("InitDoExecuteComputeTile", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteComputeTileOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        Product product = runAsSingleOperatorInGraph("InitDoExecuteComputeTile", EMPTY_PARAMETERS_PART);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteComputeTileOperatorWorksWithRunAsConnectedOperatorInGraph() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorks_oneBand("InitDoExecuteComputeTile");
    }

    @Test
    public void testInitJAIImageOperatorWorksWithWriteAndRead() throws IOException {
        Product product = writeAndReadProduct(new Operators.InitJAIImageOperator(), "InitJAIImage");
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitJAIImageWorksWithOnlyInitialize() {
        Product product = onlyInitializeProduct(new Operators.InitJAIImageOperator());
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitJAIImageOperatorWorksWithRunAsSingleOperatorInGpt() throws Exception {
        Product product = runSingleOperatorInGpt("InitJAIImage", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitJAIImageOperatorWorksWithCreateProductFromGPF() {
        Product product = createProductFromGPF("InitJAIImage", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitJAIImageOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        Product product = runAsSingleOperatorInGraph("InitJAIImage", EMPTY_PARAMETERS_PART);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitJAIImageOperatorWorksWithRunAsConnectedOperatorInGraph() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorks_oneBand("InitJAIImage");
    }

    @Test
    public void testInitJAIImageDoExecuteOperatorWorksWithWriteAndRead() throws IOException {
        Product product = writeAndReadProduct(new Operators.InitJAIImageDoExecuteOperator(), "InitJAIImageDoExecute");
        assertExpectedBandsExist(product, new String[]{"band1", "band2"});
    }

    @Test
    public void testInitJAIImageDoExecuteWorksWithOnlyInitialize() {
        Product product = onlyInitializeProduct(new Operators.InitJAIImageDoExecuteOperator());
        try {
            assertExpectedBandsExist(product, new String[]{"band1", "band2"});
        } catch (AssertionError ae) {
            //after initialisation, we expect band1 to be present, but not band2
            assertExpectedBandsExist(product, new String[]{"band1"});
            return;
        }
        fail("AssertionError expected");
    }

    @Test
    public void testInitJAIImageDoExecuteOperatorWorksWithRunAsSingleOperatorInGpt() throws Exception {
        Product product = runSingleOperatorInGpt("InitJAIImageDoExecute", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1", "band2"});
    }

    @Test
    public void testInitJAIImageDoExecuteOperatorWorksWithCreateProductFromGPF() {
        Product product = createProductFromGPF("InitJAIImageDoExecute", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1", "band2"});
    }

    @Test
    public void testInitJAIImageDoExecuteOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        Product product = runAsSingleOperatorInGraph("InitJAIImageDoExecute", EMPTY_PARAMETERS_PART);
        assertExpectedBandsExist(product, new String[]{"band1", "band2"});
    }

    @Test
    public void testInitJAIImageDoExecuteOperatorWorksWithRunAsConnectedOperatorInGraph() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorks_twoBands("InitJAIImageDoExecute");
    }

    @Test
    public void testInitDoExecuteSetsJAIImageOperatorWorksWithWriteAndRead() throws IOException {
        Product product = writeAndReadProduct(new Operators.InitDoExecuteSetsJAIImageOperator(),
                "InitDoExecuteSetsJAIImage");
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteSetsJAIImageOperatorWorksWithOnlyInitialize() {
        Product product = onlyInitializeProduct(new Operators.InitDoExecuteSetsJAIImageOperator());
        assertEquals(1, product.getNumBands());
        Band band = product.getBand("band1");
        assertNotNull(band);
        assertEquals(0, band.getSampleInt(0, 0)); //only initialize, image has not been set yet
    }

    @Test
    public void testInitDoExecuteSetsJAIImageOperatorWorksWithRunAsSingleOperatorInGpt() throws Exception {
        Product product = runSingleOperatorInGpt("InitDoExecuteSetsJAIImage", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteSetsJAIImageOperatorWorksWithCreateProductFromGPF() {
        Product product = createProductFromGPF("InitDoExecuteSetsJAIImage", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteSetsJAIImageOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        Product product = runAsSingleOperatorInGraph("InitDoExecuteSetsJAIImage", EMPTY_PARAMETERS_PART);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteSetsJAIImageOperatorWorksWithRunAsConnectedOperatorInGraph() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorks_oneBand("InitDoExecuteSetsJAIImage");
    }

    @Test
    public void testInitDoExecuteSetsTargetProductAndJAIImageOperatorWorksWithWriteAndRead() throws IOException {
        Product product = writeAndReadProduct(new Operators.InitDoExecuteSetsTargetProductAndJAIImageOperator(),
                "InitDoExecuteSetsTargetProductAndJAIImage");
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteSetsTargetProductAndJAIImageOperatorWorksWithOnlyInitialize() {
        Product product = onlyInitializeProduct(new Operators.InitDoExecuteSetsTargetProductAndJAIImageOperator());
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteSetsTargetProductAndJAIImageOperatorWorksWithRunAsSingleOperatorInGpt() throws Exception {
        Product product = runSingleOperatorInGpt("InitDoExecuteSetsTargetProductAndJAIImage", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteSetsTargetProductAndJAIImageOperatorWorksWithCreateProductFromGPF() {
        Product product = createProductFromGPF("InitDoExecuteSetsTargetProductAndJAIImage", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteSetsTargetProductAndJAIImageOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        Product product = runAsSingleOperatorInGraph("InitDoExecuteSetsTargetProductAndJAIImage", EMPTY_PARAMETERS_PART);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteSetsTargetProductAndJAIImageOperatorWorksWithRunAsConnectedOperatorInGraph() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorks_oneBand("InitDoExecuteSetsTargetProductAndJAIImage");
    }

    @Test
    public void testInitAndDoExecuteSetNoTargetProductOperatorWorksWithWriteAndRead() throws IOException {
        String outputFileName = "OperatorExecutionsTest/InitAndDoExecuteSetNoTargetProduct_awr.txt";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        Operators.InitAndDoExecuteSetNoTargetProductOperator operator =
                new Operators.InitAndDoExecuteSetNoTargetProductOperator();
        operator.setParameter("outputFilePath", outputFile.getAbsolutePath());
        writeAndReadProduct(operator, "InitAndDoExecuteSetNoTargetProduct");
        assertExpectedFileExists(outputFile);
    }

    @Test
    public void testInitAndDoExecuteSetNoTargetProductOperatorWorksWithOnlyInitialize() {
        String outputFileName = "OperatorExecutionsTest/InitAndDoExecuteSetNoTargetProduct_oi.txt";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        Operators.InitAndDoExecuteSetNoTargetProductOperator operator =
                new Operators.InitAndDoExecuteSetNoTargetProductOperator();
        operator.setParameter("outputFilePath", outputFile.getAbsolutePath());
        onlyInitializeProduct(operator);
        assertExpectedFileExists(outputFile);
    }

    @Test
    public void testInitAndDoExecuteSetNoTargetProductOperatorWorksWithRunAsSingleOperatorInGpt() throws Exception {
        String outputFileName = "OperatorExecutionsTest/InitAndDoExecuteSetNoTargetProduct_rasoigpt.txt";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        runSingleOperatorInGpt("InitAndDoExecuteSetNoTargetProduct",
                new String[]{"outputFilePath", outputFile.getAbsolutePath()});
        assertExpectedFileExists(outputFile);
    }

    @Test
    public void testInitAndDoExecuteSetNoTargetProductOperatorWorksWithCreateProductFromGPF() {
        String outputFileName = "OperatorExecutionsTest/InitAndDoExecuteSetNoTargetProduct_cpfgpf.txt";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        createProductFromGPF("InitAndDoExecuteSetNoTargetProduct",
                new String[]{"outputFilePath", outputFile.getAbsolutePath()});
        assertExpectedFileExists(outputFile);
    }

    @Test
    public void testInitAndDoExecuteSetNoTargetProductOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        String outputFileName = "OperatorExecutionsTest/InitAndDoExecuteSetNoTargetProduct_rasoig.txt";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        String parameter = PARAMETER_PART.replace("{parameter_name}", "outputFilePath").
                replace("parameter_value", outputFile.getAbsolutePath());
        String parametersPart = PARAMETERS_PART.replace("{parameter}", parameter);
        runAsSingleOperatorInGraph("InitAndDoExecuteSetNoTargetProduct", parametersPart);
        assertExpectedFileExists(outputFile);
    }

    @Test
    public void testInitSetsNoTargetProductOperatorWorksWithWriteAndRead() throws IOException {
        String outputFileName = "OperatorExecutionsTest/InitSetsNoTargetProduct_awr.txt";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        Operator operator = new Operators.InitSetsNoTargetProductOperator();
        operator.setParameter("outputFilePath", outputFile.getAbsolutePath());
        writeAndReadProduct(operator, "InitSetsNoTargetProduct");
        assertExpectedFileExists(outputFile);
    }

    @Test
    public void testInitSetsNoTargetProductOperatorWorksWithOnlyInitialize() {
        String outputFileName = "OperatorExecutionsTest/InitSetsNoTargetProduct_oi.txt";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        Operator operator = new Operators.InitSetsNoTargetProductOperator();
        operator.setParameter("outputFilePath", outputFile.getAbsolutePath());
        onlyInitializeProduct(operator);
        assertExpectedFileExists(outputFile);
    }

    @Test
    public void testInitSetsNoTargetProductOperatorWorksWithRunAsSingleOperatorInGpt() throws Exception {
        String outputFileName = "OperatorExecutionsTest/InitSetsNoTargetProduct_rasoigpt.txt";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        runSingleOperatorInGpt("InitSetsNoTargetProduct", new String[]{"outputFilePath", outputFile.getAbsolutePath()});
        assertExpectedFileExists(outputFile);
    }

    @Test
    public void testInitSetsNoTargetProductOperatorWorksWithCreateProductFromGPF() {
        String outputFileName = "OperatorExecutionsTest/InitAndDoExecuteSetNoTargetProduct_cpfgpf.txt";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        createProductFromGPF("InitAndDoExecuteSetNoTargetProduct",
                new String[]{"outputFilePath", outputFile.getAbsolutePath()});
        assertExpectedFileExists(outputFile);
    }

    @Test
    public void testInitSetsNoTargetProductOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        String outputFileName = "OperatorExecutionsTest/InitSetsNoTargetProduct_rasoig.txt";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        String parameter = PARAMETER_PART.replace("{parameter_name}", "outputFilePath").
                replace("{parameter_value}", outputFile.getAbsolutePath());
        String parametersPart = PARAMETERS_PART.replace("{parameter}", parameter);
        runAsSingleOperatorInGraph("InitSetsNoTargetProduct", parametersPart);
        assertExpectedFileExists(outputFile);
    }

    private Product writeAndReadProduct(Operator operator, String operatorName) throws IOException {
        String outputFileName = "OperatorExecutionsTest/" + operatorName + "_awr.dim";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        GPF.writeProduct(operator.getTargetProduct(), outputFile, "BEAM-DIMAP", false,
                ProgressMonitor.NULL);
        return ProductIO.readProduct(outputFile);
    }

    private Product onlyInitializeProduct(Operator operator) {
        return operator.getTargetProduct();
    }

    private Product runSingleOperatorInGpt(String operatorName, String[] parameters) throws Exception {
        String outputFileName = "OperatorExecutionsTest/" + operatorName + "_rasoigpt.dim";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();
        ArrayList<String> arguments = new ArrayList<>();
        arguments.add(operatorName);
        for (int i = 0; i < parameters.length; i += 2) {
            arguments.add("-P" + parameters[i] + "=" + parameters[i + 1]);
        }
        arguments.add("-t");
        arguments.add(outputFilePath);
        GPT.run(arguments.toArray(new String[0]));
        return ProductIO.readProduct(outputFilePath);
    }

    private Product createProductFromGPF(String operatorName, String[] parameters) {
        Map<String, Object> parametersMap = new HashMap<>();
        for (int i = 0; i < parameters.length; i += 2) {
            parametersMap.put(parameters[i], parameters[i + 1]);
        }
        return GPF.createProduct(operatorName, parametersMap);
    }

    private Product runAsSingleOperatorInGraph(String operatorName, String parameterPart) throws Exception {
        String graph = SINGLE_OPERATOR_GRAPH.replace("{operator_name}", operatorName);
        graph = graph.replace("{parameter_part}", parameterPart);
        String graphFileName = "OperatorExecutionsTest/" + operatorName + "_rasoig.xml";
        File graphFile = GlobalTestConfig.getSnapTestDataOutputFile(graphFileName);
        PrintWriter printWriter = new PrintWriter(graphFile);
        printWriter.print(graph);
        printWriter.close();
        String outputFileName = "OperatorExecutionsTest/" + operatorName + "_rasoig.dim";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();
        GPT.run(new String[]{graphFile.getAbsolutePath(), "-t", outputFilePath});
        return ProductIO.readProduct(outputFilePath);
    }

    private void assertRunningAsConnectedOperatorInGraphWorks_oneBand(String operatorName) throws Exception {
        String graph = OPERATOR_WITH_BAND_MATHS_GRAPH.replace("{operator_name}", operatorName);
        String graphFileName = "OperatorExecutionsTest/" + operatorName + "_racoig.dim";
        File graphFile = GlobalTestConfig.getSnapTestDataOutputFile(graphFileName);
        PrintWriter printWriter = new PrintWriter(graphFile);
        printWriter.print(graph);
        printWriter.close();

        String outputFileName = "OperatorExecutionsTest/" + operatorName + "_racoigp.dim";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();
        GPT.run(new String[]{graphFile.getAbsolutePath(), "-t", outputFilePath});
        Product product = ProductIO.readProduct(outputFilePath);
        Band band = product.getBand("computed_band1");
        assertNotNull(band);
        assertEquals(5, band.getSampleInt(0, 0));
    }

    private void assertRunningAsConnectedOperatorInGraphWorks_twoBands(String operatorName) throws Exception {
        String graph = OPERATOR_WITH_BAND_MATHS_GRAPH_2.replace("{operator_name}", operatorName);
        String graphFileName = "OperatorExecutionsTest/" + operatorName + "_racoig.dim";
        File graphFile = GlobalTestConfig.getSnapTestDataOutputFile(graphFileName);
        PrintWriter printWriter = new PrintWriter(graphFile);
        printWriter.print(graph);
        printWriter.close();

        String outputFileName = "OperatorExecutionsTest/" + operatorName + "_racoigp.dim";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();
        GPT.run(new String[]{graphFile.getAbsolutePath(), "-t", outputFilePath});
        Product product = ProductIO.readProduct(outputFilePath);
        Band band1 = product.getBand("computed_band1");
        assertNotNull(band1);
        assertEquals(5, band1.getSampleInt(0, 0));
        Band band2 = product.getBand("computed_band1");
        assertNotNull(band2);
        assertEquals(5, band2.getSampleInt(0, 0));
    }

    private void assertExpectedBandsExist(Product product, String[] expectedBands) {
        assertEquals(expectedBands.length, product.getNumBands());
        for (String expectedBand : expectedBands) {
            Band band = product.getBand(expectedBand);
            assertNotNull(band);
            assertEquals(3, band.getSampleInt(0, 0));
        }
    }

    private void assertExpectedFileExists(File outputFile) {
        assert (outputFile.exists());
    }


}
