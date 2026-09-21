package org.esa.snap.dem.dataio.copernicus;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dem.dataio.EarthGravitationalModel96;
import org.esa.snap.runtime.Config;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regression cover for the SNAP 14 Copernicus 30 m DEM slowdown, exercised through the
 * PRODUCTION constructor and the PRODUCTION 3600 x 3600 target grid that
 * {@code Copernicus30mFile.createDirectTile()} builds. The pre-existing
 * {@link CopernicusDirectElevationTileTest} only ever used the package-private constructor with
 * hand-picked settings on 4x4 grids, which is why both defects shipped.
 */
@STTM("SNAP-4256")
public class CopernicusDirectElevationTileDefaultsTest {

    /** Copernicus 30 m: 3600 x 3600 target grid, GeoTIFF internally tiled 1024 x 1024. */
    private static final int COP30 = 3600;
    private static final int COP30_INTERNAL_TILE_ROWS = 1024;
    private static final int EXPECTED_BLOCKS = 4;          // 1024 + 1024 + 1024 + 528
    private static final int LAST_BLOCK_ROWS = COP30 - 3 * COP30_INTERNAL_TILE_ROWS;

    /** Copernicus 90 m: 1200 x 1200 grid, internally tiled 2048 x 2048 (one tile for the image). */
    private static final int COP90 = 1200;
    private static final int COP90_INTERNAL_TILE_ROWS = 2048;

    /** Compact grid for value comparisons, so a test does not need 50 MB per tile. */
    private static final int SMALL_W = 256;
    private static final int SMALL_H = 512;
    private static final int SMALL_PREF = 128;             // below the floor, so it is raised to 256
    /** Target width for the upsampling cases; 720 over 480/360/240/144 source columns. */
    private static final int RAMP_TARGET_W = 720;

    private static final String EGM_KEY = "snap.useDEMGravitationalModel";
    private static final String BLOCK_ROWS_KEY = "snap.dem.copernicus.blockRows";
    private static final String RETIRED_KEY = "snap.dem.copernicus.fullTileCacheMaxBytes";

    private String savedEgm;
    private String savedBlockRows;
    private String savedRetired;

    @Before
    public void setUp() {
        // Every one of these is JVM-wide and shared with the other test classes in this fork.
        savedEgm = Config.instance().preferences().get(EGM_KEY, null);
        savedBlockRows = Config.instance().preferences().get(BLOCK_ROWS_KEY, null);
        savedRetired = Config.instance().preferences().get(RETIRED_KEY, null);
        // EGM96 needs auxdata; the tests that want it construct the tile with it explicitly.
        Config.instance().preferences().putBoolean(EGM_KEY, false);
        // Pin the preference the production constructor actually reads, so a developer with it
        // set in snap.properties does not get spurious failures (and so this class keeps testing
        // the derived path rather than whatever happens to be configured).
        Config.instance().preferences().remove(BLOCK_ROWS_KEY);
        Config.instance().preferences().remove(RETIRED_KEY);
    }

    @After
    public void tearDown() {
        restore(EGM_KEY, savedEgm);
        restore(BLOCK_ROWS_KEY, savedBlockRows);
        restore(RETIRED_KEY, savedRetired);
    }

    private static void restore(final String key, final String saved) {
        if (saved == null) {
            Config.instance().preferences().remove(key);
        } else {
            Config.instance().preferences().put(key, saved);
        }
    }


    // ---------------------------------------------------------------- block geometry

    /** Blocks must line up with the source's internal tiling, not with a byte budget. */
    @Test
    public void blockHeightFollowsTheSourceInternalTiling() {
        assertEquals(COP30_INTERNAL_TILE_ROWS, newCop30Tile(new CountingSource(COP30, COP30)).getBlockRows());
    }

