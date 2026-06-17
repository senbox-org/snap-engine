package org.esa.snap.dem.dataio;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import java.awt.Rectangle;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class DEMFactoryFillTest {


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
