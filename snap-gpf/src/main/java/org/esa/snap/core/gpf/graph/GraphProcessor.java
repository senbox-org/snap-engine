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
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.internal.OperatorContext;
import org.esa.snap.core.gpf.internal.ProductSetHandler;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.jai.JAIUtils;
import org.esa.snap.core.util.math.MathUtils;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import javax.media.jai.TileScheduler;
import javax.media.jai.util.ImagingListener;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

/**
 * The {@code GraphProcessor} is responsible for executing processing
 * graphs.
 *
 * @author Maximilian Aulinger
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Marco Zuehlke
 * @since 4.1
 */
public class GraphProcessor {

    private final List<GraphProcessingObserver> observerList;
    private Logger logger;
    private volatile OperatorException error = null;


    /**
     * Creates a new instance og {@code GraphProcessor}.
     */
    public GraphProcessor() {
        observerList = new ArrayList<>(3);
        logger = SystemUtils.LOG;
    }

    /**
     * Gets the logger.
     *
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Sets a logger.
     *
     * @param logger a logger
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Adds an observer to this graph processor. {@link GraphProcessingObserver}s are informed about
     * processing steps of the currently running processing graph.
     *
     * @param processingObserver the observer
     * @see GraphProcessingObserver
     */
    public void addObserver(GraphProcessingObserver processingObserver) {
        observerList.add(processingObserver);
    }

    /**
     * Gets all observers currently attached to this {@code GraphProcessor}.
     *
     * @return the observers
     */
    public GraphProcessingObserver[] getObservers() {
        return observerList.toArray(new GraphProcessingObserver[0]);
    }

    /**
     * Executes the graph using a new default {@link GraphContext}.
     *
     * @param graph the {@link Graph}
     * @param pm    a progress monitor. Can be used to signal progress.
     * @throws GraphException if any error occurs during execution
     * @see GraphProcessor#executeGraph(GraphContext, ProgressMonitor)
     */
    public void executeGraph(Graph graph, ProgressMonitor pm) throws GraphException {
        GraphContext graphContext;
        try {
            pm.beginTask("Executing processing graph", 100);

            // handle product sets
            final ProductSetHandler productSet = new ProductSetHandler(graph);
            productSet.replaceProductSetsWithReaders();

            graphContext = new GraphContext(graph);
            executeGraph(graphContext, SubProgressMonitor.create(pm, 90));
            graphContext.dispose();
        } finally {
            pm.done();
        }
    }

    private void executeNodeSources(NodeSource[] sources, GraphContext graphContext, ProgressMonitor pm) {
        for (NodeSource source : sources) {
            Node node = source.getSourceNode();
            if (node != null) {
                executeNodeSources(node.getSources(), graphContext, pm);
                NodeContext nodeContext = graphContext.getNodeContext(node);
                if (nodeContext != null) {
                    nodeContext.getOperator().execute(SubProgressMonitor.create(pm, 1));
                }
            }
        }
    }

