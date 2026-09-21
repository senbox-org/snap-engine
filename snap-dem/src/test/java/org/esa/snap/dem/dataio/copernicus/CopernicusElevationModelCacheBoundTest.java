package org.esa.snap.dem.dataio.copernicus;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dem.dataio.copernicus.copernicus30m.Copernicus30mElevationModel;
import org.esa.snap.dem.dataio.copernicus.copernicus30m.Copernicus30mElevationModelDescriptor;
import org.esa.snap.runtime.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A Copernicus DEM tile keeps every row block it has been asked for until the tile itself is
 * evicted, so the number of cached tiles is what bounds DEM memory. This pins the preference that
 * sets that bound - including the degenerate values, because
 * {@code BaseElevationModel.updateCache} evicts down to the bound immediately after inserting,
 * so a bound below 1 disposes each tile at the moment it is handed to the caller.
 */
public class CopernicusElevationModelCacheBoundTest {

    private static final String KEY = "snap.dem.copernicus.maxCachedTiles";

    private String saved;

    @Before
    public void setUp() {
        saved = Config.instance().preferences().get(KEY, null);
    }

    @After
    public void tearDown() {
        if (saved == null) {
            Config.instance().preferences().remove(KEY);
        } else {
            Config.instance().preferences().put(KEY, saved);
        }
    }

    @Test
    public void aCacheBoundOfZeroDoesNotDisposeTheTileItWasJustGiven() {
        Config.instance().preferences().putInt(KEY, 0);
        final FakeTile tile = new FakeTile();

        newModel().updateCache(tile);

        assertFalse("a bound of 0 must be raised to 1, not dispose the live tile",
                    tile.isDisposed());
    }

    @Test
    public void aNegativeCacheBoundDoesNotDisposeTheTileItWasJustGiven() {
        Config.instance().preferences().putInt(KEY, -5);
        final FakeTile tile = new FakeTile();

        newModel().updateCache(tile);

        assertFalse(tile.isDisposed());
    }

    /** The preference must actually reach setMaxCacheSize, not be read and dropped. */
    @Test
    @STTM("SNAP-4256")
    public void theConfiguredBoundIsWhatEvicts() {
        Config.instance().preferences().putInt(KEY, 2);
        final Copernicus30mElevationModel model = newModel();

        final FakeTile first = new FakeTile();
        final FakeTile second = new FakeTile();
        final FakeTile third = new FakeTile();
        model.updateCache(first);
        model.updateCache(second);
        model.updateCache(third);

        assertTrue("with a bound of 2 the oldest of three tiles must be evicted", first.isDisposed());
        assertFalse(second.isDisposed());
        assertFalse(third.isDisposed());
    }

    @Test
    @STTM("SNAP-4256")
    public void theDefaultBoundKeepsFarMoreThanAHandfulOfTiles() {
        Config.instance().preferences().remove(KEY);
        final Copernicus30mElevationModel model = newModel();

        final FakeTile first = new FakeTile();
        model.updateCache(first);
        for (int i = 0; i < 50; i++) {
            model.updateCache(new FakeTile());
        }

        assertFalse("the default bound must comfortably hold 51 tiles", first.isDisposed());
    }

    private static Copernicus30mElevationModel newModel() {
        return new Copernicus30mElevationModel(new Copernicus30mElevationModelDescriptor(),
                                               Resampling.NEAREST_NEIGHBOUR);
    }

    private static final class FakeTile implements ElevationTile {
        private boolean disposed;

        @Override
        public float getSample(int pixelX, int pixelY) {
            return 0.0f;
        }

        @Override
        public void clearCache() {
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
