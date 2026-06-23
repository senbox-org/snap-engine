package org.esa.snap.dem.dataio;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.esa.snap.runtime.Config;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class DEMFactoryFillTest {

    private static final String ADD_ELEVATION_TILE_SIZE_KEY = "snap.dem.addElevationTileSize";
    private static final String MAX_DEGREES_PER_ELEVATION_TILE_KEY = "snap.dem.maxDegreesPerElevationTile";


    @Test
    @STTM("SNAP-4213")
    public void fillElevationDataWritesDirectlyIntoTargetBuffer() throws Exception {
        Product product = TestUtils.createProduct("test", 8, 6);
        Rectangle rectangle = new Rectangle(2, 1, 3, 2);
        TileGeoreferencing tileGeoRef = new TileGeoreferencing(product, rectangle.x, rectangle.y,
                                                               rectangle.width, rectangle.height);
        ProductData targetData = ProductData.createInstance(ProductData.TYPE_FLOAT32,
                                                            rectangle.width * rectangle.height);
        FakeElevationModel dem = new FakeElevationModel(-999.0f);

        boolean valid = DEMFactory.fillElevationData(dem, dem.getDescriptor().getNoDataValue(), tileGeoRef,
                                                     rectangle, targetData, true, ProgressMonitor.NULL);

        assertTrue(valid);

        GeoPos expectedGeoPos = new GeoPos();
        int index = 0;
        for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                tileGeoRef.getGeoPos(x, y, expectedGeoPos);
                assertEquals(elevationFor(expectedGeoPos), targetData.getElemDoubleAt(index), 1.0e-4);
                index++;
            }
        }
    }

    @Test
    @STTM("SNAP-4213")
    public void fillElevationDataWritesOnlyRequestedSubRectangleIntoTargetBuffer() throws Exception {
        Product product = TestUtils.createProduct("test", 12, 10);
        Rectangle dataRectangle = new Rectangle(1, 2, 5, 4);
        Rectangle computeRectangle = new Rectangle(3, 3, 2, 2);
        TileGeoreferencing tileGeoRef = new TileGeoreferencing(product, dataRectangle.x, dataRectangle.y, dataRectangle.width, dataRectangle.height);

        ProductData targetData = ProductData.createInstance(ProductData.TYPE_FLOAT32, dataRectangle.width * dataRectangle.height);
        final double sentinel = -12345.0;
        for (int i = 0; i < targetData.getNumElems(); i++) {
            targetData.setElemDoubleAt(i, sentinel);
        }

        FakeElevationModel dem = new FakeElevationModel(-999.0f);
        boolean valid = DEMFactory.fillElevationData(dem, dem.getDescriptor().getNoDataValue(), tileGeoRef,
                dataRectangle, computeRectangle, targetData, true, ProgressMonitor.NULL);

        assertTrue(valid);

        GeoPos expectedGeoPos = new GeoPos();
        for (int y = dataRectangle.y; y < dataRectangle.y + dataRectangle.height; y++) {
            for (int x = dataRectangle.x; x < dataRectangle.x + dataRectangle.width; x++) {
                int index = (y - dataRectangle.y) * dataRectangle.width + x - dataRectangle.x;
                if (computeRectangle.contains(x, y)) {
                    tileGeoRef.getGeoPos(x, y, expectedGeoPos);
                    assertEquals(elevationFor(expectedGeoPos), targetData.getElemDoubleAt(index), 1.0e-4);
                } else {
                    assertEquals(sentinel, targetData.getElemDoubleAt(index), 0.0);
                }
            }
        }
    }

    @Test
    @STTM("SNAP-4213")
    public void addElevationTileSizeUsesConfiguredPixelLimit() {
        Preferences preferences = Config.instance().preferences();
        String previousTileSize = preferences.get(ADD_ELEVATION_TILE_SIZE_KEY, null);
        String previousMaxDegrees = preferences.get(MAX_DEGREES_PER_ELEVATION_TILE_KEY, null);
        try {
            preferences.putInt(ADD_ELEVATION_TILE_SIZE_KEY, 64);
            preferences.putDouble(MAX_DEGREES_PER_ELEVATION_TILE_KEY, 2.0);

            Dimension tileSize = DEMFactory.getAddElevationTileSize(TestUtils.createProduct("test", 200, 150));

            assertEquals(new Dimension(64, 64), tileSize);
        } finally {
            restorePreference(preferences, ADD_ELEVATION_TILE_SIZE_KEY, previousTileSize);
            restorePreference(preferences, MAX_DEGREES_PER_ELEVATION_TILE_KEY, previousMaxDegrees);
        }
    }

    @Test
    @STTM("SNAP-4213")
    public void addElevationOverviewTileSizeUsesConfiguredGeographicLimit() throws Exception {
        Preferences preferences = Config.instance().preferences();
        String previousTileSize = preferences.get(ADD_ELEVATION_TILE_SIZE_KEY, null);
        String previousMaxDegrees = preferences.get("snap.dem.maxDegreesPerElevationOverviewTile", null);
        try {
            preferences.putInt(ADD_ELEVATION_TILE_SIZE_KEY, 128);
            preferences.putDouble("snap.dem.maxDegreesPerElevationOverviewTile", 24.0);

            Product product = TestUtils.createProduct("test", 200, 200);
            product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                    product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                    10.0, 50.0, 0.1, -0.25, 0.0, 0.0));

            Dimension tileSize = DEMFactory.getAddElevationOverviewTileSize(product);

            assertEquals(new Dimension(128, 96), tileSize);
        } finally {
            restorePreference(preferences, ADD_ELEVATION_TILE_SIZE_KEY, previousTileSize);
            restorePreference(preferences, "snap.dem.maxDegreesPerElevationOverviewTile", previousMaxDegrees);
        }
    }

    @Test
    @STTM("SNAP-4213")
    public void addElevationTileSizeLimitsGeographicExtent() throws Exception {
        Preferences preferences = Config.instance().preferences();
        String previousTileSize = preferences.get(ADD_ELEVATION_TILE_SIZE_KEY, null);
        String previousMaxDegrees = preferences.get(MAX_DEGREES_PER_ELEVATION_TILE_KEY, null);
        try {
            preferences.putInt(ADD_ELEVATION_TILE_SIZE_KEY, 128);
            preferences.putDouble(MAX_DEGREES_PER_ELEVATION_TILE_KEY, 2.0);

            Product product = TestUtils.createProduct("test", 100, 100);
            product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                    product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                    10.0, 50.0, 0.1, -0.2, 0.0, 0.0));

            Dimension tileSize = DEMFactory.getAddElevationTileSize(product);

            assertEquals(new Dimension(20, 10), tileSize);
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

    private static double elevationFor(GeoPos geoPos) {
        return geoPos.lat * 10.0 + geoPos.lon;
    }

    private static final class FakeElevationModel implements ElevationModel {
        private final ElevationModelDescriptor descriptor;

        private FakeElevationModel(float noDataValue) {
            descriptor = new FakeDescriptor(noDataValue);
        }

        @Override
        public ElevationModelDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public double getElevation(GeoPos geoPos) {
            return elevationFor(geoPos);
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
            return y * 10.0 + x;
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
            return new File(".");
        }
    }
}