    /**
     * Executes the graph given by {@link GraphContext}.
     *
     * @param graphContext the {@link GraphContext} to execute
     * @param pm           a progress monitor. Can be used to signal progress.
     * @return the output products of the executed graph
     */
    public Product[] executeGraph(GraphContext graphContext, ProgressMonitor pm) {
        fireProcessingStarted(graphContext);

        NodeContext[] outputNodeContexts = graphContext.getOutputNodeContexts();

        Dimension maxTileLayout = getMaxTileLayout(outputNodeContexts);

        JAI jaiInstance = JAI.getDefaultInstance();
        ImagingListener imagingListenerBackup = jaiInstance.getImagingListener();
        jaiInstance.setImagingListener(new GPFImagingListener());

        final TileScheduler tileScheduler = jaiInstance.getTileScheduler();
        final int parallelism = tileScheduler.getParallelism();
        final Semaphore semaphore = new Semaphore(parallelism, true);
        final TileComputationListener[] tilelisteners = new TileComputationListener[]{new GraphTileComputationListener(semaphore, parallelism)};

        boolean canComputeTileStack = isComputeTileStackUsable(graphContext);

        try {
            final int outputNodeCount = outputNodeContexts.length;
            int numPmTicks = (outputNodeCount * maxTileLayout.width * maxTileLayout.height) + outputNodeCount;
            pm.beginTask("Executing operators...", numPmTicks);
            for (NodeContext outputNodeContext : outputNodeContexts) {
                NodeSource[] sources = outputNodeContext.getNode().getSources();
                executeNodeSources(sources, graphContext, ProgressMonitor.NULL);
                outputNodeContext.getOperator().execute(ProgressMonitor.NULL);
                pm.worked(1);
            }
            pm.setTaskName("Computing raster data...");

            if (canComputeTileStack) {
                for (int tileY = 0; tileY < maxTileLayout.height; tileY++) {
                    for (int tileX = 0; tileX < maxTileLayout.width; tileX++) {
                        if (pm.isCanceled()) {
                            return graphContext.getOutputProducts();
                        }

                        for (NodeContext nodeContext : outputNodeContexts) {
                            Product targetProduct = nodeContext.getTargetProduct();
                            final Dimension tileSize = targetProduct.getPreferredTileSize();
                            Rectangle tileRectangle = new Rectangle(tileX * tileSize.width,
                                                                    tileY * tileSize.height,
                                                                    tileSize.width,
                                                                    tileSize.height);

                            fireTileStarted(graphContext, tileRectangle);

                            // (1) Pull tile from first OperatorImage we find. This will trigger pulling
                            // tiles of all other OperatorImage computed stack-wise.
                            //
                            for (Band band : targetProduct.getBands()) {
                                final int maxBandTilesX = MathUtils.ceilInt(band.getRasterWidth() / tileSize.getWidth());
                                final int maxBandTilesY = MathUtils.ceilInt(band.getRasterHeight() / tileSize.getHeight());
                                if (tileX > maxBandTilesX || tileY > maxBandTilesY) {
                                    // tileIndex is not inside image, probably due to multi-size nature
                                    // This image is smaller than the product size
                                    continue;
                                }

                                PlanarImage image = nodeContext.getTargetImage(band);
                                if (image != null) {
                                    orderTile(image, tileX, tileY, semaphore, tileScheduler, tilelisteners,
                                              parallelism);

                                    break;
                                }
                            }

                            // (2) Pull tile from source images of other regular bands.
                            //
                            for (Band band : targetProduct.getBands()) {
                                PlanarImage image = nodeContext.getTargetImage(band);
                                if (image == null) {
                                    if (OperatorContext.isRegularBand(band) && band.isSourceImageSet()) {
                                        orderTile(band.getSourceImage(), tileX, tileY, semaphore,
                                                  tileScheduler, tilelisteners, parallelism);
                                    }
                                }
                            }
                            fireTileStopped(graphContext, tileRectangle);
                        }
                        pm.worked(1);
                    }
                }
            } else {
                for (NodeContext nodeContext : outputNodeContexts) {
                    Product targetProduct = nodeContext.getTargetProduct();
                    final Dimension tileSize = targetProduct.getPreferredTileSize();
                    boolean monitorProgress = true;
                    for (Band band : targetProduct.getBands()) {
                        PlanarImage image = nodeContext.getTargetImage(band);
                        final int maxBandTilesX = MathUtils.ceilInt(band.getRasterWidth() / tileSize.getWidth());
                        final int maxBandTilesY = MathUtils.ceilInt(band.getRasterHeight() / tileSize.getHeight());
                        for (int tileY = 0; tileY < maxTileLayout.height; tileY++) {
                            for (int tileX = 0; tileX < maxTileLayout.width; tileX++) {
                                if (pm.isCanceled()) {
                                    return graphContext.getOutputProducts();
                                }

                                Rectangle tileRectangle = new Rectangle(tileX * tileSize.width,
                                                                        tileY * tileSize.height,
                                                                        tileSize.width,
                                                                        tileSize.height);

                                if (tileX > maxBandTilesX || tileY > maxBandTilesY) {
                                    // tileIndex is not inside image, probably due to multi-size nature
                                    // This image is smaller than the product size
                                    continue;
                                }
                                fireTileStarted(graphContext, tileRectangle);

                                // Simply pull tile from source images of regular bands.
                                //
                                if (image != null) {
                                    orderTile(image, tileX, tileY, semaphore, tileScheduler, tilelisteners,
                                              parallelism);
                                } else if (OperatorContext.isRegularBand(band) && band.isSourceImageSet()) {
                                    orderTile(band.getSourceImage(), tileX, tileY, semaphore,
                                              tileScheduler, tilelisteners, parallelism);
                                }
                                fireTileStopped(graphContext, tileRectangle);

                                if (monitorProgress) {
                                    pm.worked(1);
                                    // as a consequence of inverting the loop, progressMonitor ticks must only be increased
                                    // once per product processed. This crude boolean logic ensures that. Nevertheless,
                                    // this class needs refactoring! tb 2021-05-21
                                    monitorProgress = false;
                                }
                            }
                        }
                    }
                }
            }

            acquirePermits(semaphore, parallelism);

            if (error != null) {
                throw error;
            }
        } finally {
            semaphore.release(parallelism);
            pm.done();
            JAI.getDefaultInstance().setImagingListener(imagingListenerBackup);
            fireProcessingStopped(graphContext);
        }

        return graphContext.getOutputProducts();
    }

