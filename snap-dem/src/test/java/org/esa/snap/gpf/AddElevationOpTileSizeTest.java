package org.esa.snap.gpf;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dem.gpf.AddElevationOp;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.esa.snap.runtime.Config;
import org.junit.Test;

import java.awt.Dimension;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;


public class AddElevationOpTileSizeTest {


    private static final String ADD_ELEVATION_TILE_SIZE_KEY = "snap.dem.addElevationTileSize";


    @Test
    @STTM("SNAP-4213")
    public void targetProductUsesConfiguredAddElevationTileSize() {
        Preferences preferences = Config.instance().preferences();
        String previousTileSize = preferences.get(ADD_ELEVATION_TILE_SIZE_KEY, null);
        try {
            preferences.putInt(ADD_ELEVATION_TILE_SIZE_KEY, 5);

            Product product = TestUtils.createProduct("type", 20, 20);

            AddElevationOp op = new AddElevationOp();
            op.setSourceProduct(product);
            op.setParameter("demName", "Copernicus 90m Global DEM");
            Product targetProduct = op.getTargetProduct();

            assertEquals(new Dimension(5, 5), targetProduct.getPreferredTileSize());
        } finally {
            restorePreference(preferences, ADD_ELEVATION_TILE_SIZE_KEY, previousTileSize);
        }
    }

    private static void restorePreference(Preferences preferences, String key, String value) {
        if (value == null) {
            preferences.remove(key);
        } else {
            preferences.put(key, value);
        }
    }
}
