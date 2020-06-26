package org.esa.snap.core.dataio.dimap;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.Rectangle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CacheBlockTest {

    @Test
    public void testCreate_and_getter() {
        final int width = 2051;
        final int height = 64;
        final CacheBlock cacheBlock = new CacheBlock(128, width, height, ProductData.TYPE_INT16);

        assertEquals(128, cacheBlock.getYOffset());

        final Rectangle rect = cacheBlock.getRegion();
        assertEquals(0, rect.getX(), 1e-8);
        assertEquals(128, rect.getY(), 1e-8);
        assertEquals(width, rect.getWidth(), 1e-8);
        assertEquals(height, rect.getHeight(), 1e-8);

        final ProductData data = cacheBlock.getData();
        assertNotNull(data);
        assertEquals(width * height, data.getNumElems());
        assertEquals(ProductData.TYPE_INT16, data.getType());
    }

    @Test
    public void testCreate_and_dispose(){
        final CacheBlock cacheBlock = new CacheBlock(411, 109, 14, ProductData.TYPE_INT32);

        assertNotNull(cacheBlock.getData());

        cacheBlock.dispose();

        assertNull(cacheBlock.getData());
    }

    @Test
    public void testCreate_and_isComplete(){
        final CacheBlock cacheBlock = new CacheBlock(412, 110, 15, ProductData.TYPE_FLOAT32);

        assertFalse(cacheBlock.isComplete());
    }
}
