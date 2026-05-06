package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class CachedSubsamplingReaderTest {


    private ProductCache cache;


    @Before
    public void setUp() {
        cache = new ProductCache(new MockProvider(ProductData.TYPE_INT32));
        cache.setMemoryUsageTracker(new TestMemoryUsageTracker());
    }


    @Test
    @STTM("SNAP-4190")
    public void testRead_step1_fastPath_noTempBuffer() throws Exception {
        final ProductData dest = ProductData.createInstance(ProductData.TYPE_INT32, 16);

        CachedSubsamplingReader.read(cache, "band", ProductData.TYPE_INT32, 0, 0, 4, 4, 1, 1, 4, 4, dest);

        assertEquals(0, dest.getElemIntAt(0));
        assertEquals(1, dest.getElemIntAt(1));
        assertEquals(2, dest.getElemIntAt(2));
        assertEquals(3, dest.getElemIntAt(3));
        assertEquals(10, dest.getElemIntAt(4));
        assertEquals(11, dest.getElemIntAt(5));
        assertEquals(20, dest.getElemIntAt(8));
        assertEquals(30, dest.getElemIntAt(12));
        assertEquals(33, dest.getElemIntAt(15));
    }

    @Test
    @STTM("SNAP-4190")
    public void testRead_step1_withOffset() throws Exception {
        final ProductData dest = ProductData.createInstance(ProductData.TYPE_INT32, 9);

        CachedSubsamplingReader.read(cache, "band", ProductData.TYPE_INT32, 3, 2, 3, 3, 1, 1, 3, 3, dest);

        assertEquals(23, dest.getElemIntAt(0));
        assertEquals(24, dest.getElemIntAt(1));
        assertEquals(25, dest.getElemIntAt(2));
        assertEquals(33, dest.getElemIntAt(3));
        assertEquals(43, dest.getElemIntAt(6));
    }

    @Test
    @STTM("SNAP-4190")
    public void testRead_stepX2_stepY1() throws Exception {
        final ProductData dest = ProductData.createInstance(ProductData.TYPE_INT32, 8);

        CachedSubsamplingReader.read(cache, "band", ProductData.TYPE_INT32, 0, 0, 4, 4, 2, 1, 2, 4, dest);

        assertEquals(0, dest.getElemIntAt(0));
        assertEquals(2, dest.getElemIntAt(1));
        assertEquals(10, dest.getElemIntAt(2));
        assertEquals(12, dest.getElemIntAt(3));
        assertEquals(20, dest.getElemIntAt(4));
        assertEquals(22, dest.getElemIntAt(5));
        assertEquals(30, dest.getElemIntAt(6));
        assertEquals(32, dest.getElemIntAt(7));
    }

    @Test
    @STTM("SNAP-4190")
    public void testRead_stepX1_stepY2() throws Exception {
        final ProductData dest = ProductData.createInstance(ProductData.TYPE_INT32, 8);

        CachedSubsamplingReader.read(cache, "band", ProductData.TYPE_INT32, 0, 0, 4, 4, 1, 2, 4, 2, dest);

        assertEquals(0, dest.getElemIntAt(0));
        assertEquals(1, dest.getElemIntAt(1));
        assertEquals(2, dest.getElemIntAt(2));
        assertEquals(3, dest.getElemIntAt(3));
        assertEquals(20, dest.getElemIntAt(4));
        assertEquals(21, dest.getElemIntAt(5));
        assertEquals(22, dest.getElemIntAt(6));
        assertEquals(23, dest.getElemIntAt(7));
    }

    @Test
    @STTM("SNAP-4190")
    public void testRead_stepX2_stepY2() throws Exception {

        final ProductData dest = ProductData.createInstance(ProductData.TYPE_INT32, 4);

        CachedSubsamplingReader.read(cache, "band", ProductData.TYPE_INT32, 0, 0, 4, 4, 2, 2, 2, 2, dest);

        assertEquals(0, dest.getElemIntAt(0));
        assertEquals(2, dest.getElemIntAt(1));
        assertEquals(20, dest.getElemIntAt(2));
        assertEquals(22, dest.getElemIntAt(3));
    }

    @Test
    @STTM("SNAP-4190")
    public void testRead_stepX3_stepY3() throws Exception {
        final ProductData dest = ProductData.createInstance(ProductData.TYPE_INT32, 4);

        CachedSubsamplingReader.read(cache, "band", ProductData.TYPE_INT32, 0, 0, 6, 6, 3, 3, 2, 2, dest);

        assertEquals(0, dest.getElemIntAt(0));
        assertEquals(3, dest.getElemIntAt(1));
        assertEquals(30, dest.getElemIntAt(2));
        assertEquals(33, dest.getElemIntAt(3));
    }

    @Test
    @STTM("SNAP-4190")
    public void testRead_subsampling_withOffset() throws Exception {
        final ProductData dest = ProductData.createInstance(ProductData.TYPE_INT32, 4);

        CachedSubsamplingReader.read(cache, "band", ProductData.TYPE_INT32, 1, 2, 4, 4, 2, 2, 2, 2, dest);

        assertEquals(21, dest.getElemIntAt(0));
        assertEquals(23, dest.getElemIntAt(1));
        assertEquals(41, dest.getElemIntAt(2));
        assertEquals(43, dest.getElemIntAt(3));
    }

    @Test
    @STTM("SNAP-4190")
    public void testRead_floatType_stepX2_stepY2() throws Exception {
        final ProductCache floatCache = new ProductCache(new MockProvider(ProductData.TYPE_FLOAT32));
        floatCache.setMemoryUsageTracker(new TestMemoryUsageTracker());

        final ProductData dest = ProductData.createInstance(ProductData.TYPE_FLOAT32, 4);

        CachedSubsamplingReader.read(floatCache, "band", ProductData.TYPE_FLOAT32, 0, 0, 4, 4, 2, 2, 2, 2, dest);

        assertEquals(0.0, dest.getElemDoubleAt(0), 1e-6);
        assertEquals(2.0, dest.getElemDoubleAt(1), 1e-6);
        assertEquals(20.0, dest.getElemDoubleAt(2), 1e-6);
        assertEquals(22.0, dest.getElemDoubleAt(3), 1e-6);
    }

    @Test
    @STTM("SNAP-4190")
    public void testRead_singlePixel_step1() throws Exception {
        final ProductData dest = ProductData.createInstance(ProductData.TYPE_INT32, 1);

        CachedSubsamplingReader.read(cache, "band", ProductData.TYPE_INT32, 7, 5, 1, 1, 1, 1, 1, 1, dest);

        assertEquals(57, dest.getElemIntAt(0));
    }

    @Test
    @STTM("SNAP-4190")
    public void testRead_singlePixel_withSubsampling() throws Exception {
        final ProductData dest = ProductData.createInstance(ProductData.TYPE_INT32, 1);

        CachedSubsamplingReader.read(cache, "band", ProductData.TYPE_INT32, 0, 0, 4, 4, 4, 4, 1, 1, dest);

        assertEquals(0, dest.getElemIntAt(0));
    }
}
