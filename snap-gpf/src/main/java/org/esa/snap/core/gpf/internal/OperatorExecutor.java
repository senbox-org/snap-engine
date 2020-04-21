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
package org.esa.snap.core.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorCancelException;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.runtime.Config;

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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * This executor triggers the computation of all tiles that the bands of the
 * target product of the given operator have. The computation of these tiles is
 * parallelized to use all available CPUs (cores) using the JAI
 * {@link TileScheduler}.
 *
 * @author Marco Zuehlke
 * @since BEAM 4.7
 */
public class OperatorExecutor {

    private ImagesProvider imagesProvider;

    public static OperatorExecutor create(Operator op) {
        return new OperatorExecutor(op);
    }

    public enum ExecutionOrder {
        SCHEDULE_ROW_COLUMN_BAND,
        SCHEDULE_ROW_BAND_COLUMN,
        /**
         * Minimize disk seeks if following conditions are met:<br/>
         * 1. Bands can be computed independently of each other<br/>
         * 2. I/O-bound processing (time to compute band pixels will less than
         * time for I/O).<br/>
         */
        SCHEDULE_BAND_ROW_COLUMN,
        /**
         * for debugging purpose
         */
        PULL_ROW_BAND_COLUMN,
    }

    private final TileScheduler tileScheduler;
    private final int parallelism;
    private volatile OperatorException error = null;
    private boolean scheduleRowsSeparate = false;

    private OperatorExecutor(Operator operator) {
        this(new OperatorImagesProvider(operator), JAI.getDefaultInstance().getTileScheduler().getParallelism());
    }

    public OperatorExecutor(PlanarImage[] images, int tileCountX, int tileCountY) {
        this(new SimpleImagesProvider(images, tileCountX, tileCountY),
                JAI.getDefaultInstance().getTileScheduler().getParallelism());
    }

    public OperatorExecutor(PlanarImage[] images, int tileCountX, int tileCountY, int parallelism) {
        this(new SimpleImagesProvider(images, tileCountX, tileCountY), parallelism);
    }

    private OperatorExecutor(ImagesProvider imagesProvider, int parallelism) {
        this.imagesProvider = imagesProvider;
        this.parallelism = parallelism;
        this.tileScheduler = JAI.getDefaultInstance().getTileScheduler();
    }

    public void setScheduleRowsSeparate(boolean scheduleRowsSeparate) {
        this.scheduleRowsSeparate = scheduleRowsSeparate;
    }

    public void execute(ProgressMonitor pm) {
        execute(ExecutionOrder.SCHEDULE_ROW_BAND_COLUMN, pm);
    }

    public void execute(ExecutionOrder executionOrder, ProgressMonitor pm) {
        execute(executionOrder, "Executing operator...", pm);
    }

    public void execute(ExecutionOrder executionOrder, String executionMessage, ProgressMonitor pm) {

        pm.beginTask(executionMessage, 5);
        try {
            imagesProvider.init(SubProgressMonitor.create(pm, 1));
            scheduleBandsComputation(executionOrder, executionMessage, imagesProvider,
                                     SubProgressMonitor.create(pm, 4));
        } finally {
            pm.done();
        }
    }

    private void scheduleBandsComputation(ExecutionOrder executionOrder, String executionMessage, ImagesProvider imagesProvider, ProgressMonitor pm) {
        PlanarImage[] images = imagesProvider.getImages();
        int tileCountX = imagesProvider.getTileCountX();
        int tileCountY = imagesProvider.getTileCountY();

        final Semaphore semaphore = new Semaphore(parallelism, true);
        final TileComputationListener tcl = new OperatorTileComputationListener(semaphore, pm);
        final TileComputationListener[] listeners = new TileComputationListener[]{tcl};

        ExecutionOrder effectiveExecutionOrder = getEffectiveExecutionOrder(executionOrder);

        pm.beginTask(executionMessage, tileCountX * tileCountY * images.length);
        ImagingListener imagingListener = JAI.getDefaultInstance().getImagingListener();
        JAI.getDefaultInstance().setImagingListener(new GPFImagingListener());
        try {
            if (effectiveExecutionOrder == ExecutionOrder.SCHEDULE_ROW_BAND_COLUMN) {
                scheduleRowBandColumn(images, tileCountX, tileCountY, semaphore, listeners, pm);
            } else if (effectiveExecutionOrder == ExecutionOrder.SCHEDULE_ROW_COLUMN_BAND) {
                scheduleRowColumnBand(images, tileCountX, tileCountY, semaphore, pm);
            } else if (effectiveExecutionOrder == ExecutionOrder.SCHEDULE_BAND_ROW_COLUMN) {
                scheduleBandRowColumn(images, tileCountX, tileCountY, semaphore, listeners, pm);
            } else if (effectiveExecutionOrder == ExecutionOrder.PULL_ROW_BAND_COLUMN) {
                executeRowBandColumn(images, tileCountX, tileCountY, pm);
            } else {
                throw new IllegalArgumentException("executionOrder");
            }
            acquirePermits(semaphore, parallelism);
            if (error != null) {
                throw error;
            }
        } finally {
            semaphore.release(parallelism);
            pm.done();
            JAI.getDefaultInstance().setImagingListener(imagingListener);
        }
    }

