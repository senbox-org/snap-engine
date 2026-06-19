package org.esa.snap.dem.dataio.copernicus;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.dataop.dem.ElevationFile;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class CopernicusElevationModelConcurrencyTest {


    @Test
    @STTM("SNAP-4213")
    public void getElevationDoesNotSerializeIndependentTileReadsOnModelMonitor() throws Exception {
        BlockingDescriptor descriptor = new BlockingDescriptor();
        CopernicusElevationModel model = new BlockingCopernicusElevationModel(descriptor);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Double> westTileRead = executorService.submit(() -> model.getElevation(new GeoPos(89.75, -179.75)));
        Future<Double> eastTileRead = executorService.submit(() -> model.getElevation(new GeoPos(89.75, -178.75)));

        try {
            assertTrue("Both independent tile reads must reach their tile before either sample is released.",
                       descriptor.awaitBothTileReads(2, TimeUnit.SECONDS));
            descriptor.releaseTileReads();

            assertEquals(10.0, westTileRead.get(2, TimeUnit.SECONDS), 0.0);
            assertEquals(20.0, eastTileRead.get(2, TimeUnit.SECONDS), 0.0);
        } finally {
            descriptor.releaseTileReads();
            executorService.shutdownNow();
        }
    }

    private static final class BlockingCopernicusElevationModel extends CopernicusElevationModel {

        private BlockingCopernicusElevationModel(BlockingDescriptor descriptor) {
            super(descriptor, Resampling.NEAREST_NEIGHBOUR);
        }

        @Override
        protected ElevationFile createElevationFile(CopernicusElevationModel model, File localFile, ProductReader reader) {
            BlockingDescriptor descriptor = (BlockingDescriptor) getDescriptor();
            return new BlockingElevationFile(model, localFile, descriptor.createTile());
        }

        @Override
        protected int getResolution() {
            return 90;
        }
    }

    private static final class BlockingElevationFile extends ElevationFile {
        private final CopernicusElevationModel model;
        private final ElevationTile blockingTile;

        private BlockingElevationFile(CopernicusElevationModel model, File localFile, ElevationTile blockingTile) {
            super(localFile, null);
            this.model = model;
            this.blockingTile = blockingTile;
        }

        @Override
        protected boolean findLocalFile() {
            return true;
        }

        @Override
        protected void getLocalFile() {
            tile = blockingTile;
            model.updateCache(tile);
        }

        @Override
        protected Boolean getRemoteFile() {
            return false;
        }
    }

    private static final class BlockingTile implements ElevationTile {
        private final CountDownLatch enteredTileRead;
        private final CountDownLatch releaseTileRead;
        private final float sample;
        private volatile boolean disposed;

        private BlockingTile(CountDownLatch enteredTileRead, CountDownLatch releaseTileRead, float sample) {
            this.enteredTileRead = enteredTileRead;
            this.releaseTileRead = releaseTileRead;
            this.sample = sample;
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public float getSample(int pixelX, int pixelY) throws Exception {
            enteredTileRead.countDown();
            if (!releaseTileRead.await(5, TimeUnit.SECONDS)) {
                throw new IOException("Timed out waiting for test to release the blocking tile");
            }
            return sample;
        }

        @Override
        public void clearCache() {
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }

    private static final class BlockingDescriptor implements ElevationModelDescriptor {
        private final CountDownLatch enteredTileRead = new CountDownLatch(2);
        private final CountDownLatch releaseTileRead = new CountDownLatch(1);
        private final AtomicInteger createdTiles = new AtomicInteger();

        private ElevationTile createTile() {
            return new BlockingTile(enteredTileRead, releaseTileRead, createdTiles.incrementAndGet() * 10.0f);
        }

        private boolean awaitBothTileReads(long timeout, TimeUnit timeUnit) throws InterruptedException {
            return enteredTileRead.await(timeout, timeUnit);
        }

        private void releaseTileReads() {
            releaseTileRead.countDown();
        }

        @Override
        public String getName() {
            return "Blocking Copernicus";
        }

        @Override
        public float getNoDataValue() {
            return -999.0f;
        }

        @Override
        public int getRasterWidth() {
            return 2;
        }

        @Override
        public int getRasterHeight() {
            return 1;
        }

        @Override
        public int getTileWidthInDegrees() {
            return 1;
        }

        @Override
        public int getTileWidth() {
            return 1;
        }

        @Override
        public int getNumXTiles() {
            return 2;
        }

        @Override
        public int getNumYTiles() {
            return 1;
        }

        @Override
        public ElevationModel createDem(Resampling resampling) {
            return null;
        }

        @Override
        public boolean canBeDownloaded() {
            return false;
        }

        @Override
        public File getDemInstallDir() {
            return new File(".");
        }
    }
}