    /** A source finer than the floor is read in whole multiples of its own tiling, never below it. */
    @Test
    public void veryFineSourceTilingIsRaisedToWholeMultiples() {
        assertEquals(256, blockRowsFor(1, COP30));      // 1 x 256
        assertEquals(256, blockRowsFor(2, COP30));      // 2 x 128
        assertEquals(258, blockRowsFor(3, COP30));      // 3 x 86
        assertEquals(300, blockRowsFor(100, COP30));    // 100 x 3
        assertEquals(510, blockRowsFor(255, COP30));    // 255 x 2
        assertEquals(256, blockRowsFor(256, COP30));    // already at the floor
        assertEquals(257, blockRowsFor(257, COP30));    // above the floor, left alone
    }

    /** A source that cannot report its tiling is read in one block. */
    @Test
    public void unknownSourceTilingFallsBackToASingleBlock() {
        assertEquals(COP30, blockRowsFor(0, COP30));
        assertEquals(COP30, blockRowsFor(-1, COP30));
    }

    /** The 90 m DEM: internal tile taller than the image, so the whole image is one block. */
    @Test
    public void ninetyMetreTileIsASingleBlock() throws Exception {
        final CountingSource source = new CountingSource(COP90, COP90, COP90_INTERNAL_TILE_ROWS);
        final CopernicusDirectElevationTile tile = new CopernicusDirectElevationTile(
                new FakeElevationModel(), source, COP90, COP90, 111, 80);

        assertEquals(COP90, tile.getBlockRows());
        for (int y = 0; y < COP90; y += 37) {
            tile.getSample(y % COP90, y);
        }
        assertEquals("the whole 90 m image must cost a single read", 1, source.readCount.get());
    }

    /** The short trailing block must be read at its real height, not a full blockRows. */
    @Test
    public void lastShortBlockIsReadAtItsRealHeight() throws Exception {
        final CountingSource source = new CountingSource(COP30, COP30);
        final CopernicusDirectElevationTile tile = newCop30Tile(source);

        tile.getSample(0, COP30 - 1);

        assertEquals(1, source.requests.size());
        final int[] request = source.requests.get(0);
        assertEquals("last block must start at 3 x 1024", 3 * COP30_INTERNAL_TILE_ROWS, request[0]);
        assertEquals("last block must be 528 rows, not a full 1024", LAST_BLOCK_ROWS, request[1]);
        // the source clamps the request internally, so the only way to see a padded block is
        // what the tile actually retains
        assertEquals("the trailing block must hold 528 rows, not a padded 1024",
                     (long) LAST_BLOCK_ROWS * COP30 * Float.BYTES, tile.getRetainedBytes());
    }

    /** A tile holds only the blocks it was asked for - this is what bounds DEM memory. */
    @Test
    public void partialCoverageRetainsOnlyTheBlocksItTouches() throws Exception {
        final CopernicusDirectElevationTile tile = newCop30Tile(new CountingSource(COP30, COP30));

        for (int y = 0; y < 200; y++) {
            tile.getSample(y, y);
        }
        assertEquals("one 1024-row block, not the whole 49 MiB tile",
                     (long) COP30_INTERNAL_TILE_ROWS * COP30 * Float.BYTES, tile.getRetainedBytes());

        for (int y = 0; y < COP30; y += 137) {
            tile.getSample(0, y);
        }
        assertEquals("a fully covered tile holds exactly one tile's worth of floats",
                     (long) COP30 * COP30 * Float.BYTES, tile.getRetainedBytes());
    }


    // ---------------------------------------------------------------- the blockRows override

    /**
     * An explicit override must never land below the source's internal tiling: 64-row reads
     * against a 1024-row internal tile decode each tile 16 times, which is defect 1 itself.
     * 64 was the shipped SNAP 14 default, so an upgrading user may well have it set.
     */
    @Test
    public void unalignedOverrideIsRaisedToTheSourceTiling() throws Exception {
        final CountingSource source = new CountingSource(COP30, COP30);
        final CopernicusDirectElevationTile tile = new CopernicusDirectElevationTile(
                new FakeElevationModel(), source, COP30, COP30, 180, 45, 64, false);

        assertEquals(COP30_INTERNAL_TILE_ROWS, tile.getBlockRows());
        for (int y = 0; y < COP30; y += 137) {
            tile.getSample(10, y);
        }
        assertEquals(EXPECTED_BLOCKS, source.readCount.get());
    }

