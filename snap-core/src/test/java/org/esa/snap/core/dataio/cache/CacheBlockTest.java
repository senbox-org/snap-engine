package org.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.Rectangle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CacheBlockTest {

    @Test
    public void testCreate_and_getter() {
        final int width = 2051;
        final int height = 64;
        final CacheBlock cacheBlock = new CacheBlock(128, width, height, ProductData.TYPE_INT16, -1);

        assertEquals(128, cacheBlock.getYOffset());

        final Rectangle rect = cacheBlock.getRegion();
        assertEquals(0, rect.getX(), 1e-8);
        assertEquals(128, rect.getY(), 1e-8);
        assertEquals(width, rect.getWidth(), 1e-8);
        assertEquals(height, rect.getHeight(), 1e-8);

        final ProductData data = cacheBlock.getBufferData();
        assertNotNull(data);
        assertEquals(width * height, data.getNumElems());
        assertEquals(ProductData.TYPE_INT16, data.getType());
    }

    @Test
    public void testCreate_and_dispose() {
        final CacheBlock cacheBlock = new CacheBlock(411, 109, 14, ProductData.TYPE_INT32, -2);

        assertNotNull(cacheBlock.getBufferData());

        cacheBlock.dispose();

        assertNull(cacheBlock.getBufferData());
    }

    @Test
    public void testCreate_and_isComplete() {
        final CacheBlock cacheBlock = new CacheBlock(412, 110, 15, ProductData.TYPE_FLOAT32, Float.NaN);

        assertFalse(cacheBlock.isComplete());
    }

    @Test
    public void testUpdate_partially() {
        final CacheBlock cacheBlock = new CacheBlock(413, 20, 10, ProductData.TYPE_FLOAT32, Float.NaN);

        final float[] data = createFloatBuffer();

        final ProductData productData = ProductData.createInstance(data);
        cacheBlock.update(0, 0, 413, 10, 10, productData);

        final ProductData bufferData = cacheBlock.getBufferData();
        assertEquals(0.0, bufferData.getElemFloatAt(0), 1e-8);
        assertEquals(1.0, bufferData.getElemFloatAt(1), 1e-8);
        assertEquals(10.0, bufferData.getElemFloatAt(20), 1e-8);
        assertEquals(21.0, bufferData.getElemFloatAt(41), 1e-8);

        assertFalse(cacheBlock.isComplete());
    }

    @Test
    public void testUpdate_completely_two_regions() {
        final CacheBlock cacheBlock = new CacheBlock(414, 20, 10, ProductData.TYPE_FLOAT32, Float.NaN);

        final float[] data = createFloatBuffer();

        final ProductData productData = ProductData.createInstance(data);
        cacheBlock.update(0, 0, 414, 10, 10, productData);
        cacheBlock.update(10, 0, 414, 10, 10, productData);

        final ProductData bufferData = cacheBlock.getBufferData();
        assertEquals(0.0, bufferData.getElemFloatAt(0), 1e-8);
        assertEquals(1.0, bufferData.getElemFloatAt(1), 1e-8);
        assertEquals(0.0, bufferData.getElemFloatAt(10), 1e-8);
        assertEquals(1.0, bufferData.getElemFloatAt(11), 1e-8);
        assertEquals(10.0, bufferData.getElemFloatAt(20), 1e-8);
        assertEquals(10.0, bufferData.getElemFloatAt(30), 1e-8);
        assertEquals(21.0, bufferData.getElemFloatAt(41), 1e-8);

        assertTrue(cacheBlock.isComplete());
    }

    private float[] createFloatBuffer() {
        final float[] data = new float[10 * 10];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
        return data;
    }
}
