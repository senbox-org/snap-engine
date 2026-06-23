package org.esa.snap.gpf;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.gpf.internal.TileImpl;
import org.esa.snap.dem.gpf.AddElevationOp;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.esa.snap.runtime.Config;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.File;
import java.lang.reflect.Field;
import java.util.prefs.Preferences;
import javax.media.jai.RasterFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class AddElevationOpTileSizeTest {


    private static final String ADD_ELEVATION_TILE_SIZE_KEY = "snap.dem.addElevationTileSize";
    private static final String MAX_DEGREES_PER_ELEVATION_TILE_KEY = "snap.dem.maxDegreesPerElevationTile";


    @Test
    @STTM("SNAP-4213")
    public void targetProductKeepsSourcePreferredTileSize() {
        Preferences preferences = Config.instance().preferences();
        String previousTileSize = preferences.get(ADD_ELEVATION_TILE_SIZE_KEY, null);
        try {
            preferences.putInt(ADD_ELEVATION_TILE_SIZE_KEY, 5);

            Product product = TestUtils.createProduct("type", 20, 20);
            product.setPreferredTileSize(new Dimension(16, 12));

            AddElevationOp op = new AddElevationOp();
            op.setSourceProduct(product);
            op.setParameter("demName", "Copernicus 90m Global DEM");
            Product targetProduct = op.getTargetProduct();

            assertEquals(new Dimension(16, 12), targetProduct.getPreferredTileSize());
        } finally {
            restorePreference(preferences, ADD_ELEVATION_TILE_SIZE_KEY, previousTileSize);
        }
    }

    @Test
    @STTM("SNAP-4213")
    public void computeTileSplitsLargeTargetTileIntoConfiguredElevationChunks() throws Exception {
        Preferences preferences = Config.instance().preferences();
        String previousTileSize = preferences.get(ADD_ELEVATION_TILE_SIZE_KEY, null);
        String previousMaxDegrees = preferences.get(MAX_DEGREES_PER_ELEVATION_TILE_KEY, null);
        try {
            preferences.putInt(ADD_ELEVATION_TILE_SIZE_KEY, 5);
            preferences.putDouble(MAX_DEGREES_PER_ELEVATION_TILE_KEY, 100.0);

            Product product = TestUtils.createProduct("type", 12, 10);
            product.setPreferredTileSize(new Dimension(12, 10));
            product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                                       product.getSceneRasterWidth(),
                                                       product.getSceneRasterHeight(),
                                                       0.0, 50.0,
                                                       1.0, -1.0,
                                                       0.0, 0.0));

            AddElevationOp op = new AddElevationOp();
            op.setSourceProduct(product);
            op.setParameter("demName", "Copernicus 90m Global DEM");
            Product targetProduct = op.getTargetProduct();

            RecordingElevationModel dem = new RecordingElevationModel(-999.0f);
            setPrivateField(op, "dem", dem);
            setPrivateField(op, "demNoDataValue", -999.0);

            Band elevationBand = targetProduct.getBand("elevation");
            Rectangle targetRectangle = new Rectangle(0, 0, 12, 10);
            WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT,
                                                                     targetRectangle.width,
                                                                     targetRectangle.height,
                                                                     1,
                                                                     new Point(targetRectangle.x, targetRectangle.y));
            TileImpl tile = new TileImpl(elevationBand, raster, targetRectangle);

            op.computeTile(elevationBand, tile, ProgressMonitor.NULL);

            assertEquals(120, dem.getElevationCallCount());
            assertTrue("Expected chunked processing to change row traversal direction",
                       dem.getLatDirectionChangeCount() > 0);
        } finally {
            restorePreference(preferences, ADD_ELEVATION_TILE_SIZE_KEY, previousTileSize);
            restorePreference(preferences, MAX_DEGREES_PER_ELEVATION_TILE_KEY, previousMaxDegrees);
        }
    }

    private static void restorePreference(Preferences preferences, String key, String value) {
        if (value == null) {
            preferences.remove(key);
        } else {
            preferences.put(key, value);
        }
    }

    private static void setPrivateField(Object target, String name, Object value) throws Exception {
        Field field = AddElevationOp.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class RecordingElevationModel implements ElevationModel {
        private final ElevationModelDescriptor descriptor;
        private int elevationCallCount;
        private int latDirectionChangeCount;
        private double previousLat = Double.NaN;
        private int previousLatDirection;

        private RecordingElevationModel(float noDataValue) {
            descriptor = new FakeDescriptor(noDataValue);
        }

        @Override
        public ElevationModelDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public double getElevation(GeoPos geoPos) {
            if (!Double.isNaN(previousLat)) {
                final double latDiff = geoPos.lat - previousLat;
                if (Math.abs(latDiff) > 0.5) {
                    final int latDirection = latDiff > 0.0 ? 1 : -1;
                    if (previousLatDirection != 0 && latDirection != previousLatDirection) {
                        latDirectionChangeCount++;
                    }
                    previousLatDirection = latDirection;
                }
            }
            previousLat = geoPos.lat;
            elevationCallCount++;
            return geoPos.lat + geoPos.lon;
        }

        @Override
        public PixelPos getIndex(GeoPos geoPos) {
            return new PixelPos((float) geoPos.lon, (float) geoPos.lat);
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos) {
            return new GeoPos(pixelPos.y, pixelPos.x);
        }

        @Override
        public double getSample(double x, double y) {
            return y + x;
        }

        @Override
        public boolean getSamples(int[] x, int[] y, double[][] samples) {
            for (int row = 0; row < y.length; row++) {
                for (int col = 0; col < x.length; col++) {
                    samples[row][col] = getSample(x[col], y[row]);
                }
            }
            return true;
        }

        @Override
        public Resampling getResampling() {
            return Resampling.NEAREST_NEIGHBOUR;
        }

        @Override
        public void dispose() {
        }

        private int getElevationCallCount() {
            return elevationCallCount;
        }

        private int getLatDirectionChangeCount() {
            return latDirectionChangeCount;
        }
    }

    private static final class FakeDescriptor implements ElevationModelDescriptor {
        private final float noDataValue;

        private FakeDescriptor(float noDataValue) {
            this.noDataValue = noDataValue;
        }

        @Override
        public String getName() {
            return "Fake";
        }

        @Override
        public float getNoDataValue() {
            return noDataValue;
        }

        @Override
        public int getRasterWidth() {
            return 360;
        }

        @Override
        public int getRasterHeight() {
            return 180;
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
            return 360;
        }

        @Override
        public int getNumYTiles() {
            return 180;
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
            return null;
        }
    }
}