    /**
     * A source reporting 0 or 1 gives alignment nothing to work with - every block height is a
     * whole multiple of 1 - so an override must still be held to the derived floor rather than
     * sailing through and reinstating tiny unaligned reads.
     */
    @Test
    public void overrideIsFlooredWhenTheSourceCannotReportItsTiling() {
        assertEquals(256, overrideBlockRowsFor(1, 64));
        assertEquals(256, overrideBlockRowsFor(0, 64));
        assertEquals(256, overrideBlockRowsFor(1, 1));
        assertEquals("an override above the floor is still honoured", 1024, overrideBlockRowsFor(1, 1024));
    }

    @Test
    public void alignedOverrideIsHonouredAsGiven() {
        assertEquals(2048, overrideBlockRows(2048));   // already a multiple of 1024
        assertEquals(2048, overrideBlockRows(2000));   // rounded up to the next multiple
        assertEquals(COP30, overrideBlockRows(9999));  // never beyond the tile
    }

    /**
     * A nonsense override must clamp, not overflow. Rounding a near-Integer.MAX_VALUE request up
     * to the next multiple wraps in int arithmetic, and a negative block height sizes the blocks
     * array to zero - every sample then fails with IndexOutOfBoundsException.
     */
    @Test
    public void absurdOverrideClampsInsteadOfOverflowing() throws Exception {
        for (int override : new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE - 1,
                                      Integer.MAX_VALUE - 1022, Integer.MAX_VALUE - 1023}) {
            assertEquals("override " + override, COP30, overrideBlockRows(override));
        }

