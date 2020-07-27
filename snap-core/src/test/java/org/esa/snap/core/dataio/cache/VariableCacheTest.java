package org.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.Dimension;

import static org.esa.snap.core.dataio.cache.VariableCache.getStreamOutputPos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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

    @SuppressWarnings("PointlessArithmeticExpression")
    @Test
    public void testUpdate_multipleTiles_notMatchingHeight() {
        final Band band = mock(Band.class);
        when(band.getRasterSize()).thenReturn(new Dimension(200, 260));
        when(band.getDataType()).thenReturn(ProductData.TYPE_INT32);

        final VariableCache variableCache = new VariableCache(band);
        final ProductData int32Data = createInt32Data(100 * 100);

        // ---------------------------------------------------------------
        boolean canWrite = variableCache.update(0, 0, 100, 100, int32Data);
        // ---------------------------------------------------------------
        assertFalse(canWrite);
        CacheBlock cacheBlock = variableCache.cacheBlocks[0];
        int[] bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(0, bufferElems[0 * 200 + 0]);
        assertEquals(1, bufferElems[0 * 200 + 1]);
        assertEquals(99, bufferElems[0 * 200 + 99]);
        assertEquals(0, bufferElems[0 * 200 + 100]);
        // last row written
        assertEquals(9900, bufferElems[99 * 200 + 0]);
        assertEquals(9901, bufferElems[99 * 200 + 1]);
        assertEquals(9999, bufferElems[99 * 200 + 99]);
        assertEquals(0, bufferElems[99 * 200 + 100]);
        // first row not written
        assertEquals(0, bufferElems[100 * 200 + 0]);

        // ---------------------------------------------------------------
        canWrite = variableCache.update(100, 0, 100, 100, int32Data);
        // ---------------------------------------------------------------
        assertFalse(canWrite);
        cacheBlock = variableCache.cacheBlocks[0];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(99, bufferElems[0 * 200 + 99]);
        assertEquals(0, bufferElems[0 * 200 + 100]);
        assertEquals(1, bufferElems[0 * 200 + 101]);
        assertEquals(99, bufferElems[0 * 200 + 199]);
        // last row written
        assertEquals(9999, bufferElems[99 * 200 + 99]);
        assertEquals(9900, bufferElems[99 * 200 + 100]);
        assertEquals(9901, bufferElems[99 * 200 + 101]);
        assertEquals(9999, bufferElems[99 * 200 + 199]);
        // first row not written
        assertEquals(0, bufferElems[100 * 200 + 0]);

        // ---------------------------------------------------------------
        canWrite = variableCache.update(0, 100, 100, 100, int32Data);
        // ---------------------------------------------------------------
        assertFalse(canWrite);
        cacheBlock = variableCache.cacheBlocks[0];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(0, bufferElems[100 * 200 + 0]);
        assertEquals(1, bufferElems[100 * 200 + 1]);
        assertEquals(99, bufferElems[100 * 200 + 99]);
        assertEquals(0, bufferElems[100 * 200 + 100]);
        // last row written
        assertEquals(2700, bufferElems[127 * 200]);
        assertEquals(2701, bufferElems[127 * 200 + 1]);
        assertEquals(2799, bufferElems[127 * 200 + 99]);

        cacheBlock = variableCache.cacheBlocks[1];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(2800, bufferElems[0 * 200 + 0]);
        assertEquals(2801, bufferElems[0 * 200 + 1]);
        assertEquals(2899, bufferElems[0 * 200 + 99]);
        assertEquals(0, bufferElems[0 * 200 + 100]);
        // last row written
        assertEquals(9900, bufferElems[71 * 200]);
        assertEquals(9901, bufferElems[71 * 200 + 1]);
        assertEquals(9999, bufferElems[71 * 200 + 99]);
        // first row not written
        assertEquals(0, bufferElems[72 * 200]);

        // ---------------------------------------------------------------
        canWrite = variableCache.update(100, 100, 100, 100, int32Data);
        // ---------------------------------------------------------------
        assertTrue(canWrite);
        cacheBlock = variableCache.cacheBlocks[0];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(100, bufferElems[101 * 200 + 0]);
        assertEquals(101, bufferElems[101 * 200 + 1]);
        assertEquals(199, bufferElems[101 * 200 + 99]);
        // last row written
        assertEquals(2700, bufferElems[127 * 200 + 100]);
        assertEquals(2701, bufferElems[127 * 200 + 101]);
        assertEquals(2799, bufferElems[127 * 200 + 199]);

        cacheBlock = variableCache.cacheBlocks[1];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(2800, bufferElems[0 * 200 + 100]);
        assertEquals(2801, bufferElems[0 * 200 + 101]);
        assertEquals(2899, bufferElems[0 * 200 + 199]);
        // last row written
        assertEquals(9900, bufferElems[71 * 200 + 100]);
        assertEquals(9901, bufferElems[71 * 200 + 101]);
        assertEquals(9999, bufferElems[71 * 200 + 199]);
        // first row not written
        assertEquals(0, bufferElems[72 * 200 + 100]);

        // ---------------------------------------------------------------
        canWrite = variableCache.update(0, 200, 100, 60, int32Data);
        // ---------------------------------------------------------------
        assertFalse(canWrite);
        cacheBlock = variableCache.cacheBlocks[1];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(0, bufferElems[72 * 200 + 0]);
        assertEquals(1, bufferElems[72 * 200 + 1]);
        assertEquals(99, bufferElems[72 * 200 + 99]);
        // last row written
        assertEquals(5500, bufferElems[127 * 200 + 0]);
        assertEquals(5501, bufferElems[127 * 200 + 1]);
        assertEquals(5599, bufferElems[127 * 200 + 99]);

        cacheBlock = variableCache.cacheBlocks[2];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(5600, bufferElems[0 * 200 + 0]);
        assertEquals(5601, bufferElems[0 * 200 + 1]);
        assertEquals(5699, bufferElems[0 * 200 + 99]);
        // last row written
        assertEquals(5900, bufferElems[3 * 200 + 0]);
        assertEquals(5901, bufferElems[3 * 200 + 1]);
        assertEquals(5999, bufferElems[3 * 200 + 99]);

        // ---------------------------------------------------------------
        canWrite = variableCache.update(100, 200, 100, 60, int32Data);
        // ---------------------------------------------------------------
        assertTrue(canWrite);
        cacheBlock = variableCache.cacheBlocks[1];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(0, bufferElems[72 * 200 + 100]);
        assertEquals(1, bufferElems[72 * 200 + 101]);
        assertEquals(99, bufferElems[72 * 200 + 199]);
        // last row written
        assertEquals(5500, bufferElems[127 * 200 + 100]);
        assertEquals(5501, bufferElems[127 * 200 + 101]);
        assertEquals(5599, bufferElems[127 * 200 + 199]);

        cacheBlock = variableCache.cacheBlocks[2];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(5600, bufferElems[0 * 200 + 100]);
        assertEquals(5601, bufferElems[0 * 200 + 101]);
        assertEquals(5699, bufferElems[0 * 200 + 199]);
        // last row written
        assertEquals(5900, bufferElems[3 * 200 + 100]);
        assertEquals(5901, bufferElems[3 * 200 + 101]);
        assertEquals(5999, bufferElems[3 * 200 + 199]);
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    @Test
    public void testUpdate_multipleTiles_startNotAtFirstLine() {
        final Band band = mock(Band.class);
        when(band.getRasterSize()).thenReturn(new Dimension(200, 1000));
        when(band.getDataType()).thenReturn(ProductData.TYPE_INT32);

        final VariableCache variableCache = new VariableCache(band);
        final ProductData int32Data = createInt32Data(100 * 100);

        boolean canWrite = variableCache.update(0, 100, 100, 100, int32Data);
        assertFalse(canWrite);
        CacheBlock cacheBlock = variableCache.cacheBlocks[0];
        int[] bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // unwritten space
        assertEquals(0, bufferElems[99 * 200 + 12]);
        // first row written
        assertEquals(0, bufferElems[100 * 200 + 0]);
        assertEquals(1, bufferElems[100 * 200 + 1]);
        assertEquals(99, bufferElems[100 * 200 + 99]);
        // last row written
        assertEquals(2700, bufferElems[127 * 200]);
        assertEquals(2701, bufferElems[127 * 200 + 1]);
        assertEquals(2799, bufferElems[127 * 200 + 99]);

        cacheBlock = variableCache.cacheBlocks[1];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(2800, bufferElems[0 * 200 + 0]);
        assertEquals(2801, bufferElems[0 * 200 + 1]);
        assertEquals(2899, bufferElems[0 * 200 + 99]);
        assertEquals(0, bufferElems[0 * 200 + 100]);
        // last row written
        assertEquals(9900, bufferElems[71 * 200]);
        assertEquals(9901, bufferElems[71 * 200 + 1]);
        assertEquals(9999, bufferElems[71 * 200 + 99]);
        // first row not written
        assertEquals(0, bufferElems[72 * 200]);

        canWrite = variableCache.update(100, 100, 100, 100, int32Data);
        assertFalse(canWrite);
        cacheBlock = variableCache.cacheBlocks[0];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(100, bufferElems[101 * 200 + 0]);
        assertEquals(101, bufferElems[101 * 200 + 1]);
        assertEquals(199, bufferElems[101 * 200 + 99]);
        // last row written
        assertEquals(2700, bufferElems[127 * 200 + 100]);
        assertEquals(2701, bufferElems[127 * 200 + 101]);
        assertEquals(2799, bufferElems[127 * 200 + 199]);

        cacheBlock = variableCache.cacheBlocks[1];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(2800, bufferElems[0 * 200 + 100]);
        assertEquals(2801, bufferElems[0 * 200 + 101]);
        assertEquals(2899, bufferElems[0 * 200 + 199]);
        // last row written
        assertEquals(9900, bufferElems[71 * 200 + 100]);
        assertEquals(9901, bufferElems[71 * 200 + 101]);
        assertEquals(9999, bufferElems[71 * 200 + 199]);
        // first row not written
        assertEquals(0, bufferElems[72 * 200 + 100]);

        canWrite = variableCache.update(0, 0, 100, 100, int32Data);
        assertFalse(canWrite);
        cacheBlock = variableCache.cacheBlocks[0];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(0, bufferElems[0 * 200 + 0]);
        assertEquals(1, bufferElems[0 * 200 + 1]);
        assertEquals(99, bufferElems[0 * 200 + 99]);
        assertEquals(0, bufferElems[0 * 200 + 100]);
        // last row written
        assertEquals(9900, bufferElems[99 * 200 + 0]);
        assertEquals(9901, bufferElems[99 * 200 + 1]);
        assertEquals(9999, bufferElems[99 * 200 + 99]);
        assertEquals(0, bufferElems[99 * 200 + 100]);
        // data already written
        assertEquals(1, bufferElems[100 * 200 + 1]);
        assertEquals(2, bufferElems[100 * 200 + 2]);

        canWrite = variableCache.update(100, 0, 100, 100, int32Data);
        assertTrue(canWrite);
        cacheBlock = variableCache.cacheBlocks[0];
        bufferElems = (int[]) cacheBlock.getBufferData().getElems();
        // first row written
        assertEquals(99, bufferElems[0 * 200 + 99]);
        assertEquals(0, bufferElems[0 * 200 + 100]);
        assertEquals(1, bufferElems[0 * 200 + 101]);
        assertEquals(99, bufferElems[0 * 200 + 199]);
        // last row written
        assertEquals(9999, bufferElems[99 * 200 + 99]);
        assertEquals(9900, bufferElems[99 * 200 + 100]);
        assertEquals(9901, bufferElems[99 * 200 + 101]);
        assertEquals(9999, bufferElems[99 * 200 + 199]);
        // data already written
        assertEquals(1, bufferElems[100 * 200 + 1]);
        assertEquals(2, bufferElems[100 * 200 + 2]);
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


    @Test
    public void testGetStreamOutputPos() {
        assertEquals(0, getStreamOutputPos(new CacheBlock(0, 200, 10, ProductData.TYPE_UINT32, Double.NaN)));
        assertEquals(200, getStreamOutputPos(new CacheBlock(1, 200, 10, ProductData.TYPE_UINT32, Double.NaN)));
        assertEquals(1000, getStreamOutputPos(new CacheBlock(5, 200, 10, ProductData.TYPE_UINT32, Double.NaN)));

        assertEquals(36000000, getStreamOutputPos(new CacheBlock(12000, 3000, 10, ProductData.TYPE_UINT32, Double.NaN)));
        assertEquals(5688609412L, getStreamOutputPos(new CacheBlock(53558, 106214, 10, ProductData.TYPE_UINT32, Double.NaN)));


    }
}
