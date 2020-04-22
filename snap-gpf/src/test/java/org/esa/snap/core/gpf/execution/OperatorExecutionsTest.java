package org.esa.snap.core.gpf.execution;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.main.GPT;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
    private final static String OPERATOR_WITH_FOLLOW_UP_DO_EXECUTE_GRAPH =
            "<graph id=\"graph\">\n" +
                    "    <version>1.0</version>\n" +
                    "\n" +
                    "    <node id=\"test_operator\">\n" +
                    "        <operator>{operator_name}</operator>\n" +
                    "    </node>\n" +
                    "\n" +
                    "    <node id=\"follow_up_do_execute\">\n" +
                    "        <operator>FollowUpDoExecute</operator>\n" +
                    "        <sources>\n" +
                    "\t    <sourceProduct refid=\"test_operator\"/>\n" +
                    "\t</sources>\n" +
                    "    </node>\n" +
                    "\n" +
                    "</graph>";
    private final static String OPERATOR_WITH_FOLLOW_UP_DO_EXECUTE_AND_COMPUTE_TILE_GRAPH =
            "<graph id=\"graph\">\n" +
                    "    <version>1.0</version>\n" +
                    "\n" +
                    "    <node id=\"test_operator\">\n" +
                    "        <operator>{operator_name}</operator>\n" +
                    "    </node>\n" +
                    "\n" +
                    "    <node id=\"follow_up_do_execute_and_compute_tile\">\n" +
                    "        <operator>FollowUpDoExecuteAndComputeTile</operator>\n" +
                    "        <sources>\n" +
                    "\t    <sourceProduct refid=\"test_operator\"/>\n" +
                    "\t</sources>\n" +
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
    private static Operators.InitComputeTileOperatorSpi initComputeTileOperatorSpi;
    private static Operators.InitDoExecuteComputeTileOperatorSpi initDoExecuteComputeTileOperatorSpi;
    private static Operators.InitJAIImageOperatorSpi initJAIImageOperatorSpi;
    private static Operators.InitJAIImageDoExecuteOperatorSpi initJAIImageDoExecuteOperatorSpi;
    private static Operators.InitDoExecuteSetsJAIImageOperatorSpi initDoExecuteSetsJAIImageOperatorSpi;
    private static Operators.InitAndDoExecuteSetNoTargetProductOperatorSpi initAndDoExecuteSetNoTargetProductOperatorSpi;
    private static Operators.InitSetsNoTargetProductOperatorSpi initSetsNoTargetProductOperatorSpi;
    private static Operators.FollowUpDoExecuteOperatorSpi followUpDoExecuteOperatorSpi;
    private static Operators.FollowUpDoExecuteAndComputeTileOperatorSpi followUpDoExecuteAndComputeTileOperatorSpi;
    private static Operators.InitDoExecuteAddsBandComputeTileOperatorSpi initDoExecuteAddsBandComputeTileOperatorSpi;

    @BeforeClass
    public static void setUp() {
        outputDirectory = new File(GlobalTestConfig.getSnapTestDataOutputDirectory(), "OperatorExecutionsTest");
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                fail("Unable to create test output directory");
            }
        }
        initComputeTileOperatorSpi = new Operators.InitComputeTileOperatorSpi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(initComputeTileOperatorSpi);
        initDoExecuteComputeTileOperatorSpi = new Operators.InitDoExecuteComputeTileOperatorSpi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(initDoExecuteComputeTileOperatorSpi);
        initDoExecuteAddsBandComputeTileOperatorSpi = new Operators.InitDoExecuteAddsBandComputeTileOperatorSpi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(initDoExecuteAddsBandComputeTileOperatorSpi);
        initJAIImageOperatorSpi = new Operators.InitJAIImageOperatorSpi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(initJAIImageOperatorSpi);
        initJAIImageDoExecuteOperatorSpi = new Operators.InitJAIImageDoExecuteOperatorSpi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(initJAIImageDoExecuteOperatorSpi);
        initDoExecuteSetsJAIImageOperatorSpi = new Operators.InitDoExecuteSetsJAIImageOperatorSpi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(initDoExecuteSetsJAIImageOperatorSpi);
        initAndDoExecuteSetNoTargetProductOperatorSpi = new Operators.InitAndDoExecuteSetNoTargetProductOperatorSpi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(
                initAndDoExecuteSetNoTargetProductOperatorSpi);
        initSetsNoTargetProductOperatorSpi = new Operators.InitSetsNoTargetProductOperatorSpi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(initSetsNoTargetProductOperatorSpi);
        followUpDoExecuteOperatorSpi = new Operators.FollowUpDoExecuteOperatorSpi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(followUpDoExecuteOperatorSpi);
        followUpDoExecuteAndComputeTileOperatorSpi = new Operators.FollowUpDoExecuteAndComputeTileOperatorSpi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(followUpDoExecuteAndComputeTileOperatorSpi);
    }

    @AfterClass
    public static void tearDown() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(initComputeTileOperatorSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(initDoExecuteComputeTileOperatorSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(initDoExecuteAddsBandComputeTileOperatorSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(initJAIImageOperatorSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(initJAIImageDoExecuteOperatorSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(initDoExecuteSetsJAIImageOperatorSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(
                initAndDoExecuteSetNoTargetProductOperatorSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(initSetsNoTargetProductOperatorSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(followUpDoExecuteOperatorSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(followUpDoExecuteAndComputeTileOperatorSpi);
        if (outputDirectory.isDirectory()) {
            Path directoryPath = Paths.get(outputDirectory.toURI());
            try {
                Files.walk(directoryPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
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
        Product product = runSingleOperatorInGptAndReadProduct("InitComputeTile", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitComputeTileOperatorWorksWithCreateProductFromGPF() {
        Product product = createProductFromGPF("InitComputeTile", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitComputeTileOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        Product product = runAsSingleOperatorInGraphAndReadProduct("InitComputeTile", EMPTY_PARAMETERS_PART);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitComputeTileOperatorWorksWithRunAsConnectedOperatorInGraph_BandMaths() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithBandMaths_oneBand("InitComputeTile");
    }

    @Test
    public void testInitComputeTileOperatorWorksWithRunAsConnectedOperatorInGraph_FollowUpDoExecute() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecute_oneBand("InitComputeTile");
    }

    @Test
    public void testInitComputeTileOperatorWorksWithRunAsConnectedOperatorInGraph_FollowUpDoExecuteAndComputeTile() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecuteAndComputeTile_oneBand("InitComputeTile");
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
        Product product = runSingleOperatorInGptAndReadProduct("InitDoExecuteComputeTile", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteComputeTileOperatorWorksWithCreateProductFromGPF() {
        Product product = createProductFromGPF("InitDoExecuteComputeTile", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteComputeTileOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        Product product = runAsSingleOperatorInGraphAndReadProduct("InitDoExecuteComputeTile", EMPTY_PARAMETERS_PART);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteComputeTileOperatorWorksWithRunAsConnectedOperatorInGraph_BandMaths() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithBandMaths_oneBand("InitDoExecuteComputeTile");
    }

    @Test
    public void testInitDoExecuteComputeTileOperatorWorksWithRunAsConnectedOperatorInGraph_FollowUpDoExecute() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecute_oneBand("InitDoExecuteComputeTile");
    }

    @Test
    public void testInitDoExecuteComputeTileOperatorWorksWithRunAsConnectedOperatorInGraph_FollowUpDoExecuteAndComputeTile() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecuteAndComputeTile_oneBand("InitDoExecuteComputeTile");
    }

    @Test
    public void testInitDoExecuteAddsBandComputeTileOperatorWorksWithWriteAndRead() throws IOException {
        Product product = writeAndReadProduct(new Operators.InitDoExecuteAddsBandComputeTileOperator(), "InitDoExecuteAddsBandComputeTile");
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteAddsBandComputeTileOperatorWorksWithOnlyInitialize() {
        Product product = onlyInitializeProduct(new Operators.InitDoExecuteAddsBandComputeTileOperator());
        try {
            assertExpectedBandsExist(product, new String[]{"band1"});
        } catch (AssertionError ae) {
            //after initialisation, we expect band1 not to be present yet
            assertExpectedBandsExist(product, new String[]{});
            return;
        }
        fail("AssertionError expected");
    }

    @Test
    public void testInitDoExecuteAddsBandComputeTileOperatorWorksWithRunAsSingleOperatorInGpt() throws Exception {
        Product product = runSingleOperatorInGptAndReadProduct("InitDoExecuteAddsBandComputeTile", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteAddsBandComputeTileOperatorWorksWithCreateProductFromGPF() {
        Product product = createProductFromGPF("InitDoExecuteAddsBandComputeTile", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteAddsBandComputeTileOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        Product product = runAsSingleOperatorInGraphAndReadProduct("InitDoExecuteAddsBandComputeTile", EMPTY_PARAMETERS_PART);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteAddsBandComputeTileOperatorWorksWithRunAsConnectedOperatorInGraph_BandMaths() throws Exception {
        try {
            assertRunningAsConnectedOperatorInGraphWorksWithBandMaths_oneBand("InitDoExecuteAddsBandComputeTile");
        } catch (GraphException ge) {
            //after initialisation, we expect band1 not to be present yet (and therefore there will be a graph exception)
            return;
        }
        fail("AssertionError expected");
    }

    @Test
    public void testInitDoExecuteAddsBandComputeTileOperatorWorksWithRunAsConnectedOperatorInGraph_FollowUpDoExecute() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecute_oneBand("InitDoExecuteAddsBandComputeTile");
    }

    @Test
    public void testInitDoExecuteAddsBandComputeTileOperatorWorksWithRunAsConnectedOperatorInGraph_FollowUpDoExecuteAndComputeTile() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecuteAndComputeTile_oneBand("InitDoExecuteAddsBandComputeTile");
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
        Product product = runSingleOperatorInGptAndReadProduct("InitJAIImage", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitJAIImageOperatorWorksWithCreateProductFromGPF() {
        Product product = createProductFromGPF("InitJAIImage", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitJAIImageOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        Product product = runAsSingleOperatorInGraphAndReadProduct("InitJAIImage", EMPTY_PARAMETERS_PART);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitJAIImageOperatorWorksWithRunAsConnectedOperatorInGraph_BandMaths() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithBandMaths_oneBand("InitJAIImage");
    }

    @Test
    public void testInitJAIImageOperatorWorksWithRunAsConnectedOperatorInGraph_FollowUpDoExecute() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecute_oneBand("InitJAIImage");
    }

    @Test
    public void testInitJAIImageOperatorWorksWithRunAsConnectedOperatorInGraph_FollowUpDoExecuteAndComputeTile() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecuteAndComputeTile_oneBand("InitJAIImage");
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
        Product product = runSingleOperatorInGptAndReadProduct("InitJAIImageDoExecute", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1", "band2"});
    }

    @Test
    public void testInitJAIImageDoExecuteOperatorWorksWithCreateProductFromGPF() {
        Product product = createProductFromGPF("InitJAIImageDoExecute", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1", "band2"});
    }

    @Test
    public void testInitJAIImageDoExecuteOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        Product product = runAsSingleOperatorInGraphAndReadProduct("InitJAIImageDoExecute", EMPTY_PARAMETERS_PART);
        assertExpectedBandsExist(product, new String[]{"band1", "band2"});
    }

    @Test
    public void testInitJAIImageDoExecuteOperatorWorksWithRunAsConnectedOperatorInGraph_BandMaths() throws Exception {
        Product product = onlyInitializeProduct(new Operators.InitJAIImageDoExecuteOperator());
        try {
            assertRunningAsConnectedOperatorInGraphWorksWithBandMaths_twoBands("InitJAIImageDoExecute");
        } catch (GraphException ge) {
            // this will fail when both bands are asked for, as BandMaths requires the bands to be present in the initialize method
            assertRunningAsConnectedOperatorInGraphWorksWithBandMaths_oneBand("InitJAIImageDoExecute");
            return;
        }
        fail("AssertionError expected");
    }

    @Test
    public void testInitJAIImageDoExecuteOperatorWorksWithRunAsConnectedOperatorInGraph_FollowUpDoExecute() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecute_twoBands("InitJAIImageDoExecute");
    }

    @Test
    public void testInitJAIImageDoExecuteOperatorWorksWithRunAsConnectedOperatorInGraph_FollowUpDoExecuteAndComputeTile() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecuteAndComputeTile_twoBands("InitJAIImageDoExecute");
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
        Product product = runSingleOperatorInGptAndReadProduct("InitDoExecuteSetsJAIImage", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteSetsJAIImageOperatorWorksWithCreateProductFromGPF() {
        Product product = createProductFromGPF("InitDoExecuteSetsJAIImage", new String[0]);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteSetsJAIImageOperatorWorksWithRunAsSingleOperatorInGraph() throws Exception {
        Product product = runAsSingleOperatorInGraphAndReadProduct("InitDoExecuteSetsJAIImage", EMPTY_PARAMETERS_PART);
        assertExpectedBandsExist(product, new String[]{"band1"});
    }

    @Test
    public void testInitDoExecuteSetsJAIImageOperatorWorksWithRunAsConnectedOperatorInGraph_BandMaths() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithBandMaths_oneBand("InitDoExecuteSetsJAIImage");
    }

    @Test
    public void testInitDoExecuteSetsJAIImageOperatorWorksWithRunAsConnectedOperatorInGraph_FollowUpDoExecute() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecute_oneBand("InitDoExecuteSetsJAIImage");
    }

    @Test
    public void testInitDoExecuteSetsJAIImageOperatorWorksWithRunAsConnectedOperatorInGraph_FollowUpDoExecuteAndComputeTile() throws Exception {
        assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecuteAndComputeTile_oneBand("InitJAIImageDoExecute");
    }

    @Test
    public void testInitAndDoExecuteSetNoTargetProductOperatorWorksWithWriteAndRead() throws IOException {
        String outputFileName = "OperatorExecutionsTest/InitAndDoExecuteSetNoTargetProduct_awr.txt";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        Operators.InitAndDoExecuteSetNoTargetProductOperator operator =
                new Operators.InitAndDoExecuteSetNoTargetProductOperator();
        operator.setParameter("outputFilePath", outputFile.getAbsolutePath());
        writeProduct(operator, "InitAndDoExecuteSetNoTargetProduct");
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
                replace("{parameter_value}", outputFile.getAbsolutePath());
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
        writeProduct(operator, "InitSetsNoTargetProduct");
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

    private void writeProduct(Operator operator, String operatorName) throws IOException {
        String outputFileName = "OperatorExecutionsTest/" + operatorName + "_aw.dim";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        GPF.writeProduct(operator.getTargetProduct(), outputFile, "BEAM-DIMAP", false,
                ProgressMonitor.NULL);
    }

    private Product onlyInitializeProduct(Operator operator) {
        return operator.getTargetProduct();
    }

    private Product runSingleOperatorInGptAndReadProduct(String operatorName, String[] parameters) throws Exception {
        String outputFilePath = runSingleOperatorInGpt(operatorName, parameters);
        return ProductIO.readProduct(outputFilePath);
    }

    private String runSingleOperatorInGpt(String operatorName, String[] parameters) throws Exception {
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
        return outputFilePath;
    }

    private Product createProductFromGPF(String operatorName, String[] parameters) {
        Map<String, Object> parametersMap = new HashMap<>();
        for (int i = 0; i < parameters.length; i += 2) {
            parametersMap.put(parameters[i], parameters[i + 1]);
        }
        return GPF.createProduct(operatorName, parametersMap);
    }

    private Product runAsSingleOperatorInGraphAndReadProduct(String operatorName, String parameterPart) throws Exception {
        String outputFilePath = runAsSingleOperatorInGraph(operatorName, parameterPart);
        return ProductIO.readProduct(outputFilePath);
    }

    private String runAsSingleOperatorInGraph(String operatorName, String parameterPart) throws Exception {
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
        return outputFilePath;
    }

    private void assertRunningAsConnectedOperatorInGraphWorksWithBandMaths_oneBand(String operatorName) throws Exception {
        String graph = OPERATOR_WITH_BAND_MATHS_GRAPH.replace("{operator_name}", operatorName);
        String graphFileName = "OperatorExecutionsTest/" + operatorName + "_racoig_bm.xml";
        File graphFile = GlobalTestConfig.getSnapTestDataOutputFile(graphFileName);
        PrintWriter printWriter = new PrintWriter(graphFile);
        printWriter.print(graph);
        printWriter.close();

        String outputFileName = "OperatorExecutionsTest/" + operatorName + "_racoig_bm.dim";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();
        GPT.run(new String[]{graphFile.getAbsolutePath(), "-t", outputFilePath});
        Product product = ProductIO.readProduct(outputFilePath);
        Band band = product.getBand("computed_band1");
        assertNotNull(band);
        assertEquals(5, band.getSampleInt(0, 0));
    }

    private void assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecute_oneBand(String operatorName) throws Exception {
        String graph = OPERATOR_WITH_FOLLOW_UP_DO_EXECUTE_GRAPH.replace("{operator_name}", operatorName);
        String graphFileName = "OperatorExecutionsTest/" + operatorName + "_racoig_fudo.xml";
        File graphFile = GlobalTestConfig.getSnapTestDataOutputFile(graphFileName);
        PrintWriter printWriter = new PrintWriter(graphFile);
        printWriter.print(graph);
        printWriter.close();

        String outputFileName = "OperatorExecutionsTest/" + operatorName + "_racoig_fudo.dim";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();
        GPT.run(new String[]{graphFile.getAbsolutePath(), "-t", outputFilePath});
        Product product = ProductIO.readProduct(outputFilePath);
        Band band = product.getBand("computed_band1");
        assertNotNull(band);
        assertEquals(5, band.getSampleInt(0, 0));
    }

    private void assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecute_twoBands(String operatorName) throws Exception {
        String graph = OPERATOR_WITH_FOLLOW_UP_DO_EXECUTE_GRAPH.replace("{operator_name}", operatorName);
        String graphFileName = "OperatorExecutionsTest/" + operatorName + "_racoig_fudo.xml";
        File graphFile = GlobalTestConfig.getSnapTestDataOutputFile(graphFileName);
        PrintWriter printWriter = new PrintWriter(graphFile);
        printWriter.print(graph);
        printWriter.close();

        String outputFileName = "OperatorExecutionsTest/" + operatorName + "_racoig_fudo.dim";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();
        GPT.run(new String[]{graphFile.getAbsolutePath(), "-t", outputFilePath});
        Product product = ProductIO.readProduct(outputFilePath);
        Band band = product.getBand("computed_band1");
        assertNotNull(band);
        assertEquals(5, band.getSampleInt(0, 0));
        Band band2 = product.getBand("computed_band2");
        assertNotNull(band2);
        assertEquals(5, band2.getSampleInt(0, 0));
    }

    private void assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecuteAndComputeTile_oneBand(String operatorName) throws Exception {
        String graph = OPERATOR_WITH_FOLLOW_UP_DO_EXECUTE_AND_COMPUTE_TILE_GRAPH.replace("{operator_name}", operatorName);
        String graphFileName = "OperatorExecutionsTest/" + operatorName + "_racoig_fudoact.xml";
        File graphFile = GlobalTestConfig.getSnapTestDataOutputFile(graphFileName);
        PrintWriter printWriter = new PrintWriter(graphFile);
        printWriter.print(graph);
        printWriter.close();

        String outputFileName = "OperatorExecutionsTest/" + operatorName + "_racoig_fudoact.dim";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();
        GPT.run(new String[]{graphFile.getAbsolutePath(), "-t", outputFilePath});
        Product product = ProductIO.readProduct(outputFilePath);
        Band band = product.getBand("computed_band1");
        assertNotNull(band);
        assertEquals(5, band.getSampleInt(0, 0));
    }

    private void assertRunningAsConnectedOperatorInGraphWorksWithFollowUpDoExecuteAndComputeTile_twoBands(String operatorName) throws Exception {
        String graph = OPERATOR_WITH_FOLLOW_UP_DO_EXECUTE_AND_COMPUTE_TILE_GRAPH.replace("{operator_name}", operatorName);
        String graphFileName = "OperatorExecutionsTest/" + operatorName + "_racoig_fudoact.xml";
        File graphFile = GlobalTestConfig.getSnapTestDataOutputFile(graphFileName);
        PrintWriter printWriter = new PrintWriter(graphFile);
        printWriter.print(graph);
        printWriter.close();

        String outputFileName = "OperatorExecutionsTest/" + operatorName + "_racoig_fudoact.dim";
        File outputFile = GlobalTestConfig.getSnapTestDataOutputFile(outputFileName);
        String outputFilePath = outputFile.getAbsolutePath();
        GPT.run(new String[]{graphFile.getAbsolutePath(), "-t", outputFilePath});
        Product product = ProductIO.readProduct(outputFilePath);
        Band band = product.getBand("computed_band1");
        assertNotNull(band);
        assertEquals(5, band.getSampleInt(0, 0));
        Band band2 = product.getBand("computed_band2");
        assertNotNull(band2);
        assertEquals(5, band2.getSampleInt(0, 0));
    }

    private void assertRunningAsConnectedOperatorInGraphWorksWithBandMaths_twoBands(String operatorName) throws Exception {
        String graph = OPERATOR_WITH_BAND_MATHS_GRAPH_2.replace("{operator_name}", operatorName);
        String graphFileName = "OperatorExecutionsTest/" + operatorName + "_racoig.xml";
        File graphFile = GlobalTestConfig.getSnapTestDataOutputFile(graphFileName);
        PrintWriter printWriter = new PrintWriter(graphFile);
        printWriter.print(graph);
        printWriter.close();

        String outputFileName = "OperatorExecutionsTest/" + operatorName + "_racoig.dim";
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