    private boolean isComputeTileStackUsable(GraphContext graphContext) {
        // loop over all nodes and check if one of them computes tile-stack. If so, we do stack-processing. tb 2020-02-07
        boolean canComputeTileStack = false;
        final Deque<NodeContext> nodeContexts = graphContext.getInitNodeContextDeque();
        for (NodeContext nodeContext : nodeContexts) {
            if (!nodeContext.isOutput()) {
                canComputeTileStack |= nodeContext.canComputeTileStack();
            }
        }
        return canComputeTileStack;
    }

    private Dimension getMaxTileLayout(NodeContext[] outputNodeContexts) {
        int numXTiles = -1;
        int numYTiles = -1;
        for (NodeContext outputNodeContext : outputNodeContexts) {
            // it is okay to do it on a product basis
            // the product has the greatest scene width and height
            Product targetProduct = outputNodeContext.getTargetProduct();
            Dimension tileSize = targetProduct.getPreferredTileSize();
            if (tileSize == null) {
                tileSize = JAIUtils.computePreferredTileSize(targetProduct.getSceneRasterWidth(),
                                                             targetProduct.getSceneRasterHeight(), 4);
                targetProduct.setPreferredTileSize(tileSize);
            }
            final Dimension sceneSize = targetProduct.getSceneRasterSize();
            numXTiles = Math.max(numXTiles, MathUtils.ceilInt(sceneSize.getWidth() / tileSize.getWidth()));
            numYTiles = Math.max(numYTiles, MathUtils.ceilInt(sceneSize.getHeight() / tileSize.getHeight()));
        }
        return new Dimension(numXTiles, numYTiles);
    }

    private void orderTile(PlanarImage image, int tileX, int tileY, Semaphore semaphore,
                           TileScheduler tileScheduler, TileComputationListener[] listeners,
                           int parallelism) {
        acquirePermit(semaphore);
        if (error != null) {
            semaphore.release(parallelism);
            throw error;
        }
        /////////////////////////////////////////////////////////////////////
        //
        // Note: GPF pull-processing is triggered here!!!
        //
        Point[] points = new Point[]{new Point(tileX, tileY)};
        tileScheduler.scheduleTiles(image, points, listeners);

        //
        /////////////////////////////////////////////////////////////////////
    }

    private static void acquirePermit(Semaphore semaphore) {
        acquirePermits(semaphore, 1);
    }

    private static void acquirePermits(Semaphore semaphore, int permits) {
        try {
            semaphore.acquire(permits);
        } catch (InterruptedException e) {
            throw new OperatorException(e);
        }
    }


    private void fireProcessingStarted(GraphContext graphContext) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.graphProcessingStarted(graphContext);
        }
    }

    private void fireProcessingStopped(GraphContext graphContext) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.graphProcessingStopped(graphContext);
        }
    }

    private void fireTileStarted(GraphContext graphContext, Rectangle rect) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.tileProcessingStarted(graphContext, rect);
        }

    }

    private void fireTileStopped(GraphContext graphContext, Rectangle rect) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.tileProcessingStopped(graphContext, rect);
        }
    }

    private class GraphTileComputationListener implements TileComputationListener {

        private final Semaphore semaphore;
        private final int parallelism;

        GraphTileComputationListener(Semaphore semaphore, int parallelism) {
            this.semaphore = semaphore;
            this.parallelism = parallelism;
        }

        @Override
        public void tileComputed(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                 int tileY,
                                 Raster raster) {
            semaphore.release();
        }

        @Override
        public void tileCancelled(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                  int tileY) {
            if (error == null) {
                error = new OperatorException("Operation cancelled.");
            }
            semaphore.release(parallelism);
        }

        @Override
        public void tileComputationFailure(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                           int tileY, Throwable situation) {
            if (error == null) {
                error = new OperatorException("Operation failed.", situation);
            }
            semaphore.release(parallelism);
        }
    }

    private class GPFImagingListener implements ImagingListener {

        @Override
        public boolean errorOccurred(String message, Throwable thrown, Object where, boolean isRetryable)
                throws RuntimeException {
            if (error == null && !thrown.getClass().getSimpleName().equals("MediaLibLoadException")) {
                error = new OperatorException(thrown);
            }
            return false;
        }
    }

}
