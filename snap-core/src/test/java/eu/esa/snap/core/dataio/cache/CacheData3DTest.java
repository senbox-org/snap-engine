package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static eu.esa.snap.core.dataio.cache.CacheTestUtil.createPreparedBuffer;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class CacheData3DTest {

    @Test
    public void testInside_z() {
        int[] offsets = new int[]{10, 200, 300};
        int[] shapes = new int[]{20, 50, 50};
        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);

        assertTrue(cacheData3D.inside_z(12));
        assertTrue(cacheData3D.inside_z(23));
        assertTrue(cacheData3D.inside_z(29));

        assertFalse(cacheData3D.inside_z(9));
        assertFalse(cacheData3D.inside_z(30));
    }

    @Test
    public void testInside_y() {
        final int[] offsets = new int[]{10, 200, 300};
        final int[] shapes = new int[]{20, 50, 50};
        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);

        assertTrue(cacheData3D.inside_y(214));
        assertTrue(cacheData3D.inside_y(236));
        assertTrue(cacheData3D.inside_y(249));

        assertFalse(cacheData3D.inside_y(199));
        assertFalse(cacheData3D.inside_y(250));
    }

    @Test
    public void testInside_x() {
        final int[] offsets = new int[]{10, 200, 300};
        final int[] shapes = new int[]{20, 50, 50};
        final CacheData3D cacheData3D = new CacheData3D(offsets, shapes);

        assertTrue(cacheData3D.inside_x(306));
        assertTrue(cacheData3D.inside_x(311));
        assertTrue(cacheData3D.inside_x(349));

        assertFalse(cacheData3D.inside_x(299));
        assertFalse(cacheData3D.inside_x(350));
    }

    @Test
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

    // bounding rect - do we need this in 3d world? Better a bounding cube.

    @Test
    public void testCopyDataBuffer_requestCompletelyInCache() {
        // dimension 10 x 10 x 20 (z, y, x)
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_INT16, 2000);

        // 5x5x10 upper left corner to upper left corner, short
        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_INT16, 250);
        int[] srcOffsets = new int[]{0, 0, 0};
        int[] targetOffsets = new int[]{0, 0, 0};
        int[] targetShapes = new int[]{5, 5, 10};
        int[] targetBufferSizes = new int[]{5, 5, 10};
        CacheData3D.copyDataBuffer(srcOffsets, 10, cacheData, targetOffsets, targetShapes, targetBufferSizes, targetBuffer);

        assertEquals(0, targetBuffer.getElemIntAt(0));
        assertEquals(1, targetBuffer.getElemIntAt(1));
        assertEquals(5, targetBuffer.getElemIntAt(5));
        assertEquals(10, targetBuffer.getElemIntAt(10));
        assertEquals(100, targetBuffer.getElemIntAt(100));
        assertEquals(174, targetBuffer.getElemIntAt(174));
        assertEquals(249, targetBuffer.getElemIntAt(249));

        // 1x5x10 upper left corner, layer 3 to upper left corner, short
        targetBuffer = ProductData.createInstance(ProductData.TYPE_INT16, 250);
        srcOffsets = new int[]{2, 0, 0};
        targetOffsets = new int[]{0, 0, 0};
        targetShapes = new int[]{1, 5, 10};
        targetBufferSizes = new int[]{5, 5, 10};
        CacheData3D.copyDataBuffer(srcOffsets, 10, cacheData, targetOffsets, targetShapes, targetBufferSizes, targetBuffer);

        assertEquals(100, targetBuffer.getElemIntAt(0));
        assertEquals(101, targetBuffer.getElemIntAt(1));
        assertEquals(105, targetBuffer.getElemIntAt(5));
        assertEquals(110, targetBuffer.getElemIntAt(10));
        assertEquals(149, targetBuffer.getElemIntAt(49));
        assertEquals(0, targetBuffer.getElemIntAt(50));
        assertEquals(0, targetBuffer.getElemIntAt(249));
    }

    @Test
    public void testCopyDataBuffer_front() {
        // dimension 10 x 10 x 20 (z, y, x)
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_INT32, 2000);

        // 5x5x10 intersecting front, one layer
        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_INT32, 250);
        int[] srcOffsets = new int[]{0, 1, 0};
        int[] targetOffsets = new int[]{0, 0, 0};
        int[] targetShapes = new int[]{1, 5, 10};
        int[] targetBufferSizes = new int[]{5, 5, 10};
        CacheData3D.copyDataBuffer(srcOffsets, 10, cacheData, targetOffsets, targetShapes, targetBufferSizes, targetBuffer);

        assertEquals(10, targetBuffer.getElemIntAt(0));
        assertEquals(11, targetBuffer.getElemIntAt(1));
        assertEquals(16, targetBuffer.getElemIntAt(6));
        assertEquals(59, targetBuffer.getElemIntAt(49));
        assertEquals(0, targetBuffer.getElemIntAt(50));
    }

    @Test
    public void testCopyDataBuffer_topLeft() {
        // dimension 10 x 10 x 20 (z, y, x)
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_FLOAT32, 2000);

        // 2x5x10 intersecting top left, two layers
        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_FLOAT32, 100);
        int[] srcOffsets = new int[]{0, 0, 0};
        int[] targetOffsets = new int[]{0, 2, 0};
        int[] targetShapes = new int[]{2, 3, 10};
        int[] targetBufferSizes = new int[]{2, 5, 10};
        CacheData3D.copyDataBuffer(srcOffsets, 10, cacheData, targetOffsets, targetShapes, targetBufferSizes, targetBuffer);

        assertEquals(0, targetBuffer.getElemIntAt(0));
        assertEquals(0, targetBuffer.getElemIntAt(1));
        assertEquals(0, targetBuffer.getElemIntAt(19));
        assertEquals(1, targetBuffer.getElemIntAt(21));
        assertEquals(29, targetBuffer.getElemIntAt(49));
        assertEquals(0, targetBuffer.getElemIntAt(50));
        assertEquals(30, targetBuffer.getElemIntAt(70));
        assertEquals(31, targetBuffer.getElemIntAt(71));
    }

    @Test
    public void testCopyDataBuffer_bottomRight() {
        // dimension 10 x 10 x 20 (z, y, x)
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_FLOAT32, 2000);

        // 3x5x10 intersecting top left, two layers
        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_FLOAT32, 150);
        int[] srcOffsets = new int[]{8, 8, 18};
        int[] targetOffsets = new int[]{0, 0, 0};
        int[] targetShapes = new int[]{2, 2, 2};
        int[] targetBufferSizes = new int[]{2, 5, 10};
        CacheData3D.copyDataBuffer(srcOffsets, 10, cacheData, targetOffsets, targetShapes, targetBufferSizes, targetBuffer);

        assertEquals(66, targetBuffer.getElemIntAt(0));
        assertEquals(67, targetBuffer.getElemIntAt(1));
        assertEquals(0, targetBuffer.getElemIntAt(2));
        assertEquals(76, targetBuffer.getElemIntAt(10));
        assertEquals(77, targetBuffer.getElemIntAt(11));
        assertEquals(0, targetBuffer.getElemIntAt(49));
        assertEquals(86, targetBuffer.getElemIntAt(50));
        assertEquals(0, targetBuffer.getElemIntAt(70));
    }

    @Test
    public void testCopyDataBuffer_intersectBack() {
        // dimension 10 x 10 x 20 (z, y, x)
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_FLOAT64, 2000);

        // 5x5x10 intersecting back
        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_FLOAT64, 250);
        int[] srcOffsets = new int[]{2, 8, 2};
        int[] targetOffsets = new int[]{0, 0, 0};
        int[] targetShapes = new int[]{5, 2, 10};
        int[] targetBufferSizes = new int[]{5, 5, 10};
        CacheData3D.copyDataBuffer(srcOffsets, 20, cacheData, targetOffsets, targetShapes, targetBufferSizes, targetBuffer);

        assertEquals(122, targetBuffer.getElemIntAt(0));
        assertEquals(123, targetBuffer.getElemIntAt(1));
        assertEquals(168, targetBuffer.getElemIntAt(56));
        assertEquals(0, targetBuffer.getElemIntAt(99));
        assertEquals(202, targetBuffer.getElemIntAt(100));
        assertEquals(0, targetBuffer.getElemIntAt(249));
    }
}
