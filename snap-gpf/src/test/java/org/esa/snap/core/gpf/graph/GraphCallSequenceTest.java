/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.jai.JAI;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphCallSequenceTest {

    private static final List<String> callRecordList = Collections.synchronizedList(new ArrayList<>());
    private static N1Spi n1Spi;
    private static N2Spi n2Spi;
    private static N3Spi n3Spi;
    private static N4Spi n4Spi;
    private static N5Spi n5Spi;
    private static N6Spi n6Spi;

    @BeforeClass
    public static void setUpTest() {
        n1Spi = new N1Spi();
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.addOperatorSpi(n1Spi);
        n2Spi = new N2Spi();
        registry.addOperatorSpi(n2Spi);
        n3Spi = new N3Spi();
        registry.addOperatorSpi(n3Spi);
        n4Spi = new N4Spi();
        registry.addOperatorSpi(n4Spi);
        n5Spi = new N5Spi();
        registry.addOperatorSpi(n5Spi);
        n6Spi = new N6Spi();
        registry.addOperatorSpi(n6Spi);
    }

    @AfterClass
    public static void tearDownTest() {
        final OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.removeOperatorSpi(n1Spi);
        spiRegistry.removeOperatorSpi(n2Spi);
        spiRegistry.removeOperatorSpi(n3Spi);
        spiRegistry.removeOperatorSpi(n4Spi);
        spiRegistry.removeOperatorSpi(n5Spi);
        spiRegistry.removeOperatorSpi(n6Spi);
    }

    @Before
    public void setUp() throws Exception {
        callRecordList.clear();
        JAI.getDefaultInstance().getTileCache().flush();
    }

    @After
    public void tearDown() throws Exception {
        JAI.getDefaultInstance().getTileCache().flush();
        callRecordList.clear();
    }

    ////////////////////////////////////////////////////////////////////////
    //
    //                   N1
    //                  /
    //                N2
    //
    //
    @Test
    public void testTwoNodeTraversion() throws GraphException {

        Node node1 = new Node("N1", "N1");
        Node node2 = new Node("N2", "N2");

        node2.addSource(new NodeSource("input", "N1"));

        Graph graph = new Graph("test-graph");
        graph.addNode(node1);
        graph.addNode(node2);

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = new GraphContext(graph);
        Product[] targetProducts = processor.executeGraph(graphContext, ProgressMonitor.NULL);

        Assert.assertNotNull(targetProducts);
        Assert.assertEquals(1, targetProducts.length);
        Assert.assertNotNull(targetProducts[0]);
        Assert.assertEquals("N2", targetProducts[0].getName());

        graphContext.dispose();

        String[] expectedRecordStrings = new String[]{
                "N1:Operator.initialize",
                "N1:Product.construct",
                "N2:Operator.initialize",
                "N2:Product.construct",
                "N2:Operator.computeBand",
                "N1:Operator.computeBand",
                "N2:Product.dispose",
                "N2:Operator.dispose",
                "N1:Product.dispose",
                "N1:Operator.dispose",
        };

        Assert.assertEquals(expectedRecordStrings.length, callRecordList.size());

        for (int i = 0; i < expectedRecordStrings.length; i++) {
            Assert.assertEquals(expectedRecordStrings[i], callRecordList.get(i));
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //
    //                   N1
    //                  /
    //                N2
    //               /
    //              N3
    //
    //
    @Test
    public void testThreeNodeTraversion() throws GraphException {
        Node node1 = new Node("N1", "N1");
        Node node2 = new Node("N2", "N2");
        Node node3 = new Node("N3", "N3");

        node2.addSource(new NodeSource("input", "N1"));
        node3.addSource(new NodeSource("input", "N2"));

        Graph graph = new Graph("test-graph");
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = new GraphContext(graph);
        Product[] targetProducts = processor.executeGraph(graphContext, ProgressMonitor.NULL);

        Assert.assertNotNull(targetProducts);
        Assert.assertEquals(1, targetProducts.length);
        Assert.assertNotNull(targetProducts[0]);
        Assert.assertEquals("N3", targetProducts[0].getName());

        graphContext.dispose();

        String[] expectedRecordStrings = new String[]{
                "N1:Operator.initialize",
                "N1:Product.construct",
                "N2:Operator.initialize",
                "N2:Product.construct",
                "N3:Operator.initialize",
                "N3:Product.construct",
                "N3:Operator.computeBand",
                "N2:Operator.computeBand",
                "N1:Operator.computeBand",
                "N3:Product.dispose",
                "N3:Operator.dispose",
                "N2:Product.dispose",
                "N2:Operator.dispose",
                "N1:Product.dispose",
                "N1:Operator.dispose",
        };

        Assert.assertEquals(expectedRecordStrings.length, callRecordList.size());

        for (int i = 0; i < expectedRecordStrings.length; i++) {
            Assert.assertEquals(expectedRecordStrings[i], callRecordList.get(i));
        }
    }


    ////////////////////////////////////////////////////////////////////////
    //
    //                   N1
    //                  /  \
    //                 N2   N3
    //               /  \
    //              N4   N5
    //
    //
    @Test
    public void testSingleSources3Ouputs() throws GraphException {
        Node node1 = new Node("N1", "N1");
        Node node2 = new Node("N2", "N2");
        Node node3 = new Node("N3", "N3");
        Node node4 = new Node("N4", "N4");
        Node node5 = new Node("N5", "N5");

        node2.addSource(new NodeSource("input", "N1"));
        node3.addSource(new NodeSource("input", "N1"));
        node4.addSource(new NodeSource("input", "N2"));
        node5.addSource(new NodeSource("input", "N2"));

        Graph graph = new Graph("test-graph");
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);
        graph.addNode(node4);
        graph.addNode(node5);

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = new GraphContext(graph);
        Product[] outputProducts = processor.executeGraph(graphContext, ProgressMonitor.NULL);

        Assert.assertNotNull(outputProducts);
        Assert.assertEquals(3, outputProducts.length);
        Assert.assertNotNull(outputProducts[0]);
        Assert.assertNotNull(outputProducts[1]);
        Assert.assertNotNull(outputProducts[2]);
        // Nodes shall be processed in the order they are defined!
        Assert.assertEquals("N3", outputProducts[0].getName());
        Assert.assertEquals("N4", outputProducts[1].getName());
        Assert.assertEquals("N5", outputProducts[2].getName());

        graphContext.dispose();

        String[] expectedRecords = new String[]{
                "N1:Operator.initialize",
                "N1:Product.construct",
                "N3:Operator.initialize",
                "N3:Product.construct",
                "N2:Operator.initialize",
                "N2:Product.construct",
                "N4:Operator.initialize",
                "N4:Product.construct",
                "N5:Operator.initialize",
                "N5:Product.construct",
                "N3:Operator.computeBand",
                "N1:Operator.computeBand",
                "N4:Operator.computeBand",
                "N2:Operator.computeBand",
                //"N1:Operator.computeBand",  is cached!
                "N5:Operator.computeBand",
                //"N2:Operator.computeBand",  is cached!
                //"N1:Operator.computeBand",  is cached!
                "N5:Operator.dispose",
                "N5:Product.dispose",
                "N4:Operator.dispose",
                "N4:Product.dispose",
                "N2:Operator.dispose",
                "N2:Product.dispose",
                "N3:Operator.dispose",
                "N3:Product.dispose",
                "N1:Operator.dispose",
                "N1:Product.dispose",
        };

        for (String expectedRecord : expectedRecords) {
//            System.out.println("callRecordList = " + callRecordList.get(i).toString());
            boolean contains = callRecordList.contains(expectedRecord);
            Assert.assertTrue("Graph must call " + expectedRecord, contains);
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //
    //                   N1
    //                  /  \
    //                 N2   N3
    //               /  \  /
    //              N4   N6      <-- N6 has 2 sources!
    //
    //
    @Test
    public void test2Sources1Ouput() throws GraphException {
        Node node1 = new Node("N1", "N1");
        Node node2 = new Node("N2", "N2");
        Node node3 = new Node("N3", "N3");
        Node node4 = new Node("N4", "N4");
        Node node6 = new Node("N6", "N6");

        node2.addSource(new NodeSource("input", "N1"));
        node3.addSource(new NodeSource("input", "N1"));
        node4.addSource(new NodeSource("input", "N2"));
        node6.addSource(new NodeSource("input1", "N2"));
        node6.addSource(new NodeSource("input2", "N3"));

        Graph graph = new Graph("test-graph");
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);
        graph.addNode(node4);
        graph.addNode(node6);

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = new GraphContext(graph);
        Product[] outputProducts = processor.executeGraph(graphContext, ProgressMonitor.NULL);

        Assert.assertNotNull(outputProducts);
        Assert.assertEquals(2, outputProducts.length);
        Assert.assertNotNull(outputProducts[0]);
        Assert.assertNotNull(outputProducts[1]);
        // Nodes shall be processed in the order they are defined!
        Assert.assertEquals("N4", outputProducts[0].getName());
        Assert.assertEquals("N6", outputProducts[1].getName());

        graphContext.dispose();

        String[] expectedRecords = new String[]{
                "N1:Operator.initialize",
                "N1:Product.construct",
                "N2:Operator.initialize",
                "N2:Product.construct",
                "N4:Operator.initialize",
                "N4:Product.construct",
                "N3:Operator.initialize",
                "N3:Product.construct",
                "N6:Operator.initialize",
                "N6:Product.construct",
                "N4:Operator.computeBand",
                "N2:Operator.computeBand",
                "N1:Operator.computeBand",
                "N6:Operator.computeBand",
                // "N2:Operator.computeBand",  is cached!
                // "N1:Operator.computeBand",  is cached!
                "N3:Operator.computeBand",
                // "N1:Operator.computeBand",  is cached!
                "N6:Operator.dispose",
                "N6:Product.dispose",
                "N3:Operator.dispose",
                "N3:Product.dispose",
                "N4:Operator.dispose",
                "N4:Product.dispose",
                "N2:Operator.dispose",
                "N2:Product.dispose",
                "N1:Operator.dispose",
                "N1:Product.dispose",
        };

        for (String expectedRecord : expectedRecords) {
//          System.out.println("callRecordList = " + callRecordList.get(i).toString());
            boolean contains = callRecordList.contains(expectedRecord);
            Assert.assertTrue("Graph must call " + expectedRecord, contains);
        }
    }

    private static String getOpName(RecordingOp recordingOp) {
        return recordingOp.getSpi().getOperatorAlias();
    }

    public static class RecordingOp extends Operator {


        @Override
        public void initialize() throws OperatorException {
            recordCall(getOpName(this), "Operator.initialize");
            setTargetProduct(new RecordingProduct(this));
        }

        @Override
        public void dispose() {
            recordCall(getOpName(this), "Operator.dispose");
            super.dispose();
        }
    }

    public static class NoSourceOp extends RecordingOp {

        @TargetProduct
        private Product targetProduct;

        // todo - add tests that verify correct computing output
        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws
                OperatorException {
            recordCall(getOpName(this), "Operator.computeBand");

            Rectangle r = targetTile.getRectangle();
            float offset = r.y * targetProduct.getSceneRasterWidth() + r.x;

            ProductData rawSampleData = targetTile.getRawSamples();
            float[] targetElems = (float[]) rawSampleData.getElems();
            for (int i = 0; i < targetElems.length; i++) {
                targetElems[i] = offset + i;
            }
            rawSampleData.setElems(targetElems);
            targetTile.setRawSamples(rawSampleData);

        }

        @Override
        public void dispose() {
            recordCall(getOpName(this), "Operator.dispose");
        }
    }

    public static class SingleSourceOp extends NoSourceOp {

        @SourceProduct(alias = "input")
        private Product sourceProduct;
        @TargetProduct
        private Product targetProduct;

        // todo - add tests that verify correct computing output
        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws
                OperatorException {
            recordCall(getOpName(this), "Operator.computeBand");

            Tile sourceTile = getSourceTile(sourceProduct.getBandAt(0),
                                            targetTile.getRectangle());

            float[] sourceElems = (float[]) sourceTile.getRawSamples().getElems();
            ProductData rawSampleData = targetTile.getRawSamples();
            float[] targetElems = (float[]) rawSampleData.getElems();
            for (int i = 0; i < targetElems.length; i++) {
                targetElems[i] = 0.1f * sourceElems[i];
            }
            rawSampleData.setElems(targetElems);
            targetTile.setRawSamples(rawSampleData);
        }
    }

    public static class DualSourceOp extends NoSourceOp {

        @SourceProduct(alias = "input1")
        private Product sourceProduct1;
        @SourceProduct(alias = "input2")
        private Product sourceProduct2;
        @TargetProduct
        private Product targetProduct;

        // todo - add tests that verify correct computing output
        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
            recordCall(getOpName(this), "Operator.computeBand");

            Tile sourceTile1 = getSourceTile(sourceProduct1.getBandAt(0),
                                             targetTile.getRectangle());

            Tile sourceTile2 = getSourceTile(sourceProduct2.getBandAt(0),
                                             targetTile.getRectangle());

            float[] source1Elems = (float[]) sourceTile1.getRawSamples().getElems();
            float[] source2Elems = (float[]) sourceTile2.getRawSamples().getElems();
            ProductData rawSampleData = targetTile.getRawSamples();
            float[] targetElems = (float[]) rawSampleData.getElems();
            for (int i = 0; i < targetElems.length; i++) {
                targetElems[i] = 0.1f * (source1Elems[i] + source2Elems[i]);
            }
            rawSampleData.setElems(targetElems);
            targetTile.setRawSamples(rawSampleData);
        }
    }

    public static abstract class NSpi extends OperatorSpi {

        protected NSpi(Class<? extends Operator> operatorClass, String name) {
            super(operatorClass, name);
        }
    }

    public static class N1Spi extends NSpi {

        public N1Spi() {
            super(NoSourceOp.class, "N1");
        }
    }

    public static class N2Spi extends NSpi {

        public N2Spi() {
            super(SingleSourceOp.class, "N2");
        }
    }

    public static class N3Spi extends NSpi {

        public N3Spi() {
            super(SingleSourceOp.class, "N3");
        }
    }

    public static class N4Spi extends NSpi {

        public N4Spi() {
            super(SingleSourceOp.class, "N4");
        }

    }

    public static class N5Spi extends NSpi {

        public N5Spi() {
            super(SingleSourceOp.class, "N5");
        }
    }

    public static class N6Spi extends NSpi {

        public N6Spi() {
            super(DualSourceOp.class, "N6");
        }
    }

    public static class RecordingProduct extends Product {

        public RecordingProduct(RecordingOp op) {
            super(op.getSpi().getOperatorAlias(), op.getClass().getSimpleName(), 1, 1);
            addBand("band_0", ProductData.TYPE_FLOAT32);
            recordCall(getName(), "Product.construct");
        }

        @Override
        public void dispose() {
            recordCall(getName(), "Product.dispose");
            super.dispose();
        }
    }

    private static void recordCall(String product, String method) {
        callRecordList.add(product + ":" + method);
    }
}
