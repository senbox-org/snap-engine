package org.esa.snap.core.dataio.dimap;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.Dimension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VariableCacheTest {

    @Test
    public void testConstruct_correctBufferAllocation() {
        final Band band = mock(Band.class);
        when(band.getRasterSize()).thenReturn(new Dimension(156, 2578));

        VariableCache variableCache = new VariableCache(band);
        assertEquals(21, variableCache.cacheBlocks.length);

        when(band.getRasterSize()).thenReturn(new Dimension(156, 256));

        variableCache = new VariableCache(band);
        assertEquals(2, variableCache.cacheBlocks.length);

        when(band.getRasterSize()).thenReturn(new Dimension(156, 257));

        variableCache = new VariableCache(band);
        assertEquals(3, variableCache.cacheBlocks.length);
    }

    @Test
    public void testUpdate_firstBlock_updateSmallerThanCacheHeight() {
        final Band band = mock(Band.class);
        when(band.getRasterSize()).thenReturn(new Dimension(157, 2579));
        when(band.getDataType()).thenReturn(ProductData.TYPE_INT16);

        final VariableCache variableCache = new VariableCache(band);

        final boolean canWrite = variableCache.update(0, 0, 100, 100, createInt16Data(100 * 100));
        assertFalse(canWrite);
        final CacheBlock cacheBlock = variableCache.cacheBlocks[0];
        assertNotNull(cacheBlock);

        final short[] bufferElems = (short[]) cacheBlock.getBufferData().getElems();
        assertEquals(1, bufferElems[1]);
        assertEquals(99, bufferElems[99]);
        // next row
        assertEquals(100, bufferElems[157]);
        assertEquals(101, bufferElems[158]);

        // 11th row
        assertEquals(1000, bufferElems[1570]);
        assertEquals(1001, bufferElems[1571]);
    }

    @Test
    public void testUpdate_secondBlock_matchCacheHeight() {
        final Band band = mock(Band.class);
        when(band.getRasterSize()).thenReturn(new Dimension(158, 2580));
        when(band.getDataType()).thenReturn(ProductData.TYPE_INT16);

        final VariableCache variableCache = new VariableCache(band);

        final boolean canWrite = variableCache.update(0, 128, 100, 128, createInt16Data((128 + 100) * 128));
        assertFalse(canWrite);
        final CacheBlock cacheBlock = variableCache.cacheBlocks[1];
        assertNotNull(cacheBlock);

        final short[] bufferElems = (short[]) cacheBlock.getBufferData().getElems();
        assertEquals(0, bufferElems[0]);
        assertEquals(1, bufferElems[1]);
        assertEquals(99, bufferElems[99]);
        // next row
        assertEquals(100, bufferElems[158]);
        assertEquals(101, bufferElems[159]);

        // 12th row
        assertEquals(1200, bufferElems[1896]);
        assertEquals(1201, bufferElems[1897]);
    }

    @Test
    public void testUpdate_secondBlock_notMatchingHeight() {
        final Band band = mock(Band.class);
        when(band.getRasterSize()).thenReturn(new Dimension(600, 1000));
        when(band.getDataType()).thenReturn(ProductData.TYPE_INT32);

        final VariableCache variableCache = new VariableCache(band);

        boolean canWrite = variableCache.update(0, 0, 200, 200, createInt32Data(200 * 200));
        assertFalse(canWrite);
        assertNotNull(variableCache.cacheBlocks[0]);
        assertNotNull(variableCache.cacheBlocks[1]);

        CacheBlock cacheBlock = variableCache.cacheBlocks[0];
        int[] elems = (int[]) cacheBlock.getBufferData().getElems();
        assertEquals(0, elems[0]);
        assertEquals(1, elems[1]);
        assertEquals(25400, elems[76200]);

        cacheBlock = variableCache.cacheBlocks[1];
        elems = (int[]) cacheBlock.getBufferData().getElems();
        assertEquals(25600, elems[0]);
        assertEquals(25601, elems[1]);
        assertEquals(39800, elems[71 * 600]);

        canWrite = variableCache.update(0, 200, 200, 200, createInt32Data(200 * 200));
        assertFalse(canWrite);
        assertNotNull(variableCache.cacheBlocks[0]);
        assertNotNull(variableCache.cacheBlocks[1]);
        assertNotNull(variableCache.cacheBlocks[2]);
        assertNotNull(variableCache.cacheBlocks[3]);
    }

    private ProductData createInt16Data(int size) {
        final short[] data = new short[size];
        for (short i = 0; i < data.length; i++) {
            data[i] = i;
        }
        return ProductData.createInstance(data);
    }

    private ProductData createInt32Data(int size) {
        final int[] data = new int[size];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
        return ProductData.createInstance(data);
    }
}
