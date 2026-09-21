package org.esa.snap.dem.dataio.copernicus;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.runtime.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Races the lock-free read path for real. The monitor-holding test in
 * {@link CopernicusDirectElevationTileDefaultsTest} proves the fast path takes no lock; this
 * proves that is actually safe: every sample is exact, each block loads exactly once, and the
 * GeoTIFF reader underneath is never entered concurrently.
 */
public class CopernicusDirectElevationTileConcurrencyTest {

    private static final int COP30 = 3600;
    private static final int INTERNAL = 1024;
    private static final String EGM_KEY = "snap.useDEMGravitationalModel";
    private static final String BLOCK_ROWS_KEY = "snap.dem.copernicus.blockRows";

    private String savedEgm;
    private String savedBlockRows;

    @Before
    public void setUp() {
        savedEgm = Config.instance().preferences().get(EGM_KEY, null);
        savedBlockRows = Config.instance().preferences().get(BLOCK_ROWS_KEY, null);
        Config.instance().preferences().putBoolean(EGM_KEY, false);
        // this test builds the tile through the PUBLIC constructor and hard-asserts the block
        // count, so the preference that sets it must be pinned, not inherited from the environment
        Config.instance().preferences().remove(BLOCK_ROWS_KEY);
    }

    @After
    public void tearDown() {
        restore(EGM_KEY, savedEgm);
        restore(BLOCK_ROWS_KEY, savedBlockRows);
    }

    private static void restore(final String key, final String saved) {
        if (saved == null) {
            Config.instance().preferences().remove(key);
        } else {
            Config.instance().preferences().put(key, saved);
        }
    }

    @Test
    @STTM("SNAP-4256")
    public void manyThreadsOnOneTile() throws Exception {
        final RaceDetectingSource source = new RaceDetectingSource();
        final CopernicusDirectElevationTile tile = new CopernicusDirectElevationTile(
                new FakeElevationModel(), source, COP30, COP30, 180, 45);

        final int threads = 16;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicLong wrongValues = new AtomicLong();
        final List<Thread> all = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int seed = t;
            final Thread th = new Thread(() -> {
                try {
                    start.await();
                    final java.util.Random rnd = new java.util.Random(seed);
                    for (int i = 0; i < 60_000; i++) {
                        final int x = rnd.nextInt(COP30);
                        final int y = rnd.nextInt(COP30);
                        final float got = tile.getSample(x, y);
                        final float want = y * 10.0f + x;   // what the source encodes
                        if (got != want) {
                            wrongValues.incrementAndGet();
                        }
                    }
                } catch (Throwable e) {
                    error.compareAndSet(null, e);
                } finally {
                    done.countDown();
                }
            });
            all.add(th);
            th.start();
        }
        start.countDown();
        assertTrue("threads must finish", done.await(120, java.util.concurrent.TimeUnit.SECONDS));
        for (Thread th : all) {
            th.join();
        }

        assertNull("no thread may fail", error.get());
        assertEquals("every sample must be exact", 0, wrongValues.get());
        assertEquals("each of the 4 blocks loaded exactly once despite 16 racing threads",
                     4, source.readCount.get());
        assertEquals("readRows must never be entered concurrently (GeoTIFF reader is not thread-safe)",
                     0, source.concurrentEntries.get());
        System.out.println("stress ok: reads=" + source.readCount.get()
                + " maxConcurrentReaders=" + source.maxConcurrent.get());
    }

    private static final class RaceDetectingSource implements CopernicusTileSource {
        final AtomicInteger readCount = new AtomicInteger();
        final AtomicInteger inFlight = new AtomicInteger();
        final AtomicInteger maxConcurrent = new AtomicInteger();
        final AtomicInteger concurrentEntries = new AtomicInteger();

        @Override
        public int getWidth() { return COP30; }

        @Override
        public int getHeight() { return COP30; }

        @Override
        public int getPreferredBlockHeight() { return INTERNAL; }

        @Override
        public float[] readRows(int y, int rowCount) {
            final int now = inFlight.incrementAndGet();
            maxConcurrent.accumulateAndGet(now, Math::max);
            if (now > 1) {
                concurrentEntries.incrementAndGet();
            }
            readCount.incrementAndGet();
            try {
                Thread.sleep(20);   // widen the window for a racing loader
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final float[] rows = new float[COP30 * rowCount];
            for (int row = 0; row < rowCount; row++) {
                final int off = row * COP30;
                for (int x = 0; x < COP30; x++) {
                    rows[off + x] = (y + row) * 10.0f + x;
                }
            }
            inFlight.decrementAndGet();
            return rows;
        }

        @Override
        public void close() { }
    }

    private static final class FakeElevationModel implements ElevationModel {
        private final ElevationModelDescriptor d = new FakeDescriptor();
        @Override public ElevationModelDescriptor getDescriptor() { return d; }
        @Override public double getElevation(GeoPos g) { return 0; }
        @Override public PixelPos getIndex(GeoPos g) { return new PixelPos(); }
        @Override public GeoPos getGeoPos(PixelPos p) { return new GeoPos(); }
        @Override public double getSample(double x, double y) { return 0; }
        @Override public boolean getSamples(int[] x, int[] y, double[][] s) { return true; }
        @Override public Resampling getResampling() { return Resampling.NEAREST_NEIGHBOUR; }
        @Override public void dispose() { }
    }

    private static final class FakeDescriptor implements ElevationModelDescriptor {
        @Override public String getName() { return "Fake"; }
        @Override public float getNoDataValue() { return 0.0f; }
        @Override public int getRasterWidth() { return 360 * COP30; }
        @Override public int getRasterHeight() { return 180 * COP30; }
        @Override public int getTileWidthInDegrees() { return 1; }
        @Override public int getTileWidth() { return COP30; }
        @Override public int getNumXTiles() { return 360; }
        @Override public int getNumYTiles() { return 180; }
        @Override public ElevationModel createDem(Resampling r) { return null; }
        @Override public boolean canBeDownloaded() { return false; }
        @Override public File getDemInstallDir() { return new File("."); }
    }
}
