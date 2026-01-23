package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import javax.xml.crypto.Data;
import java.io.IOException;

import static eu.esa.snap.core.dataio.cache.CacheTestUtil.createPreparedBuffer;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class CacheData3DTest {

    @Test
    @STTM("SNAP-4121")
    public void testIntersects_z() {
        int[] offsets = new int[]{10, 200, 300};
        int[] shapes = new int[]{20, 50, 50};
        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);
        // z ranges from 10 - 29

        // outside bottom
        assertFalse(cacheData3D.intersects_z(0, 9));

        // inside
        assertTrue(cacheData3D.intersects_z(9, 12));
        assertTrue(cacheData3D.intersects_z(22, 27));
        assertTrue(cacheData3D.intersects_z(27, 32));

        // contains
        assertTrue(cacheData3D.intersects_z(9, 32));

        // outside top
        assertFalse(cacheData3D.intersects_z(41, 58));
    }

    @Test
    @STTM("SNAP-4121")
    public void testIntersects_y() {
        int[] offsets = new int[]{10, 200, 300};
        int[] shapes = new int[]{20, 50, 50};
        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);
        // y ranges from 200 - 249

        // outside front
        assertFalse(cacheData3D.intersects_y(0, 9));

        // inside
        assertTrue(cacheData3D.intersects_y(199, 212));
        assertTrue(cacheData3D.intersects_y(222, 227));
        assertTrue(cacheData3D.intersects_y(248, 255));

        // contains
        assertTrue(cacheData3D.intersects_y(198, 255));

        // outside back
        assertFalse(cacheData3D.intersects_y(260, 300));
    }

    @Test
    @STTM("SNAP-4121")
    public void testIntersects_x() {
        int[] offsets = new int[]{10, 200, 300};
        int[] shapes = new int[]{20, 50, 50};
        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);
        // x ranges from 300 - 349

        // outside left
        assertFalse(cacheData3D.intersects_x(0, 9));

        // inside
        assertTrue(cacheData3D.intersects_x(299, 312));
        assertTrue(cacheData3D.intersects_x(322, 327));
        assertTrue(cacheData3D.intersects_x(348, 355));

        // contains
        assertTrue(cacheData3D.intersects_x(250, 400));

        // outside right
        assertFalse(cacheData3D.intersects_x(360, 400));
    }

    @Test
    @STTM("SNAP-4121")
    public void testIntersects() {
        int[] offsets = new int[]{20, 50, 100};
        int[] shapes = new int[]{20, 100, 100};
        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);

        // fully inside
        offsets = new int[]{23, 55, 108};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects front only
        offsets = new int[]{20, 45, 100};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects front and left
        offsets = new int[]{20, 45, 95};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects top and left
        offsets = new int[]{12, 50, 95};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects bottom and back
        offsets = new int[]{35, 70, 100};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects bottom and right
        offsets = new int[]{35, 50, 195};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects top front right corner
        offsets = new int[]{15, 45, 195};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // intersects bottom back left corner
        offsets = new int[]{35, 145, 95};
        shapes = new int[]{10, 10, 10};
        assertTrue(cacheData3D.intersects(offsets, shapes));

        // outsiders ----------------------------------------
        // --------------------------------------------------

        // outside front
        offsets = new int[]{20, 40, 100};
        shapes = new int[]{10, 10, 10};
        assertFalse(cacheData3D.intersects(offsets, shapes));

        // outside left
        offsets = new int[]{20, 50, 80};
        shapes = new int[]{10, 10, 10};
        assertFalse(cacheData3D.intersects(offsets, shapes));

        // outside top
        offsets = new int[]{5, 50, 100};
        shapes = new int[]{10, 10, 10};
        assertFalse(cacheData3D.intersects(offsets, shapes));

        // outside bottom
        offsets = new int[]{40, 50, 100};
        shapes = new int[]{10, 10, 10};
        assertFalse(cacheData3D.intersects(offsets, shapes));

        // outside back
        offsets = new int[]{20, 150, 100};
        shapes = new int[]{10, 10, 10};
        assertFalse(cacheData3D.intersects(offsets, shapes));

        // outside right
        offsets = new int[]{20, 50, 200};
        shapes = new int[]{10, 10, 10};
        assertFalse(cacheData3D.intersects(offsets, shapes));
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetBoundingCuboid() {
        int[] offsets = new int[]{100, 2600, 1800};
        int[] shapes = new int[]{50, 200, 300};
        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);

        final Cuboid bounds = cacheData3D.getBoundingCuboid();
        assertEquals(100, bounds.getZ());
        assertEquals(2600, bounds.getY());
        assertEquals(1800, bounds.getX());
        assertEquals(50, bounds.getDepth());
        assertEquals(200, bounds.getHeight());
        assertEquals(300, bounds.getWidth());
    }

    @Test
    @STTM("SNAP-4121")
    public void testCopyDataBuffer_requestCompletelyInCache() {
        // dimension 10 x 10 x 20 (z, y, x)
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_INT16, 2000);

        // 5x5x10 upper left corner to upper left corner, short
        ProductData targetData = ProductData.createInstance(ProductData.TYPE_INT16, 250);
        int[] srcOffsets = new int[]{0, 0, 0};
        int[] srcShapes = new int[]{10, 10, 20};
        DataBuffer dataBuffer = new DataBuffer(cacheData, srcOffsets, srcShapes);

        int[] targetOffsets = new int[]{0, 0, 0};
        int[] targetShapes = new int[]{5, 5, 10};
        int[] targetBufferSizes = new int[]{5, 5, 10};
        DataBuffer targetBuffer = new DataBuffer(targetData, targetOffsets, targetBufferSizes);
        CacheData3D.copyDataBuffer(srcOffsets, dataBuffer, targetOffsets, targetShapes, targetBuffer);

        assertEquals(0, targetData.getElemIntAt(0));
        assertEquals(1, targetData.getElemIntAt(1));
        assertEquals(5, targetData.getElemIntAt(5));
        assertEquals(20, targetData.getElemIntAt(10));
        assertEquals(200, targetData.getElemIntAt(100));
        assertEquals(344, targetData.getElemIntAt(174));
        assertEquals(489, targetData.getElemIntAt(249));

        // 1x5x10 upper left corner, layer 3 to upper left corner, short
        targetData = ProductData.createInstance(ProductData.TYPE_INT16, 250);
        srcOffsets = new int[]{2, 0, 0};
        srcShapes = new int[]{10, 10, 20};
        dataBuffer = new DataBuffer(cacheData, srcOffsets, srcShapes);

        targetOffsets = new int[]{0, 0, 0};
        targetShapes = new int[]{1, 5, 10};
        targetBufferSizes = new int[]{5, 5, 10};
        targetBuffer = new DataBuffer(targetData, targetOffsets, targetBufferSizes);
        CacheData3D.copyDataBuffer(srcOffsets, dataBuffer, targetOffsets, targetShapes, targetBuffer);

        assertEquals(400, targetData.getElemIntAt(0));
        assertEquals(401, targetData.getElemIntAt(1));
        assertEquals(405, targetData.getElemIntAt(5));
        assertEquals(420, targetData.getElemIntAt(10));
        assertEquals(489, targetData.getElemIntAt(49));
        assertEquals(0, targetData.getElemIntAt(50));
        assertEquals(0, targetData.getElemIntAt(249));
    }

    @Test
    @STTM("SNAP-4121")
    public void testCopyDataBuffer_front() {
        // dimension 10 x 10 x 20 (z, y, x)
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_INT32, 2000);
        int[] srcOffsets = new int[]{0, 1, 0};
        int[] srcShapes = new int[]{10, 10, 20};
        DataBuffer dataBuffer = new DataBuffer(cacheData, srcOffsets, srcShapes);

        // 5x5x10 intersecting front, one layer
        ProductData targetData = ProductData.createInstance(ProductData.TYPE_INT32, 250);

        int[] targetOffsets = new int[]{0, 0, 0};
        int[] targetShapes = new int[]{1, 5, 10};
        int[] targetBufferSizes = new int[]{5, 5, 10};
        final DataBuffer targetBuffer = new DataBuffer(targetData, targetOffsets, targetBufferSizes);
        CacheData3D.copyDataBuffer(srcOffsets, dataBuffer, targetOffsets, targetShapes, targetBuffer);

        assertEquals(20, targetData.getElemIntAt(0));
        assertEquals(21, targetData.getElemIntAt(1));
        assertEquals(26, targetData.getElemIntAt(6));
        assertEquals(109, targetData.getElemIntAt(49));
        assertEquals(0, targetData.getElemIntAt(50));
    }

    @Test
    @STTM("SNAP-4121")
    public void testCopyDataBuffer_topLeft() {
        // dimension 10 x 10 x 20 (z, y, x)
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_FLOAT32, 2000);
        int[] srcOffsets = new int[]{0, 0, 0};
        int[] srcShapes = new int[]{10, 10, 20};
        DataBuffer dataBuffer = new DataBuffer(cacheData, srcOffsets, srcShapes);

        // 2x5x10 intersecting top left, two layers
        ProductData targetData = ProductData.createInstance(ProductData.TYPE_FLOAT32, 100);

        int[] targetOffsets = new int[]{0, 2, 0};
        int[] targetShapes = new int[]{2, 3, 10};
        int[] targetBufferSizes = new int[]{2, 5, 10};
        final DataBuffer targetBuffer = new DataBuffer(targetData, targetOffsets, targetBufferSizes);
        CacheData3D.copyDataBuffer(srcOffsets, dataBuffer, targetOffsets, targetShapes, targetBuffer);

        assertEquals(0, targetData.getElemIntAt(0));
        assertEquals(0, targetData.getElemIntAt(1));
        assertEquals(0, targetData.getElemIntAt(19));
        assertEquals(1, targetData.getElemIntAt(21));
        assertEquals(49, targetData.getElemIntAt(49));
        assertEquals(60, targetData.getElemIntAt(50));
        assertEquals(100, targetData.getElemIntAt(70));
        assertEquals(101, targetData.getElemIntAt(71));
    }

    @Test
    @STTM("SNAP-4121")
    public void testCopyDataBuffer_bottomRight() {
        // dimension 10 x 10 x 20 (z, y, x)
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_FLOAT32, 2000);
        int[] srcOffsets = new int[]{8, 8, 18};
        int[] srcShapes = new int[]{10, 10, 20};
        DataBuffer dataBuffer = new DataBuffer(cacheData, srcOffsets, srcShapes);

        // 3x5x10 intersecting top left, two layers
        ProductData targetData = ProductData.createInstance(ProductData.TYPE_FLOAT32, 100);
        int[] targetOffsets = new int[]{0, 0, 0};
        int[] targetShapes = new int[]{2, 2, 2};
        int[] targetBufferSizes = new int[]{2, 5, 10};
        final DataBuffer targetBuffer = new DataBuffer(targetData, targetOffsets, targetBufferSizes);
        CacheData3D.copyDataBuffer(srcOffsets, dataBuffer, targetOffsets, targetShapes, targetBuffer);

        assertEquals(1778, targetData.getElemIntAt(0));
        assertEquals(1779, targetData.getElemIntAt(1));
        assertEquals(0, targetData.getElemIntAt(2));
        assertEquals(1798, targetData.getElemIntAt(10));
        assertEquals(1799, targetData.getElemIntAt(11));
        assertEquals(0, targetData.getElemIntAt(49));
        assertEquals(0, targetData.getElemIntAt(50));
        assertEquals(0, targetData.getElemIntAt(70));
    }

    @Test
    @STTM("SNAP-4121")
    public void testCopyDataBuffer_intersectBack() {
        // dimension 10 x 10 x 20 (z, y, x)
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_FLOAT64, 2000);
        int[] srcOffsets = new int[]{2, 8, 2};
        int[] srcShapes = new int[]{10, 10, 20};
        DataBuffer dataBuffer = new DataBuffer(cacheData, srcOffsets, srcShapes);

        // 5x5x10 intersecting back
        ProductData targetData = ProductData.createInstance(ProductData.TYPE_FLOAT64, 250);
        int[] targetOffsets = new int[]{0, 0, 0};
        int[] targetShapes = new int[]{5, 2, 10};
        int[] targetBufferSizes = new int[]{5, 5, 10};
        final DataBuffer targetBuffer = new DataBuffer(targetData, targetOffsets, targetBufferSizes);
        CacheData3D.copyDataBuffer(srcOffsets, dataBuffer, targetOffsets, targetShapes, targetBuffer);

        assertEquals(562, targetData.getElemIntAt(0));
        assertEquals(563, targetData.getElemIntAt(1));
        assertEquals(668, targetData.getElemIntAt(56));
        assertEquals(751, targetData.getElemIntAt(99));
        assertEquals(0, targetData.getElemIntAt(100));
        assertEquals(0, targetData.getElemIntAt(249));
    }

    @Test
    @STTM("SNAP-4121")
    public void testEnsureData() throws IOException {
        final CacheDataProvider cacheDataProvider = new MockProvider(ProductData.TYPE_INT64);
        final int[] offsets = new int[]{0, 0, 0};
        final int[] shapes = new int[]{30, 40, 40};

        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);
        final CacheContext cacheContext = new CacheContext(new VariableDescriptor(), cacheDataProvider);
        cacheData3D.setCacheContext(cacheContext);
        assertNull(cacheData3D.getData());

        final DataBuffer dataBuffer = new DataBuffer(ProductData.TYPE_INT64, new int[]{0, 0, 0}, new int[]{4, 10, 10});
        cacheData3D.copyData(new int[]{0, 0, 0}, new int[]{0, 0, 0}, new int[]{4, 5, 5}, dataBuffer);
        assertNotNull(cacheData3D.getData());
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetSizeInBytes() throws IOException {
        final int[] offsets = new int[]{400, 350, 200};
        final int[] shapes = new int[]{10, 10, 20};
        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);

        assertEquals(384, cacheData3D.getSizeInBytes());

        // trigger reading the buffer
        final CacheDataProvider cacheDataProvider = new MockProvider(ProductData.TYPE_INT64);
        final CacheContext cacheContext = new CacheContext(new VariableDescriptor(), cacheDataProvider);
        cacheData3D.setCacheContext(cacheContext);

        final DataBuffer dataBuffer = new DataBuffer(ProductData.TYPE_INT64, new int[]{0, 0, 0}, new int[]{4, 10, 10});
        cacheData3D.copyData(new int[]{0, 0, 0}, new int[]{1, 0, 0}, new int[]{2, 5, 5}, dataBuffer);

        // default size plus 2000 longs (10x10x20)
        assertEquals(16384, cacheData3D.getSizeInBytes());
    }
}
