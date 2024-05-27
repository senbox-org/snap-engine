package eu.esa.opt.snap.core.datamodel.band;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

public class SparseDataBandTest {

    @Test
    @STTM("SNAP-1691")
    public void testWritePixels() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT32, 3, 5, new NoDataProvider());

        try {
            band.writePixels(0, 0, 2, 2, new int[0], null);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }

        try {
            band.writePixels(0, 0, 2, 2, new float[0], null);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }

        try {
            band.writePixels(0, 0, 2, 2, new double[0], null);
            fail("IllegalStateException expected");
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

        assertEquals(-2, band.getPixelInt(3, 4));
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

    @Test
    @STTM("SNAP-1691")
    public void testGetPixel_threeDataPoints_scaled() {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT16, 4, 6, new ThreeInt16DataProvider());
        band.setNoDataValue(-999);
        band.setScalingFactor(0.5);
        band.setScalingOffset(-6.0);

        assertEquals(9, band.getPixelInt(1, 2));
        assertEquals(-22.f, band.getPixelFloat(2, 2), 1e-8);
        assertEquals(48.5, band.getPixelDouble(2, 3), 1e-8);
    }

    @Test
    @STTM("SNAP-1691")
    public void testReadPixels_noData_int() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT16, 4, 6, new ThreeInt16DataProvider());
        band.setNoDataValue(-1);

        final int[] expected = {-1, -1, -1, -1};
        final int[] targetData = new int[4];
        band.readPixels(0, 0, 2, 2, targetData, ProgressMonitor.NULL);

        assertArrayEquals(expected, targetData);
    }

    @Test
    @STTM("SNAP-1691")
    public void testReadPixels_noData_float() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT32, 4, 6, new ThreeInt16DataProvider());
        band.setNoDataValue(Float.MIN_VALUE);

        final float[] expected = {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
        final float[] targetData = new float[4];
        band.readPixels(0, 0, 2, 2, targetData, ProgressMonitor.NULL);

        assertArrayEquals(expected, targetData, 1e-8F);
    }

    @Test
    @STTM("SNAP-1691")
    public void testReadPixels_noData_double() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT64, 4, 6, new ThreeInt16DataProvider());
        band.setNoDataValue(Double.NaN);

        final double[] expected = {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
        final double[] targetData = new double[4];
        band.readPixels(0, 0, 2, 2, targetData, ProgressMonitor.NULL);

        assertArrayEquals(expected, targetData, 1e-8);
    }

    @Test
    @STTM("SNAP-1691")
    public void testReadPixels_oneDataPoint_int() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT16, 4, 6, new OneDataProvider());
        band.setNoDataValue(-1);

        final int[] expected = {-1, -2, -1, -1};
        final int[] targetData = new int[4];
        band.readPixels(2, 4, 2, 2, targetData, ProgressMonitor.NULL);

        assertArrayEquals(expected, targetData);
    }

    @Test
    @STTM("SNAP-1691")
    public void testReadPixels_oneDataPoint_float() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT32, 4, 6, new OneDataProvider());
        band.setNoDataValue(Float.MIN_VALUE);

        final float[] expected = {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, -1.67f};
        final float[] targetData = new float[4];
        band.readPixels(2, 3, 2, 2, targetData, ProgressMonitor.NULL);

        assertArrayEquals(expected, targetData, 1e-8F);
    }

    @Test
    @STTM("SNAP-1691")
    public void testReadPixels_oneDataPoint_double() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT64, 4, 6, new OneDataProvider());
        band.setNoDataValue(Double.NaN);

        final double[] expected = {Double.NaN, Double.NaN, -1.67, Double.NaN, Double.NaN, Double.NaN};
        final double[] targetData = new double[6];
        band.readPixels(1, 4, 3, 2, targetData, ProgressMonitor.NULL);

        assertArrayEquals(expected, targetData, 1e-8);
    }

    @Test
    @STTM("SNAP-1691")
    public void testReadPixels_threeDataPoints_int() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT16, 4, 6, new ThreeInt16DataProvider());
        band.setNoDataValue(-1234);

        final int[] expected = {-1234, 30, -32,
                -1234, -1234, 109};
        final int[] targetData = new int[6];
        band.readPixels(0, 2, 3, 2, targetData, ProgressMonitor.NULL);

        assertArrayEquals(expected, targetData);
    }

    @Test
    @STTM("SNAP-1691")
    public void testReadPixels_threeDataPoints_int_partlyCovered() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT16, 4, 6, new ThreeInt16DataProvider());
        band.setNoDataValue(-99);

        final int[] expected = {-99, -99, -32, -99};
        final int[] targetData = new int[4];
        band.readPixels(2, 1, 2, 2, targetData, ProgressMonitor.NULL);

        assertArrayEquals(expected, targetData);
    }

    @Test
    @STTM("SNAP-1691")
    public void testReadPixels_threeDataPoints_float_scaled() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT32, 4, 6, new ThreeInt16DataProvider());
        band.setNoDataValue(Float.NaN);
        band.setScalingFactor(0.5);
        band.setScalingOffset(-11.34);

        final float[] expected = {3.66f, -27.34f, Float.NaN, 43.16f};
        final float[] targetData = new float[4];
        band.readPixels(1, 2, 2, 2, targetData, ProgressMonitor.NULL);

        assertArrayEquals(expected, targetData, 1e-8F);
    }


    @Test
    @STTM("SNAP-1691")
    public void testGetPixels_oneDataPoint_int() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT16, 4, 6, new OneDataProvider());
        band.setNoDataValue(-1);
        band.ensureRasterData();

        final int[] expected = {-1, -2, -1, -1};
        final int[] targetData = new int[4];
        band.getPixels(2, 4, 2, 2, targetData, ProgressMonitor.NULL);

        assertArrayEquals(expected, targetData);
    }

    @Test
    @STTM("SNAP-1691")
    public void testGetPixels_int_notLoaded() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT16, 4, 6, new OneDataProvider());

        try {
            final int[] targetData = new int[4];
            band.getPixels(2, 4, 2, 2, targetData, ProgressMonitor.NULL);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    @STTM("SNAP-1691")
    public void testGetPixels_oneDataPoint_float() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT32, 4, 6, new OneDataProvider());
        band.setNoDataValue(Float.MIN_VALUE);
        band.ensureRasterData();

        final float[] expected = {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, -1.67f};
        final float[] targetData = new float[4];
        band.getPixels(2, 3, 2, 2, targetData, ProgressMonitor.NULL);

        assertArrayEquals(expected, targetData, 1e-8F);
    }

    @Test
    @STTM("SNAP-1691")
    public void testGetPixels_float_notLoaded() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT32, 4, 6, new OneDataProvider());

        try {
            final float[] targetData = new float[4];
            band.getPixels(2, 4, 2, 2, targetData, ProgressMonitor.NULL);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    @STTM("SNAP-1691")
    public void testGetPixels_oneDataPoint_double() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT64, 4, 6, new OneDataProvider());
        band.setNoDataValue(Double.NaN);
        band.ensureRasterData();

        final double[] expected = {Double.NaN, Double.NaN, -1.67, Double.NaN, Double.NaN, Double.NaN};
        final double[] targetData = new double[6];
        band.getPixels(1, 4, 3, 2, targetData, ProgressMonitor.NULL);

        assertArrayEquals(expected, targetData, 1e-8);
    }

    @Test
    @STTM("SNAP-1691")
    public void testGetPixels_double_notLoaded() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT64, 4, 6, new OneDataProvider());

        try {
            final double[] targetData = new double[4];
            band.getPixels(2, 4, 2, 2, targetData, ProgressMonitor.NULL);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    @STTM("SNAP-1691")
    public void testReadRasterData_oneDataPoint_int() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT16, 4, 6, new OneDataProvider());
        band.setNoDataValue(-1);

        final int[] expected = {-1, -2, -1, -1};
        final int[] targetData = new int[4];
        final ProductData productData = ProductData.createInstance(targetData);
        band.readRasterData(2, 4, 2, 2, productData, ProgressMonitor.NULL);

        assertArrayEquals(expected, (int[]) productData.getElems());
    }

    @Test
    @STTM("SNAP-1691")
    public void testReadRasterData_oneDataPoint_float() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT32, 4, 6, new OneDataProvider());
        band.setNoDataValue(Float.MIN_VALUE);

        final float[] expected = {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, -1.67f};
        final float[] targetData = new float[4];
        final ProductData productData = ProductData.createInstance(targetData);
        band.readRasterData(2, 3, 2, 2, productData, ProgressMonitor.NULL);

        assertArrayEquals(expected, (float[]) productData.getElems(), 1e-8F);
    }

    @Test
    @STTM("SNAP-1691")
    public void testReadRasterData_oneDataPoint_double() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT64, 4, 6, new OneDataProvider());
        band.setNoDataValue(Double.NaN);

        final double[] expected = {Double.NaN, Double.NaN, -1.67, Double.NaN, Double.NaN, Double.NaN};
        final double[] targetData = new double[6];
        final ProductData productData = ProductData.createInstance(targetData);
        band.readRasterData(1, 4, 3, 2, productData, ProgressMonitor.NULL);

        assertArrayEquals(expected, (double[]) productData.getElems(), 1e-8);
    }

    @Test
    @STTM("SNAP-1691")
    public void testReadRasterData_oneDataPoint_invalidTargetType() throws IOException {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT64, 4, 6, new OneDataProvider());
        final ProductData productData = ProductData.createInstance(new byte[6]);

        try {
            band.readRasterData(1, 4, 3, 2, productData, ProgressMonitor.NULL);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    @STTM("SNAP-1691")
    public void testGetRasterData_int()  {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_INT32, 4, 6, new OneDataProvider());
        band.setNoDataValue(-1);

        final ProductData rasterData = band.getRasterData();
        assertEquals(ProductData.TYPE_INT32, rasterData.getType());
        assertEquals(24, rasterData.getNumElems());
        assertEquals(-1, rasterData.getElemIntAt(0));
        assertEquals(-2, rasterData.getElemIntAt(19));
    }

    @Test
    @STTM("SNAP-1691")
    public void testGetRasterData_double()  {
        final SparseDataBand band = new SparseDataBand("test", ProductData.TYPE_FLOAT64, 5, 5, new OneDataProvider());
        band.setNoDataValue(Double.NaN);

        final ProductData rasterData = band.getRasterData();
        assertEquals(ProductData.TYPE_FLOAT64, rasterData.getType());
        assertEquals(25, rasterData.getNumElems());
        assertEquals(Double.NaN, rasterData.getElemDoubleAt(1), 1e-8);
        assertEquals(-1.67, rasterData.getElemDoubleAt(23), 1e-8);
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

    private static class ThreeInt16DataProvider implements SparseDataProvider {
        @Override
        public DataPoint[] get() {
            final DataPoint[] points = new DataPoint[3];
            points[0] = new DataPoint(1, 2, 30);
            points[1] = new DataPoint(2, 2, -32);
            points[2] = new DataPoint(2, 3, 109);
            return points;
        }
    }
}