    private ExecutionOrder getEffectiveExecutionOrder(ExecutionOrder executionOrder) {
        ExecutionOrder effectiveExecutionOrder = executionOrder;
        String executionOrderProperty = Config.instance().preferences().get("snap.gpf.executionOrder", null);
        if (executionOrderProperty != null) {
            effectiveExecutionOrder = ExecutionOrder.valueOf(executionOrderProperty);
        }
        if (effectiveExecutionOrder != executionOrder) {
            SystemUtils.LOG.info(
                    "Changing execution order from " + executionOrder + " to " + effectiveExecutionOrder);
        }
        return effectiveExecutionOrder;
    }

    private void scheduleBandRowColumn(PlanarImage[] images, int tileCountX, int tileCountY, Semaphore semaphore, TileComputationListener[] listeners, ProgressMonitor pm) {
        for (final PlanarImage image : images) {
            for (int tileY = 0; tileY < tileCountY; tileY++) {
                for (int tileX = 0; tileX < tileCountX; tileX++) {
                    scheduleTile(image, tileX, tileY, tileCountX, tileCountY, semaphore, listeners, pm);
                }
                if (scheduleRowsSeparate) {
                    // wait until all threads / tiles are finished
                    acquirePermits(semaphore, parallelism);
                    semaphore.release(parallelism);
                }
            }
        }
    }

