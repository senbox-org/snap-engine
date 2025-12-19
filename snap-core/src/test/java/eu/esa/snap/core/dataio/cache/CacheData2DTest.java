package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.*;
import java.io.IOException;

import static eu.esa.snap.core.dataio.cache.CacheTestUtil.createPreparedBuffer;
import static org.junit.Assert.*;

public class CacheData2DTest {

    @Test
    @STTM("SNAP-4107")
    public void testIntersects() {
        int[] offsets = new int[]{450, 100};
        int[] shapes = new int[]{100, 100};
        final CacheData2D cacheData2D = new CacheData2D(offsets, shapes);

        // inside
        offsets = new int[]{460, 120};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect left border
        offsets = new int[]{460, 90};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect upper left corner
        offsets = new int[]{440, 90};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect upper border
        offsets = new int[]{440, 110};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect upper right corner
        offsets = new int[]{440, 190};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect right border
        offsets = new int[]{460, 190};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect lower right corner
        offsets = new int[]{490, 190};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect lower border
        offsets = new int[]{490, 130};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));

        // intersect lower left corner
        offsets = new int[]{490, 90};
        shapes = new int[]{20, 20};
        assertTrue(cacheData2D.intersects(offsets, shapes));
    }

    @Test
    @STTM("SNAP-4107")
    public void testIntersects_outside() {
        int[] offsets = new int[]{450, 100};
        int[] shapes = new int[]{100, 100};
        final CacheData2D cacheData2D = new CacheData2D(offsets, shapes);

        // too far left
        offsets = new int[]{460, 0};
        shapes = new int[]{20, 20};
        assertFalse(cacheData2D.intersects(offsets, shapes));

        // too high
        offsets = new int[]{0, 120};
        shapes = new int[]{20, 20};
        assertFalse(cacheData2D.intersects(offsets, shapes));

        // too far right
        offsets = new int[]{460, 299};
        shapes = new int[]{20, 20};
        assertFalse(cacheData2D.intersects(offsets, shapes));

        // too low
        offsets = new int[]{599, 120};
        shapes = new int[]{20, 20};
        assertFalse(cacheData2D.intersects(offsets, shapes));
    }

    @Test
    @STTM("SNAP-4107")
    public void testIntersects_y() {
        final int[] offsets = new int[]{450, 100};
        final int[] shapes = new int[]{50, 100};
        final CacheData2D cacheData2D = new CacheData2D(offsets, shapes);
        // y ranges from 450 - 499

        // outside bottom
        assertFalse(cacheData2D.intersects_y(430, 439));

        // inside
        assertTrue(cacheData2D.intersects_y(445, 455));
        assertTrue(cacheData2D.intersects_y(470, 490));
        assertTrue(cacheData2D.intersects_y(490, 510));

        // contains
        assertTrue(cacheData2D.intersects_y(400, 550));

        // outside top
        assertFalse(cacheData2D.intersects_y(560, 600));
    }

    @Test
    @STTM("SNAP-4107")
    public void testIntersects_x() {
        final int[] offsets = new int[]{450, 100};
        final int[] shapes = new int[]{50, 100};
        final CacheData2D cacheData2D = new CacheData2D(offsets, shapes);
        // x ranges from 100 - 199

        // outside left
        assertFalse(cacheData2D.intersects_x(30, 39));

        // inside
        assertTrue(cacheData2D.intersects_x(90, 110));
        assertTrue(cacheData2D.intersects_x(110, 149));
        assertTrue(cacheData2D.intersects_x(190, 210));

        // contains
        assertTrue(cacheData2D.intersects_x(90, 250));

        // outside right
        assertFalse(cacheData2D.intersects_x(260, 300));
    }

    @Test
    @STTM("SNAP-4107")
    public void testGetBoundingRect() {
        final int[] offsets = new int[]{460, 150};
        final int[] shapes = new int[]{50, 100};
        final CacheData2D cacheData2D = new CacheData2D(offsets, shapes);

        final Rectangle rect = cacheData2D.getBoundingRect();
        assertEquals(150, rect.x);
        assertEquals(460, rect.y);
        assertEquals(100, rect.width);
        assertEquals(50, rect.height);
    }

    @Test
    @STTM("SNAP-4107")
    public void testCopyDataBuffer_requestCompletelyInCache() {
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_INT8, 300);
        final int cacheWidth = 15;
        int[] srcOffsets = new int[]{0, 0};

        // 10x10 upper left corner, byte
        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_INT8, 100);
        final int targetWidth = 10;
        int[] dstOffsets = new int[]{0, 0};
        int[] dstShapes = new int[]{10, 10};

        CacheData2D.copyDataBuffer(srcOffsets, cacheWidth, cacheData, dstOffsets, dstShapes, targetWidth, targetBuffer);
        assertEquals(0, targetBuffer.getElemIntAt(0));
        assertEquals(1, targetBuffer.getElemIntAt(1));
        assertEquals(15, targetBuffer.getElemIntAt(10));
        assertEquals(31, targetBuffer.getElemIntAt(21));

        // 10x10 shifted by 3 in x-dir, byte
        srcOffsets = new int[]{0, 3};
        dstOffsets = new int[]{0, 0};
        dstShapes = new int[]{10, 10};

        CacheData2D.copyDataBuffer(srcOffsets, cacheWidth, cacheData, dstOffsets, dstShapes, targetWidth, targetBuffer);
        assertEquals(3, targetBuffer.getElemIntAt(0));
        assertEquals(4, targetBuffer.getElemIntAt(1));
        assertEquals(18, targetBuffer.getElemIntAt(10));
        assertEquals(34, targetBuffer.getElemIntAt(21));
    }

    @Test
    @STTM("SNAP-4107")
    public void testCopyDataBuffer_outLeft() {
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_INT16, 300);
        final int cacheWidth = 15;
        int[] srcOffsets = new int[]{7, 0};

        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_INT16, 100);
        final int targetWidth = 10;
        int[] dstOffsets = new int[]{0, 2};
        int[] dstShapes = new int[]{10, 8};

        CacheData2D.copyDataBuffer(srcOffsets, cacheWidth, cacheData, dstOffsets, dstShapes, targetWidth, targetBuffer);
        assertEquals(0, targetBuffer.getElemIntAt(0));
        assertEquals(105, targetBuffer.getElemIntAt(2));
        assertEquals(120, targetBuffer.getElemIntAt(12));
        assertEquals(247, targetBuffer.getElemIntAt(99));
    }

    @Test
    @STTM("SNAP-4107")
    public void testCopyDataBuffer_upperLeftCorner() {
        // size: 20 x 15
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_INT32, 300);
        final int cacheWidth = 15;
        int[] srcOffsets = new int[]{0, 0};

        // size: 10 x 10
        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_INT32, 100);
        final int targetWidth = 10;
        int[] dstOffsets = new int[]{4, 3};
        int[] dstShapes = new int[]{6, 7};

        CacheData2D.copyDataBuffer(srcOffsets, cacheWidth, cacheData, dstOffsets, dstShapes, targetWidth, targetBuffer);
        assertEquals(0, targetBuffer.getElemIntAt(43));
        assertEquals(1, targetBuffer.getElemIntAt(44));
        assertEquals(75, targetBuffer.getElemIntAt(93));
        assertEquals(76, targetBuffer.getElemIntAt(94));
        assertEquals(81, targetBuffer.getElemIntAt(99));
    }

    @Test
    @STTM("SNAP-4107")
    public void testCopyDataBuffer_outTop() {
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_INT16, 300);
        final int cacheWidth = 15;
        int[] srcOffsets = new int[]{0, 4};

        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_INT16, 100);
        final int targetWidth = 10;
        int[] dstOffsets = new int[]{8, 0};
        int[] dstShapes = new int[]{2, 10};

        CacheData2D.copyDataBuffer(srcOffsets, cacheWidth, cacheData, dstOffsets, dstShapes, targetWidth, targetBuffer);
        assertEquals(0, targetBuffer.getElemIntAt(0));
        assertEquals(0, targetBuffer.getElemIntAt(1));
        assertEquals(4, targetBuffer.getElemIntAt(80));
        assertEquals(5, targetBuffer.getElemIntAt(81));
        assertEquals(28, targetBuffer.getElemIntAt(99));
    }

    @Test
    @STTM("SNAP-4107")
    public void testCopyDataBuffer_upperRightCorner() {
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_FLOAT32, 300);
        final int cacheWidth = 15;
        int[] srcOffsets = new int[]{0, 11};

        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_FLOAT32, 100);
        final int targetWidth = 10;
        int[] dstOffsets = new int[]{3, 0};
        int[] dstShapes = new int[]{7, 4};

        CacheData2D.copyDataBuffer(srcOffsets, cacheWidth, cacheData, dstOffsets, dstShapes, targetWidth, targetBuffer);
        assertEquals(0, targetBuffer.getElemIntAt(0));
        assertEquals(11, targetBuffer.getElemIntAt(30));
        assertEquals(12, targetBuffer.getElemIntAt(31));
        assertEquals(101, targetBuffer.getElemIntAt(90));
        assertEquals(104, targetBuffer.getElemIntAt(93));
        assertEquals(0, targetBuffer.getElemIntAt(95));
    }

    @Test
    @STTM("SNAP-4107")
    public void testCopyDataBuffer_outRight() {
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_FLOAT64, 300);
        final int cacheWidth = 15;
        int[] srcOffsets = new int[]{6, 9};

        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_FLOAT64, 100);
        final int targetWidth = 10;
        int[] dstOffsets = new int[]{0, 0};
        int[] dstShapes = new int[]{10, 6};

        CacheData2D.copyDataBuffer(srcOffsets, cacheWidth, cacheData, dstOffsets, dstShapes, targetWidth, targetBuffer);
        assertEquals(99, targetBuffer.getElemIntAt(0));
        assertEquals(100, targetBuffer.getElemIntAt(1));
        assertEquals(104, targetBuffer.getElemIntAt(5));
        assertEquals(0, targetBuffer.getElemIntAt(6));
        assertEquals(234, targetBuffer.getElemIntAt(90));
        assertEquals(239, targetBuffer.getElemIntAt(95));
        assertEquals(0, targetBuffer.getElemIntAt(99));
    }

    @Test
    @STTM("SNAP-4107")
    public void testCopyDataBuffer_lowerRightCorner() {
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_UINT16, 300);
        final int cacheWidth = 15;
        int[] srcOffsets = new int[]{14, 10};

        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_UINT16, 100);
        final int targetWidth = 10;
        int[] dstOffsets = new int[]{0, 0};
        int[] dstShapes = new int[]{5, 5};

        CacheData2D.copyDataBuffer(srcOffsets, cacheWidth, cacheData, dstOffsets, dstShapes, targetWidth, targetBuffer);
        assertEquals(220, targetBuffer.getElemIntAt(0));
        assertEquals(224, targetBuffer.getElemIntAt(4));
        assertEquals(0, targetBuffer.getElemIntAt(5));
        assertEquals(280, targetBuffer.getElemIntAt(40));
        assertEquals(284, targetBuffer.getElemIntAt(44));
        assertEquals(0, targetBuffer.getElemIntAt(45));
    }

    @Test
    @STTM("SNAP-4107")
    public void testCopyDataBuffer_outBottom() {
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_UINT32, 300);
        final int cacheWidth = 15;
        int[] srcOffsets = new int[]{16, 3};

        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_UINT32, 100);
        final int targetWidth = 10;
        int[] dstOffsets = new int[]{0, 0};
        int[] dstShapes = new int[]{4, 10};

        CacheData2D.copyDataBuffer(srcOffsets, cacheWidth, cacheData, dstOffsets, dstShapes, targetWidth, targetBuffer);
        assertEquals(243, targetBuffer.getElemIntAt(0));
        assertEquals(244, targetBuffer.getElemIntAt(1));
        assertEquals(252, targetBuffer.getElemIntAt(9));
        assertEquals(288, targetBuffer.getElemIntAt(30));
        assertEquals(0, targetBuffer.getElemIntAt(40));
        assertEquals(0, targetBuffer.getElemIntAt(45));
    }

    @Test
    @STTM("SNAP-4107")
    public void testCopyDataBuffer_lowerLeftCorner() {
        ProductData cacheData = createPreparedBuffer(ProductData.TYPE_UINT16, 300);
        final int cacheWidth = 15;
        int[] srcOffsets = new int[]{19, 0};

        ProductData targetBuffer = ProductData.createInstance(ProductData.TYPE_UINT16, 100);
        final int targetWidth = 10;
        int[] dstOffsets = new int[]{0, 9};
        int[] dstShapes = new int[]{1, 1};

        CacheData2D.copyDataBuffer(srcOffsets, cacheWidth, cacheData, dstOffsets, dstShapes, targetWidth, targetBuffer);
        assertEquals(0, targetBuffer.getElemIntAt(0));
        assertEquals(285, targetBuffer.getElemIntAt(9));
        assertEquals(0, targetBuffer.getElemIntAt(10));
        assertEquals(0, targetBuffer.getElemIntAt(19));
    }

    @Test
    @STTM("SNAP-4107")
    public void testEnsureData() throws IOException {
        final CacheDataProvider cacheDataProvider = new MockProvider();
        final int[] offsets = new int[]{350, 200};
        final int[] shapes = new int[]{10, 10};
        final CacheData2D cacheData2D = new CacheData2D(offsets, shapes);

        final CacheContext cacheContext = new CacheContext(new VariableDescriptor(), cacheDataProvider);
        cacheData2D.setCacheContext(cacheContext);
        assertNull(cacheData2D.getData());

        cacheData2D.copyData(new int[]{0, 0}, new int[]{5, 5}, new int[]{5, 5}, 10, ProductData.createInstance(ProductData.TYPE_UINT16, 100));
        assertNotNull(cacheData2D.getData());
    }

    @Test
    @STTM("SNAP-4107")
    public void testGetSizeInBytes() throws IOException {
        final int[] offsets = new int[]{350, 200};
        final int[] shapes = new int[]{10, 10};
        final CacheData2D cacheData2D = new CacheData2D(offsets, shapes);

        // size without having a buffer allocated
        assertEquals(192, cacheData2D.getSizeInBytes());

        // trigger reading the buffer
        final CacheDataProvider cacheDataProvider = new MockProvider();
        final CacheContext cacheContext = new CacheContext(new VariableDescriptor(), cacheDataProvider);
        cacheData2D.setCacheContext(cacheContext);
        cacheData2D.copyData(new int[]{0, 0}, new int[]{5, 5}, new int[]{5, 5}, 10, ProductData.createInstance(ProductData.TYPE_UINT16, 100));

        // now with a data buffer - 100 times size of short added
        assertEquals(392, cacheData2D.getSizeInBytes());
    }

    private static class MockProvider implements CacheDataProvider {
        @Override
        public VariableDescriptor getVariableDescriptor(String variableName) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public ProductData readCacheBlock(String variableName, int[] offsets, int[] shapes, ProductData targetData) throws IOException {
            return ProductData.createInstance(ProductData.TYPE_UINT16, 100);
        }
    }
}