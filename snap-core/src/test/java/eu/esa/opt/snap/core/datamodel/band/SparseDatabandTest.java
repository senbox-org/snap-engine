package eu.esa.opt.snap.core.datamodel.band;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.AbstractBand;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class SparseDatabandTest {

    @Test
    @STTM("SNAP-1691")
    public void testWritePixels() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT32, 3, 5, new NoDataProvider());

        try {
            band.writePixels(0, 0, 2, 2, new int[0], null);
        } catch (IllegalStateException expected) {
        }

        try {
            band.writePixels(0, 0, 2, 2, new float[0], null);
        } catch (IllegalStateException expected) {
        }

        try {
            band.writePixels(0, 0, 2, 2, new double[0], null);
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    @STTM("SNAP-1691")
    public void testGetRasterDimensions() {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT32, 6, 7, new NoDataProvider());

        assertEquals(6, band.getRasterWidth());
        assertEquals(7, band.getRasterHeight());
    }

    @Test
    @STTM("SNAP-1691")
    public void testGetRawStorageSize() {
        SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT32, 6, 7, new NoDataProvider());
        long storageSize = band.getRawStorageSize(null);
        assertEquals(6 * 7 * 4, storageSize);

        band = new SparseDataBand("test", ProductData.TYPE_FLOAT64, 5, 7, new NoDataProvider());
        storageSize = band.getRawStorageSize(null);
        assertEquals(5 * 7 * 8, storageSize);

        ProductSubsetDef subsetDef = new ProductSubsetDef();

        // band is not contained in subset
        band = new SparseDataBand("test", ProductData.TYPE_UINT8, 5, 7, new NoDataProvider());
        storageSize = band.getRawStorageSize(subsetDef);
        assertEquals(0, storageSize);

        // band is contained in subset
        subsetDef.setNodeNames(new String[]{"test"});
        band = new SparseDataBand("test", ProductData.TYPE_UINT8, 6, 8, new NoDataProvider());
        storageSize = band.getRawStorageSize(subsetDef);
        assertEquals(6 * 8, storageSize);

        // with subsampling
        subsetDef.setSubSampling(2, 2);
        band = new SparseDataBand("test", ProductData.TYPE_UINT8, 6, 8, new NoDataProvider());
        storageSize = band.getRawStorageSize(subsetDef);
        assertEquals(3 * 4, storageSize);

        // with region
        subsetDef.setSubSampling(1, 1);
        subsetDef.setRegion(1, 1, 2, 2);
        band = new SparseDataBand("test", ProductData.TYPE_UINT8, 6, 8, new NoDataProvider());
        storageSize = band.getRawStorageSize(subsetDef);
        assertEquals(2 * 2, storageSize);

        // with regionMap
        subsetDef = new ProductSubsetDef();
        subsetDef.setNodeNames(new String[]{"test"});
        final HashMap<String, Rectangle> regionmap = new HashMap<>();
        regionmap.put("test", new Rectangle(0, 0, 2, 3));
        subsetDef.setRegionMap(regionmap);
        band = new SparseDataBand("test", ProductData.TYPE_UINT8, 6, 8, new NoDataProvider());
        storageSize = band.getRawStorageSize(subsetDef);
        assertEquals(2 * 3, storageSize);
    }

    @Test
    @STTM("SNAP-1691")
    public void testGetPixel_noData() {
       final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT32, 5, 8, new NoDataProvider());
       band.setNoDataValue(-1);

        assertEquals(-1, band.getPixelInt(1, 1));
        assertEquals(-1.f, band.getPixelFloat(2, 2), 1e-8);
        assertEquals(-1.0, band.getPixelDouble(3, 3), 1e-8);
    }

    @Test
    @STTM("SNAP-1691")
    public void testGetPixel_oneDataPoint() {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT32, 4, 9, new OneDataProvider());
        band.setNoDataValue(-999);

        assertEquals(-1, band.getPixelInt(3, 4));
        assertEquals(-1.67f, band.getPixelFloat(3, 4), 1e-8);
        assertEquals(-1.67, band.getPixelDouble(3, 4), 1e-8);

        // off the one data point
        assertEquals(-999, band.getPixelInt(2, 4));
        assertEquals(-999.f, band.getPixelFloat(1, 4), 1e-8);
        assertEquals(-999.0, band.getPixelDouble(0, 4), 1e-8);
    }

    @Test
    @STTM("SNAP-1691")
    public void testGetPixel_threeDataPoints() {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT32, 4, 6, new ThreeDataProvider());
        band.setNoDataValue(Float.NaN);

        assertEquals(19, band.getPixelInt(1, 2));
        assertEquals(20.17f, band.getPixelFloat(2, 2), 1e-8);
        assertEquals(21.06, band.getPixelDouble(2, 3), 1e-8);

        // off the data points
        assertEquals(0, band.getPixelInt(3, 5));
        assertEquals(Float.NaN, band.getPixelFloat(2, 5), 1e-8);
        assertEquals(Double.NaN, band.getPixelDouble(1, 5), 1e-8);
    }


    private static class NoDataProvider implements SparseDataProvider {
        @Override
        public DataPoint[] get() {
            return new DataPoint[0];
        }
    }

    private static class OneDataProvider implements SparseDataProvider {
        @Override
        public DataPoint[] get() {
            final DataPoint[] points = new DataPoint[1];
            points[0] = new DataPoint(3, 4, -1.67);
            return points;
        }
    }

    private static class ThreeDataProvider implements SparseDataProvider {
        @Override
        public DataPoint[] get() {
            final DataPoint[] points = new DataPoint[3];
            points[0] = new DataPoint(1, 2, 19.3);
            points[1] = new DataPoint(2, 2, 20.17);
            points[2] = new DataPoint(2, 3, 21.06);
            return points;
        }
    }
}
