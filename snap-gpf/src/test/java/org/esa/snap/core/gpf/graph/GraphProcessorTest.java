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

import com.bc.ceres.annotation.STTM;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.TestOps;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.jai.VerboseTileCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.JAI;
import javax.media.jai.TileCache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class GraphProcessorTest {
    private OperatorSpi spi1;
    private OperatorSpi spi2;
    private OperatorSpi spi3;
    private TileCache jaiTileCache;
    private TileCache testTileCache;
    private boolean defVerbosity;

    @Before
    public void setUp() throws Exception {
        jaiTileCache = JAI.getDefaultInstance().getTileCache();
        defVerbosity = VerboseTileCache.isVerbose();
        VerboseTileCache.setVerbose(false);
        testTileCache = new VerboseTileCache(jaiTileCache);
        JAI.getDefaultInstance().setTileCache(testTileCache);
        testTileCache.flush();

        TestOps.clearCalls();
        spi1 = new TestOps.Op1.Spi();
        spi2 = new TestOps.Op2.Spi();
        spi3 = new TestOps.Op3.Spi();
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.addOperatorSpi(spi1);
        registry.addOperatorSpi(spi2);
        registry.addOperatorSpi(spi3);
    }

    @After
    public void tearDown() throws Exception {
        testTileCache.flush();
        VerboseTileCache.setVerbose(defVerbosity);
        JAI.getDefaultInstance().setTileCache(jaiTileCache);
        final OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.removeOperatorSpi(spi1);
        spiRegistry.removeOperatorSpi(spi2);
        spiRegistry.removeOperatorSpi(spi3);
    }


    @SuppressWarnings("null")
    @Test
    public void testTwoOpsExecutionOrder() throws Exception {
        GraphProcessor processor = new GraphProcessor();

        Graph graph = new Graph("chain1");

        Node node1 = new Node("node1", "Op1");
        Node node2 = new Node("node2", "Op2");

        node2.addSource(new NodeSource("input", "node1"));

        graph.addNode(node1);
        graph.addNode(node2);

        GraphContext graphContext = new GraphContext(graph);
        Product chainOut = processor.executeGraph(graphContext, ProgressMonitor.NULL)[0];

        assertNotNull(chainOut);
        assertEquals("Op2Name", chainOut.getName());

        assertEquals("Op1;Op2;", TestOps.getCalls());
        TestOps.clearCalls();
    }

    ////////////////////////////////////////////////////////////////////////
    //            node1
    //            /  \
    //        node2   \
    //            \   /
    //            node3    <-- Target!
    //
    @Test
    public void testThreeOpsExecutionOrder() throws Exception {
        Graph graph = new Graph("graph");

        Node node1 = new Node("node1", "Op1");
        Node node2 = new Node("node2", "Op2");
        Node node3 = new Node("node3", "Op3");

        node2.addSource(new NodeSource("input", "node1"));
        node3.addSource(new NodeSource("input1", "node1"));
        node3.addSource(new NodeSource("input2", "node2"));
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = new GraphContext(graph);
        Product chainOut = graphContext.getOutputProducts()[0];

        assertNotNull(chainOut);
        assertEquals("Op3Name", chainOut.getName());

        processor.executeGraph(graphContext, ProgressMonitor.NULL);
        // - Op3 requires the two bands of Op2
        // - Op2 computes all bands
        // --> Op2 should only be called once
        assertEquals("Op1;Op2;Op3;", TestOps.getCalls());
        TestOps.clearCalls();
    }

    @Test
    @STTM("SNAP-4391")
    public void graphContextIsDisposedWhenTheGraphFails() throws Exception {
        final OperatorSpi failingSpi = new FailingOp.Spi();
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.addOperatorSpi(failingSpi);
        FailingOp.disposed = 0;
        try {
            final Graph graph = new Graph("failing");
            final Node node1 = new Node("node1", "Op1");
            final Node node2 = new Node("node2", "GraphProcessorTestFailingOp");
            node2.addSource(new NodeSource("input", "node1"));
            graph.addNode(node1);
            graph.addNode(node2);

            final RuntimeException e = assertThrows(RuntimeException.class,
                                                    () -> new GraphProcessor().executeGraph(graph, ProgressMonitor.NULL));
            assertTrue(String.valueOf(e), String.valueOf(e).contains("simulated failure"));
            assertEquals("the failed graph's operators must be disposed", 1, FailingOp.disposed);
        } finally {
            registry.removeOperatorSpi(failingSpi);
        }
    }

    @Test
    @STTM("SNAP-4391")
    public void graphContextIsDisposedWhenTheGraphSucceeds() throws Exception {
        final Graph graph = new Graph("chain");
        final Node node1 = new Node("node1", "Op1");
        final Node node2 = new Node("node2", "Op2");
        node2.addSource(new NodeSource("input", "node1"));
        graph.addNode(node1);
        graph.addNode(node2);

        new GraphProcessor().executeGraph(graph, ProgressMonitor.NULL);
        assertEquals("Op1;Op2;", TestOps.getCalls());
        TestOps.clearCalls();
    }

    @OperatorMetadata(alias = "GraphProcessorTestFailingOp")
    public static class FailingOp extends Operator {
        static int disposed;

        @SourceProduct
        public Product input;

        @TargetProduct
        public Product output;

        @Override
        public void initialize() {
            output = new Product("FailingName", "FailingType", TestOps.RASTER_WIDTH, TestOps.RASTER_HEIGHT);
            output.addBand(new Band("F", ProductData.TYPE_INT8, TestOps.RASTER_WIDTH, TestOps.RASTER_HEIGHT));
        }

        @Override
        public void doExecute(ProgressMonitor pm) {
            throw new OperatorException("simulated failure");
        }

        @Override
        public void dispose() {
            disposed++;
            super.dispose();
        }

        public static class Spi extends OperatorSpi {
            public Spi() {
                super(FailingOp.class);
            }
        }
    }
}