    private void scheduleRowBandColumn(PlanarImage[] images, int tileCountX, int tileCountY, Semaphore semaphore, TileComputationListener[] listeners, ProgressMonitor pm) {
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            for (final PlanarImage image : images) {
                for (int tileX = 0; tileX < tileCountX; tileX++) {
                    scheduleTile(image, tileX, tileY, tileCountX, tileCountY, semaphore, listeners, pm);
                }
            }
            if (scheduleRowsSeparate) {
                // wait until all threads / tiles are finished
                acquirePermits(semaphore, parallelism);
                semaphore.release(parallelism);
            }
        }
    }

    private void scheduleRowColumnBand(PlanarImage[] images, int tileCountX, int tileCountY, Semaphore semaphore, ProgressMonitor pm) {
        //better handle stack operators, should equal well work for normal operators
        if (images.length >= 1) {
            final TileComputationListener tcl = new OperatorTileComputationListenerStack(semaphore, images,
                    tileCountX, tileCountY, pm);
            final TileComputationListener[] listeners = new TileComputationListener[]{tcl};
            for (int tileY = 0; tileY < tileCountY; tileY++) {
                for (int tileX = 0; tileX < tileCountX; tileX++) {
                    scheduleTile(images[0], tileX, tileY, tileCountX, tileCountY, semaphore, listeners, pm);
                }
                if (scheduleRowsSeparate) {
                    // wait until all threads / tiles are finished
                    acquirePermits(semaphore, parallelism);
                    semaphore.release(parallelism);
                }
            }
        }
    }

    private void scheduleTile(final PlanarImage image, int tileX, int tileY, int tileCountX, int tileCountY, Semaphore semaphore,
                              TileComputationListener[] listeners, ProgressMonitor pm) {

        SystemUtils.LOG.finest(String.format("Scheduling tile x=%d/%d y=%d/%d for %s",
                                             tileX + 1, tileCountX, tileY + 1, tileCountY, image));

        checkForCancellation(pm);
        acquirePermits(semaphore, 1);
        if (error != null) {
            semaphore.release(parallelism);
            throw error;
        }
        Point[] points = new Point[]{new Point(tileX, tileY)};
        /////////////////////////////////////////////////////////////////////
        //
        // Note: GPF pull-processing is triggered here!!!
        //
        tileScheduler.scheduleTiles(image, points, listeners);
        //
        /////////////////////////////////////////////////////////////////////
    }

    private static void acquirePermits(Semaphore semaphore, int permits) {
        try {
            semaphore.acquire(permits);
        } catch (InterruptedException e) {
            throw new OperatorException(e);
        }
    }

    private static OperatorContext getOperatorContext(Operator operator) {
        try {
            Field field = Operator.class.getDeclaredField("context");
            field.setAccessible(true);
            OperatorContext operatorContext = (OperatorContext) field.get(operator);
            field.setAccessible(false);
            return operatorContext;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void checkForCancellation(ProgressMonitor pm) {
        if (pm.isCanceled()) {
            throw new OperatorCancelException("Operation canceled.");
        }
    }

    // unused (mz) left for debugging purpose
    // does not schedule tile but instead calls getTile blocking
    private void executeRowBandColumn(PlanarImage[] images, int tileCountX, int tileCountY, ProgressMonitor pm) {
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            for (final PlanarImage image : images) {
                for (int tileX = 0; tileX < tileCountX; tileX++) {
                    checkForCancellation(pm);
                    /////////////////////////////////////////////////////////////////////
                    //
                    // Note: GPF pull-processing is triggered here!!!
                    //
                    image.getTile(tileX, tileY);
                    //
                    /////////////////////////////////////////////////////////////////////
                    pm.worked(1);
                }
            }
        }
    }

    private class OperatorTileComputationListenerStack implements TileComputationListener {

        private final Semaphore semaphore;
        private final PlanarImage[] images;
        private final ProgressMonitor pm;
        private final int tileCountX;
        private final int tileCountY;

        OperatorTileComputationListenerStack(Semaphore semaphore, PlanarImage[] images, int tileCountX, int tileCountY,
                                             ProgressMonitor pm) {
            this.semaphore = semaphore;
            this.images = images;
            this.tileCountX = tileCountX;
            this.tileCountY = tileCountY;
            this.pm = pm;
        }

        @Override
        public void tileComputed(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX, int tileY,
                                 Raster raster) {
            for (PlanarImage planarImage : images) {
                if (image != planarImage) {
                    SystemUtils.LOG.finest(String.format("Scheduling tile x=%d/%d y=%d/%d for %s",
                                                         tileX + 1, tileCountX, tileY + 1, tileCountY, planarImage));
                    planarImage.getTile(tileX, tileY);
                }
                pm.worked(1);
            }
            semaphore.release();
        }

        @Override
        public void tileCancelled(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX, int tileY) {
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

    private class OperatorTileComputationListener implements TileComputationListener {

        private final Semaphore semaphore;
        private final ProgressMonitor pm;

        OperatorTileComputationListener(Semaphore semaphore, ProgressMonitor pm) {
            this.semaphore = semaphore;
            this.pm = pm;
        }

        @Override
        public void tileComputed(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX, int tileY,
                                 Raster raster) {
            semaphore.release();
            pm.worked(1);
        }

        @Override
        public void tileCancelled(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX, int tileY) {
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

    interface ImagesProvider {

        void init(ProgressMonitor pm);

        PlanarImage[] getImages();

        int getTileCountX();

        int getTileCountY();

    }

    private static class OperatorImagesProvider implements ImagesProvider {

        private final Operator operator;
        private int tileCountX = 0;
        private int tileCountY = 0;
        private PlanarImage[] images;

        OperatorImagesProvider(Operator operator) {
            this.operator = operator;
        }

        @Override
        public PlanarImage[] getImages() {
            return images;
        }

        @Override
        public int getTileCountX() {
            return tileCountX;
        }

        @Override
        public int getTileCountY() {
            return tileCountY;
        }

        @Override
        public void init(ProgressMonitor pm) {
            OperatorContext operatorContext = getOperatorContext(operator);
            Product targetProduct = operator.getTargetProduct();
            operator.execute(pm);
            // todo - [multisize_products] fix: don't rely on tiling is same for all bands (nf)
            Dimension tileSize = targetProduct.getPreferredTileSize();

            int rasterHeight = targetProduct.getSceneRasterHeight();
            int rasterWidth = targetProduct.getSceneRasterWidth();
            Rectangle boundary = new Rectangle(rasterWidth, rasterHeight);
            tileCountX = MathUtils.ceilInt(boundary.width / (double) tileSize.width);
            tileCountY = MathUtils.ceilInt(boundary.height / (double) tileSize.height);
            Band[] targetBands = targetProduct.getBands();
            images = createImages(targetBands, operatorContext);
        }

        private static PlanarImage[] createImages(Band[] targetBands, OperatorContext operatorContext) {
            final ArrayList<PlanarImage> images = new ArrayList<>(targetBands.length);
            for (final Band band : targetBands) {
                OperatorImage operatorImage = operatorContext.getTargetImage(band);
                if (operatorImage != null) {
                    images.add(operatorImage);
                }
            }
            return images.toArray(new PlanarImage[0]);
        }

    }

    private static class SimpleImagesProvider implements ImagesProvider {

        private final PlanarImage[] images;
        private final int tileCountX;
        private final int tileCountY;

        SimpleImagesProvider(PlanarImage[] images, int tileCountX, int tileCountY) {
            this.images = images;
            this.tileCountX = tileCountX;
            this.tileCountY = tileCountY;
        }

        @Override
        public void init(ProgressMonitor pm) {
        }

        @Override
        public PlanarImage[] getImages() {
            return images;
        }

        @Override
        public int getTileCountX() {
            return tileCountX;
        }

        @Override
        public int getTileCountY() {
            return tileCountY;
        }
    }

}
