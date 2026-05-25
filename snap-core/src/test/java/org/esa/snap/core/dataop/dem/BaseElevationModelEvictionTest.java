/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.core.dataop.dem;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Regression test for the LRU-eviction-of-disposed-tiles bug.
 *
 * <p>{@link BaseElevationModel#updateCache(ElevationTile)} disposes evicted
 * tiles, but the owning {@link ElevationFile} still holds a reference to the
 * disposed tile. Before the fix, {@link ElevationFile#getTile()} would return
 * that half-disposed tile, and {@link BaseElevationTile#getSample(int, int)}
 * would then read through a {@code Band} whose backing {@code Product} had
 * been disposed — yielding garbage or NPEs.
 *
 * <p>The fix introduces {@link ElevationTile#isDisposed()} and teaches
 * {@code ElevationFile.getTile()} to drop and reload stale references.
 */
public class BaseElevationModelEvictionTest {

    /** Tile that knows its load generation. Mirrors the real failure mode:
     *  reads after dispose throw, just as reads through a disposed Band would. */
    private static final class CountingTile implements ElevationTile {
        final int loadId;
        private volatile boolean disposed;

        CountingTile(int loadId) {
            this.loadId = loadId;
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void clearCache() {
            // no-op for the test
        }

        @Override
        public float getSample(int pixelX, int pixelY) {
            if (disposed) {
                throw new IllegalStateException("getSample on disposed tile (loadId=" + loadId + ")");
            }
            return loadId;
        }
    }

    /** Synthesises a fresh CountingTile on every getLocalFile() — no real I/O. */
    private static final class CountingFile extends ElevationFile {
        final AtomicInteger reloadCount = new AtomicInteger();
        final BaseElevationModel model;

        CountingFile(BaseElevationModel model) {
            // ElevationFile's ctor dereferences localFile.getParentFile(), so the
            // dummy path must include a parent directory component.
            super(new File(System.getProperty("java.io.tmpdir"), "dummy.tif"), null);
            this.model = model;
            // Leave remoteFileExists at its default (true) so that the
            // getTile() short-circuit at "!remoteFileExists && !localFileExists"
            // doesn't fire before findLocalFile() has a chance to run.
        }

        @Override
        protected boolean findLocalFile() {
            return true; // pretend the local tile exists
        }

        @Override
        protected void getLocalFile() {
            final CountingTile fresh = new CountingTile(reloadCount.incrementAndGet());
            this.tile = fresh;
            model.updateCache(fresh);
        }

        @Override
        protected Boolean getRemoteFile() {
            return Boolean.FALSE; // never called in this test
        }
    }

    /** 2×2 grid model. createElevationFile installs CountingFiles. */
    private static final class CountingModel extends BaseElevationModel {
        CountingModel(int maxCacheSize) {
            super(new StubDescriptor(), Resampling.NEAREST_NEIGHBOUR);
            setMaxCacheSize(maxCacheSize);
        }

        @Override
        protected void createElevationFile(final ElevationFile[][] elevationFiles,
                                           final int x, final int y, final File demInstallDir) {
            elevationFiles[x][y] = new CountingFile(this);
        }

        @Override public double getIndexX(GeoPos geoPos) { return 0; }
        @Override public double getIndexY(GeoPos geoPos) { return 0; }
        @Override public GeoPos getGeoPos(PixelPos pixelPos) { return new GeoPos(); }
    }

    /** Bare-bones descriptor — 2×2 tiles, NUM_PIXELS_PER_TILE_X/Y = 1. */
    private static final class StubDescriptor implements ElevationModelDescriptor {
        @Override public String getName() { return "Stub"; }
        @Override public float getNoDataValue() { return -1f; }
        @Override public int getRasterWidth() { return 2; }
        @Override public int getRasterHeight() { return 2; }
        @Override public int getTileWidthInDegrees() { return 1; }
        @Override public int getTileWidth() { return 1; }
        @Override public int getNumXTiles() { return 2; }
        @Override public int getNumYTiles() { return 2; }
        @Override public ElevationModel createDem(Resampling resampling) { return null; }
        @Override public boolean canBeDownloaded() { return false; }
        @Override public File getDemInstallDir() { return new File("."); }
    }

    private static CountingFile fileAt(CountingModel m, int x, int y) {
        return (CountingFile) m.elevationFiles[x][y];
    }

    /**
     * After LRU eviction disposes a tile, the next request must observe the
     * disposal and reload — not silently hand back the disposed tile.
     */
    @Test
    @STTM("SNAP-4208")
    public void evictedTileIsReloadedNotReusedAfterDispose() throws Exception {
        final CountingModel m = new CountingModel(/*maxCacheSize*/ 2);

        // Load three distinct tiles in order: A(0,0), B(1,0), C(0,1).
        // With maxCacheSize=2, touching C evicts A and disposes it.
        final ElevationTile a1 = m.elevationFiles[0][0].getTile();
        final ElevationTile b1 = m.elevationFiles[1][0].getTile();
        final ElevationTile c1 = m.elevationFiles[0][1].getTile();
        assertEquals(1, fileAt(m, 0, 0).reloadCount.get());
        assertEquals(1, fileAt(m, 1, 0).reloadCount.get());
        assertEquals(1, fileAt(m, 0, 1).reloadCount.get());
        assertTrue("A should be disposed after eviction by C", a1.isDisposed());
        assertFalse(b1.isDisposed());
        assertFalse(c1.isDisposed());

        // Re-request A. Pre-fix this would return the disposed `a1` and a
        // subsequent getSample would blow up. Post-fix we get a fresh load.
        final ElevationTile a2 = m.elevationFiles[0][0].getTile();
        assertNotSame("expected a fresh tile after eviction", a1, a2);
        assertFalse("reloaded tile must not be disposed", a2.isDisposed());
        assertEquals("reload should have been invoked twice for A",
                2, fileAt(m, 0, 0).reloadCount.get());

        // Reload evicted B (LRU after A was reloaded — B is now oldest).
        assertTrue("B should be disposed after A's reload pushed it out", b1.isDisposed());
        // getSample on the live tile must succeed and return the new load id.
        assertEquals(2f, a2.getSample(0, 0), 0f);
    }

    /**
     * Sanity: a tile not evicted must NOT be reloaded; the same instance is
     * returned. Guards against an over-eager invalidation regression.
     */
    @Test
    @STTM("SNAP-4208")
    public void liveTileIsNotReloaded() throws Exception {
        final CountingModel m = new CountingModel(/*maxCacheSize*/ 4);

        final ElevationTile a1 = m.elevationFiles[0][0].getTile();
        final ElevationTile a2 = m.elevationFiles[0][0].getTile();
        final ElevationTile a3 = m.elevationFiles[0][0].getTile();
        assertEquals("no eviction → no reload", 1, fileAt(m, 0, 0).reloadCount.get());
        // Same identity each time.
        assertEquals(a1, a2);
        assertEquals(a2, a3);
    }

    /**
     * isDisposed() default on the interface is false, so existing third-party
     * ElevationTile implementations that don't override it still compile and
     * behave as before this patch.
     */
    @Test
    @STTM("SNAP-4208")
    public void defaultIsDisposedIsFalse() {
        final ElevationTile bareImpl = new ElevationTile() {
            @Override public void dispose() {}
            @Override public float getSample(int pixelX, int pixelY) { return 0f; }
            @Override public void clearCache() {}
        };
        assertFalse(bareImpl.isDisposed());
    }
}