        // and the tile must still be usable, not just report a sane number
        final CountingSource source = new CountingSource(COP30, COP30);
        final CopernicusDirectElevationTile tile = new CopernicusDirectElevationTile(
                new FakeElevationModel(), source, COP30, COP30, 180, 45, Integer.MAX_VALUE, false);
        assertEquals(CountingSource.expected(7, COP30 - 1), tile.getSample(7, COP30 - 1), 0.0f);
        assertEquals(1, source.readCount.get());
    }

    /** Alignment must also survive a source that reports an absurd internal tile height. */
    @Test
    public void absurdSourceTilingClampsInsteadOfOverflowing() {
        assertEquals(COP30, blockRowsFor(Integer.MAX_VALUE, COP30));
        assertEquals(COP30, new CopernicusDirectElevationTile(new FakeElevationModel(),
                new CountingSource(64, COP30, Integer.MAX_VALUE), 64, COP30, 180, 45, 64, false)
                .getBlockRows());
    }

    /** The production default must be the derived value, not a leftover constant. */
    @Test
    public void productionDefaultDerivesFromTheSource() {
        Config.instance().preferences().putInt(BLOCK_ROWS_KEY, 64);
        try {
            // even explicitly configured, the unaligned value is corrected
            assertEquals(COP30_INTERNAL_TILE_ROWS, newCop30Tile(new CountingSource(COP30, COP30)).getBlockRows());
        } finally {
            Config.instance().preferences().remove(BLOCK_ROWS_KEY);
        }
    }


    // ---------------------------------------------------------------- read behaviour

    /** Defect 1: no row may be decoded twice, however the tile is walked. */
    @Test
    public void everyRowBlockIsReadAtMostOnce() throws Exception {
        final CountingSource source = new CountingSource(COP30, COP30);
        final CopernicusDirectElevationTile tile = newCop30Tile(source);

        // walk the tile the way a multi-threaded scheduler does: jump between row bands
        for (int y = 0; y < COP30; y += 137) {
            tile.getSample(10, y);
        }
        for (int y = COP30 - 1; y >= 0; y -= 137) {
            tile.getSample(10, y);
        }

        assertEquals("a fully covered 3600x3600 tile must cost one read per row block",
                     EXPECTED_BLOCKS, source.readCount.get());
    }

    /** Loaded blocks are never evicted, so a second pass costs nothing. */
    @Test
    public void loadedBlocksAreNeverEvicted() throws Exception {
        final CountingSource source = new CountingSource(COP30, COP30);
        final CopernicusDirectElevationTile tile = newCop30Tile(source);

        for (int pass = 0; pass < 3; pass++) {
            for (int y = 0; y < COP30; y += 7) {
                tile.getSample(y % COP30, y);
            }
        }

        assertEquals(EXPECTED_BLOCKS, source.readCount.get());
    }

    /** A product overlapping only part of a tile must not pay for the whole tile. */
    @Test
    public void partialCoverageReadsOnlyTheBlocksItTouches() throws Exception {
        final CountingSource source = new CountingSource(COP30, COP30);
        final CopernicusDirectElevationTile tile = newCop30Tile(source);

        for (int y = 0; y < 200; y++) {
            tile.getSample(y, y);
        }

        assertEquals("touching the top 200 rows must read one block, not the whole tile",
                     1, source.readCount.get());
    }


    // ---------------------------------------------------------------- values

    /**
     * Blocking must not change a single elevation. Compares the derived multi-block tile against
     * one configured as a single block, which is what the previous full-tile cache did.
     */
    @Test
    public void perBlockValuesMatchSingleBlockExactly() throws Exception {
        assertPerBlockMatchesSingleBlock(false);
    }

    /**
     * The same, with the geoid correction on. This is what pins the per-block latitude: the EGM
     * offset is a function of the GLOBAL row, so a block that used its block-local row index
     * would be wrong by up to a degree of latitude in every block after the first.
     */
    @Test
    public void perBlockValuesMatchSingleBlockExactlyWithGeoidCorrection() throws Exception {
        assumeGeoidAvailable();
        assertPerBlockMatchesSingleBlock(true);
    }

    private void assertPerBlockMatchesSingleBlock(final boolean useGeoid) throws Exception {
        final CopernicusDirectElevationTile perBlock = new CopernicusDirectElevationTile(
                new FakeElevationModel(), new CountingSource(SMALL_W, SMALL_H, SMALL_PREF),
                SMALL_W, SMALL_H, 111, 40, 0, useGeoid);
        final CopernicusDirectElevationTile singleBlock = new CopernicusDirectElevationTile(
                new FakeElevationModel(), new CountingSource(SMALL_W, SMALL_H, SMALL_PREF),
                SMALL_W, SMALL_H, 111, 40, SMALL_H, useGeoid);

        // the floor branch must have raised 128 to 256, giving two blocks over 512 rows
        assertEquals(256, perBlock.getBlockRows());
        assertEquals(SMALL_H, singleBlock.getBlockRows());

        for (int y = 0; y < SMALL_H; y++) {
            for (int x = 0; x < SMALL_W; x += 17) {
                assertEquals("mismatch at (" + x + ',' + y + ") geoid=" + useGeoid,
                             singleBlock.getSample(x, y), perBlock.getSample(x, y), 0.0f);
            }
        }
    }

    /**
     * Real Copernicus 30 m tiles above 50 deg N/S keep 3600 rows but drop to 2400 / 1800 / 1200 /
     * 720 columns, so every high-latitude tile is upsampled in x. The fakes elsewhere in this
     * class are all 1:1, which leaves the interpolation at a zero fraction and never exercised.
     * <p>
     * The source here is a pure ramp - its value IS its column index - so interpolating it has an
     * independent oracle: a linear ramp interpolated at {@code sourceX} is {@code sourceX}, with
     * the right edge clamped at the last source column. That checks the interpolation without
     * restating the implementation's own formula.
     */
    @Test
    public void narrowHighLatitudeSourceIsUpsampledAcrossX() throws Exception {
        for (int sourceWidth : new int[]{480, 360, 240, 144}) {     // ratios 1.5, 2, 3, 5
            final RampSource source = new RampSource(sourceWidth, SMALL_H, SMALL_PREF);
            final CopernicusDirectElevationTile tile = new CopernicusDirectElevationTile(
                    new FakeElevationModel(), source, RAMP_TARGET_W, SMALL_H, 111, 40, 0, false);
            assertEquals(256, tile.getBlockRows());          // more than one block over 512 rows

            final double upSampling = (double) RAMP_TARGET_W / sourceWidth;
            for (int y : new int[]{0, 1, 255, 256, 257, SMALL_H - 1}) {
                for (int x = 0; x < RAMP_TARGET_W; x++) {
                    final double sourceX = x / upSampling;
                    final float expected = (float) Math.min(sourceX, sourceWidth - 1);
                    final float actual = tile.getSample(x, y);
                    if (Math.abs(expected - actual) > 1e-3f) {
                        fail("sourceWidth " + sourceWidth + " at (" + x + ',' + y + "): expected "
                             + expected + " but was " + actual);
                    }
                }
            }
        }
    }

    /** Upsampling must not change across a block boundary either. */
    @Test
    public void upsampledValuesMatchBetweenBlockedAndSingleBlockTiles() throws Exception {
        final CopernicusDirectElevationTile perBlock = new CopernicusDirectElevationTile(
                new FakeElevationModel(), new RampSource(720, SMALL_H, SMALL_PREF),
                RAMP_TARGET_W, SMALL_H, 111, 40, 0, false);
        final CopernicusDirectElevationTile singleBlock = new CopernicusDirectElevationTile(
                new FakeElevationModel(), new RampSource(720, SMALL_H, SMALL_PREF),
                RAMP_TARGET_W, SMALL_H, 111, 40, SMALL_H, false);

        assertEquals(256, perBlock.getBlockRows());
        assertEquals(SMALL_H, singleBlock.getBlockRows());
        for (int y = 0; y < SMALL_H; y += 7) {
            for (int x = 0; x < RAMP_TARGET_W; x += 13) {
                assertEquals("(" + x + ',' + y + ')',
                             singleBlock.getSample(x, y), perBlock.getSample(x, y), 0.0f);
            }
        }
    }

    /** Values either side of every block boundary must be continuous with the source data. */
    @Test
    public void valuesAreCorrectAcrossBlockBoundaries() throws Exception {
        final CopernicusDirectElevationTile tile = newCop30Tile(new CountingSource(COP30, COP30));

        final int[] rows = {0, 1023, 1024, 1025, 2047, 2048, 3071, 3072, 3073, COP30 - 1};
        for (int y : rows) {
            for (int x : new int[]{0, 1, COP30 / 2, COP30 - 1}) {
                assertEquals("row " + y + " col " + x,
                             CountingSource.expected(x, y), tile.getSample(x, y), 0.0f);
            }
        }
    }

    /** The geoid offset must be applied, and must track the global row, not the block-local one. */
    @Test
    public void geoidOffsetTracksTheGlobalRow() throws Exception {
        assumeGeoidAvailable();
        final EarthGravitationalModel96 egm = EarthGravitationalModel96.instance();

        final int tileX = 111;
        final int tileY = 40;
        final CopernicusDirectElevationTile tile = new CopernicusDirectElevationTile(
                new FakeElevationModel(), new CountingSource(SMALL_W, SMALL_H, SMALL_PREF),
                SMALL_W, SMALL_H, tileX, tileY, 0, true);

        // row 300 is in the SECOND block (blockRows == 256), so its block-local index is 44
        final int globalRow = 300;
        final int x = 7;
        final double lat = 90.0 - tileY - (double) globalRow / (double) SMALL_H;
        final double lon = (tileX - 180.0) + x / (double) SMALL_W;
        final float expected = CountingSource.expected(x, globalRow)
                + egm.getEGM(lat, lon, new double[4][4]);

        assertEquals(expected, tile.getSample(x, globalRow), 1e-3f);

        // and the block-local reading must NOT be what we get
        final double wrongLat = 90.0 - tileY - 44.0 / SMALL_H;
        final float wrong = CountingSource.expected(x, globalRow) + egm.getEGM(wrongLat, lon, new double[4][4]);
        assertNotEquals("geoid must use the global row, not the block-local one",
                        wrong, tile.getSample(x, globalRow), 1e-6f);
    }


    // ---------------------------------------------------------------- lifecycle

    @Test
    public void disposeRejectsLaterReads() throws Exception {
        final CopernicusDirectElevationTile tile = newCop30Tile(new CountingSource(COP30, COP30));
        tile.getSample(0, 0);
        tile.dispose();

        assertTrue(tile.isDisposed());
        try {
            tile.getSample(0, 0);
            fail("a disposed tile must not serve samples");
        } catch (IllegalStateException expected) {
            // contract
        }
    }

    @Test
    public void disposeIsIdempotent() throws Exception {
        final CountingSource source = new CountingSource(COP30, COP30);
        final CopernicusDirectElevationTile tile = newCop30Tile(source);
        tile.getSample(0, 0);

        tile.dispose();
        tile.dispose();

        assertTrue(tile.isDisposed());
        // production really does double-dispose: BaseElevationModel.dispose() disposes every
        // cached tile and then walks the ElevationFiles, which dispose the same tiles again.
        // CopernicusTileSource.close() promises nothing about repeat calls, so guard it here.
        assertEquals("the source must be closed exactly once", 1, source.closeCount.get());
    }

    @Test
    public void clearCacheDropsBlocksButKeepsTheTileUsable() throws Exception {
        final CountingSource source = new CountingSource(COP30, COP30);
        final CopernicusDirectElevationTile tile = newCop30Tile(source);

        final float before = tile.getSample(5, 5);
        assertEquals(1, source.readCount.get());

        tile.clearCache();

        assertEquals("re-read after clearCache must return the same value", before, tile.getSample(5, 5), 0.0f);
        assertEquals("the dropped block must have been read again", 2, source.readCount.get());
    }


    // ---------------------------------------------------------------- concurrency

    /** Defect 2: once a block is loaded, reading it must not need the tile monitor. */
    @Test
    public void getSampleDoesNotTakeTheTileMonitorOnceLoaded() throws Exception {
        final CopernicusDirectElevationTile tile = newCop30Tile(new CountingSource(COP30, COP30));

        final int probeX = 1234;
        final int probeY = 345;
        tile.getSample(0, probeY);   // force the load of the block the probe will read
        assertTrue("probe must target an already loaded block",
                   probeY / tile.getBlockRows() == 0);

        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<Float> value = new AtomicReference<>();
        final Thread reader = new Thread(() -> {
            try {
                value.set(tile.getSample(probeX, probeY));
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });

        synchronized (tile) {                 // hold the tile's monitor
            reader.start();
            assertTrue("getSample must complete while the tile monitor is held",
                       done.await(10, TimeUnit.SECONDS));
        }
        reader.join();
        assertNull(error.get());
        assertEquals(CountingSource.expected(probeX, probeY), value.get(), 0.0f);
    }


    // ---------------------------------------------------------------- helpers

    private static void assumeGeoidAvailable() {
        try {
            EarthGravitationalModel96.instance();
        } catch (Exception e) {
            Assume.assumeNoException("EGM96 auxdata unavailable", e);
        }
    }

    private static int blockRowsFor(final int preferredBlockHeight, final int targetHeight) {
        return new CopernicusDirectElevationTile(new FakeElevationModel(),
                new CountingSource(64, targetHeight, preferredBlockHeight),
                64, targetHeight, 180, 45, 0, false).getBlockRows();
    }

    private static int overrideBlockRowsFor(final int preferredBlockHeight, final int override) {
        return new CopernicusDirectElevationTile(new FakeElevationModel(),
                new CountingSource(64, COP30, preferredBlockHeight), 64, COP30, 180, 45, override, false)
                .getBlockRows();
    }

    private static int overrideBlockRows(final int override) {
        return new CopernicusDirectElevationTile(new FakeElevationModel(),
                new CountingSource(64, COP30), 64, COP30, 180, 45, override, false).getBlockRows();
    }

    private static CopernicusDirectElevationTile newCop30Tile(final CountingSource source) {
        // public constructor == exactly what Copernicus30mFile.createDirectTile() builds
        return new CopernicusDirectElevationTile(new FakeElevationModel(), source, COP30, COP30, 180, 45);
    }

    /** Stands in for a Copernicus 30 m COG: 3600 x 3600, internally tiled 1024 x 1024. */
    private static final class CountingSource implements CopernicusTileSource {
        private final int width;
        private final int height;
        private final int preferredBlockHeight;
        private final AtomicInteger readCount = new AtomicInteger();
        private final AtomicInteger closeCount = new AtomicInteger();
        private final List<int[]> requests = Collections.synchronizedList(new ArrayList<>());

        private CountingSource(int width, int height) {
            this(width, height, COP30_INTERNAL_TILE_ROWS);
        }

        private CountingSource(int width, int height, int preferredBlockHeight) {
            this.width = width;
            this.height = height;
            this.preferredBlockHeight = preferredBlockHeight;
        }

        static float expected(int x, int y) {
            return y * 10.0f + x;
        }

        @Override
        public int getWidth() { return width; }

        @Override
        public int getHeight() { return height; }

        @Override
        public int getPreferredBlockHeight() { return preferredBlockHeight; }

        @Override
        public float[] readRows(int y, int rowCount) {
            readCount.incrementAndGet();
            requests.add(new int[]{y, rowCount});
            final float[] rows = new float[width * rowCount];
            for (int row = 0; row < rowCount; row++) {
                final int off = row * width;
                for (int x = 0; x < width; x++) {
                    rows[off + x] = expected(x, y + row);
                }
            }
            return rows;
        }

        @Override
        public void close() { closeCount.incrementAndGet(); }
    }

    /** Value == column index, so interpolating it has an oracle independent of the implementation. */
    private static final class RampSource implements CopernicusTileSource {
        private final int width;
        private final int height;
        private final int preferredBlockHeight;

        private RampSource(int width, int height, int preferredBlockHeight) {
            this.width = width;
            this.height = height;
            this.preferredBlockHeight = preferredBlockHeight;
        }

        @Override
        public int getWidth() { return width; }

        @Override
        public int getHeight() { return height; }

        @Override
        public int getPreferredBlockHeight() { return preferredBlockHeight; }

        @Override
        public float[] readRows(int y, int rowCount) {
            final float[] rows = new float[width * rowCount];
            for (int row = 0; row < rowCount; row++) {
                for (int x = 0; x < width; x++) {
                    rows[row * width + x] = x;
                }
            }
            return rows;
        }

        @Override
        public void close() { }
    }

    private static final class FakeElevationModel implements ElevationModel {
        private final ElevationModelDescriptor descriptor = new FakeDescriptor();

        @Override
        public ElevationModelDescriptor getDescriptor() { return descriptor; }

        @Override
        public double getElevation(GeoPos geoPos) { return 0.0; }

        @Override
        public PixelPos getIndex(GeoPos geoPos) { return new PixelPos(); }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos) {
            return new GeoPos(90.0 - pixelPos.y, pixelPos.x - 180.0);
        }

        @Override
        public double getSample(double pixelX, double pixelY) { return 0.0; }

        @Override
        public boolean getSamples(int[] x, int[] y, double[][] samples) { return true; }

        @Override
        public Resampling getResampling() { return Resampling.NEAREST_NEIGHBOUR; }

        @Override
        public void dispose() { }
    }

    private static final class FakeDescriptor implements ElevationModelDescriptor {
        @Override
        public String getName() { return "Fake"; }
        @Override
        public float getNoDataValue() { return -32768.0f; }
        @Override
        public int getRasterWidth() { return 360 * COP30; }
        @Override
        public int getRasterHeight() { return 180 * COP30; }
        @Override
        public int getTileWidthInDegrees() { return 1; }
        @Override
        public int getTileWidth() { return COP30; }
        @Override
        public int getNumXTiles() { return 360; }
        @Override
        public int getNumYTiles() { return 180; }
        @Override
        public ElevationModel createDem(Resampling resampling) { return null; }
        @Override
        public boolean canBeDownloaded() { return false; }
        @Override
        public File getDemInstallDir() { return new File("."); }
    }
}
