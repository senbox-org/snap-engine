package org.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SlabCacheTest {

    private DataStorage storage;

    @Before
    public void setUp() {
        storage = mock(DataStorage.class);
    }

    @Test
    public void testGet_oneSlab_created() {
        final SlabCache slabCache = new SlabCache(100, 200, 10, 10, storage);

        final Slab[] slabs = slabCache.get(2, 3, 3, 3);
        assertEquals(1, slabs.length);

        final Rectangle region = slabs[0].getRegion();
        assertEquals(0, region.x);
        assertEquals(0, region.y);
        assertEquals(10, region.width);
        assertEquals(10, region.height);

        assertTrue(slabs[0].getLastAccess() > -1);

        verify(storage, times(1)).readRasterData(eq(0), eq(0), eq(10), eq(10), anyObject());
        verify(storage, times(1)).createBuffer(eq(100));
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void testGet_oneSlab_fromCached() {
        final SlabCache slabCache = new SlabCache(100, 200, 10, 10, storage);

        final Slab[] slabs = slabCache.get(2, 3, 3, 3);
        assertEquals(1, slabs.length);
        final long lastAccess = slabs[0].getLastAccess();
        assertTrue(lastAccess > -1);
        final Rectangle region = slabs[0].getRegion();
        assertEquals(0, region.x);
        assertEquals(0, region.y);
        assertEquals(10, region.width);
        assertEquals(10, region.height);

        final Slab[] slabsSecond = slabCache.get(2, 3, 3, 3);
        assertEquals(1, slabsSecond.length);
        assertSame(slabs[0], slabsSecond[0]);
        assertTrue(slabsSecond[0].getLastAccess() > -1);
        assertTrue(slabsSecond[0].getLastAccess() >= lastAccess);

        verify(storage, times(1)).readRasterData(eq(0), eq(0), eq(10), eq(10), anyObject());
        verify(storage, times(1)).createBuffer(eq(100));
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void testGet_twoSlabs_horizontal() {
        final SlabCache slabCache = new SlabCache(90, 190, 10, 9, storage);

        final Slab[] slabs = slabCache.get(8, 3, 4, 3);
        assertEquals(2, slabs.length);

        Rectangle region = slabs[0].getRegion();
        assertEquals(0, region.x);
        assertEquals(0, region.y);
        assertEquals(10, region.width);
        assertEquals(9, region.height);
        assertTrue(slabs[0].getLastAccess() > -1);

        region = slabs[1].getRegion();
        assertEquals(10, region.x);
        assertEquals(0, region.y);
        assertEquals(10, region.width);
        assertEquals(9, region.height);
        assertTrue(slabs[1].getLastAccess() > -1);

        verify(storage, times(1)).readRasterData(eq(0), eq(0), eq(10), eq(9), anyObject());
        verify(storage, times(1)).readRasterData(eq(10), eq(0), eq(10), eq(9), anyObject());
        verify(storage, times(2)).createBuffer(eq(90));
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void testGet_twoSlabs_vertical() {
        final SlabCache slabCache = new SlabCache(80, 180, 9, 8, storage);

        final Slab[] slabs = slabCache.get(2, 6, 4, 4);
        assertEquals(2, slabs.length);

        Rectangle region = slabs[0].getRegion();
        assertEquals(0, region.x);
        assertEquals(0, region.y);
        assertEquals(9, region.width);
        assertEquals(8, region.height);
        assertTrue(slabs[0].getLastAccess() > -1);

        region = slabs[1].getRegion();
        assertEquals(0, region.x);
        assertEquals(8, region.y);
        assertEquals(9, region.width);
        assertEquals(8, region.height);
        assertTrue(slabs[1].getLastAccess() > -1);

        verify(storage, times(1)).readRasterData(eq(0), eq(0), eq(9), eq(8), anyObject());
        verify(storage, times(1)).readRasterData(eq(0), eq(8), eq(9), eq(8), anyObject());
        verify(storage, times(2)).createBuffer(eq(72));
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void testGet_fourSlabs_intersectingCenterPoint() {
        final SlabCache slabCache = new SlabCache(50, 140, 10, 20, storage);

        final Slab[] slabs = slabCache.get(28, 38, 10, 10);
        assertEquals(4, slabs.length);

        Rectangle region = slabs[0].getRegion();
        assertEquals(20, region.x);
        assertEquals(20, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);
        assertTrue(slabs[0].getLastAccess() > -1);

        region = slabs[1].getRegion();
        assertEquals(20, region.x);
        assertEquals(40, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);
        assertTrue(slabs[1].getLastAccess() > -1);

        region = slabs[2].getRegion();
        assertEquals(30, region.x);
        assertEquals(20, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);
        assertTrue(slabs[2].getLastAccess() > -1);

        region = slabs[3].getRegion();
        assertEquals(30, region.x);
        assertEquals(40, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);
        assertTrue(slabs[3].getLastAccess() > -1);

        verify(storage, times(1)).readRasterData(eq(20), eq(20), eq(10), eq(20), anyObject());
        verify(storage, times(1)).readRasterData(eq(20), eq(40), eq(10), eq(20), anyObject());
        verify(storage, times(1)).readRasterData(eq(30), eq(20), eq(10), eq(20), anyObject());
        verify(storage, times(1)).readRasterData(eq(30), eq(40), eq(10), eq(20), anyObject());
        verify(storage, times(4)).createBuffer(eq(200));
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void testGet_fourSlabs_intersectingCenterPoint_twofromCache() {
        final SlabCache slabCache = new SlabCache(50, 140, 10, 20, storage);

        // fetch upper left of intersection - which is in cache afterwards
        Slab[] slabs = slabCache.get(22, 31, 5, 5);
        assertEquals(1, slabs.length);

        Rectangle region = slabs[0].getRegion();
        assertEquals(20, region.x);
        assertEquals(20, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);

        // fetch lower right of intersection - which is in cache afterwards
        slabs = slabCache.get(33, 42, 5, 5);
        assertEquals(1, slabs.length);

        region = slabs[0].getRegion();
        assertEquals(30, region.x);
        assertEquals(40, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);

        // fetch full intersection - with two slabs from cache and two read from data source
        slabs = slabCache.get(28, 38, 10, 10);
        assertEquals(4, slabs.length);

        region = slabs[0].getRegion();
        assertEquals(20, region.x);
        assertEquals(20, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);

        region = slabs[1].getRegion();
        assertEquals(30, region.x);
        assertEquals(40, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);

        region = slabs[2].getRegion();
        assertEquals(20, region.x);
        assertEquals(40, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);

        region = slabs[3].getRegion();
        assertEquals(30, region.x);
        assertEquals(20, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);

        verify(storage, times(1)).readRasterData(eq(20), eq(20), eq(10), eq(20), anyObject());
        verify(storage, times(1)).readRasterData(eq(20), eq(40), eq(10), eq(20), anyObject());
        verify(storage, times(1)).readRasterData(eq(30), eq(20), eq(10), eq(20), anyObject());
        verify(storage, times(1)).readRasterData(eq(30), eq(40), eq(10), eq(20), anyObject());
        verify(storage, times(4)).createBuffer(eq(200));
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void testMODIS_like_tiles_twoScans_reading() {
        final SlabCache slabCache = new SlabCache(400, 1400, 400, 20, storage);

        final Slab[] slabs = slabCache.get(200, 55, 10, 10);
        assertEquals(2, slabs.length);

        Rectangle region = slabs[0].getRegion();
        assertEquals(0, region.x);
        assertEquals(40, region.y);
        assertEquals(400, region.width);
        assertEquals(20, region.height);

        region = slabs[1].getRegion();
        assertEquals(0, region.x);
        assertEquals(60, region.y);
        assertEquals(400, region.width);
        assertEquals(20, region.height);

        verify(storage, times(1)).readRasterData(eq(0), eq(40), eq(400), eq(20), anyObject());
        verify(storage, times(1)).readRasterData(eq(0), eq(60), eq(400), eq(20), anyObject());
        verify(storage, times(2)).createBuffer(eq(8000));
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void testMODIS_like_tiles_threeScans_oneFromCache() {
        final SlabCache slabCache = new SlabCache(512, 1840, 512, 40, storage);

        // request the center scan - which is in cache afterwards
        Slab[] slabs = slabCache.get(400, 1200, 10, 10);
        assertEquals(1, slabs.length);

        Rectangle region = slabs[0].getRegion();
        assertEquals(0, region.x);
        assertEquals(1200, region.y);
        assertEquals(512, region.width);
        assertEquals(40, region.height);

        // now the full request over three scans
        slabs = slabCache.get(400, 1190, 20, 70);
        assertEquals(3, slabs.length);

        // center one is first in result-set - fetched from cache
        region = slabs[0].getRegion();
        assertEquals(0, region.x);
        assertEquals(1200, region.y);
        assertEquals(512, region.width);
        assertEquals(40, region.height);

        region = slabs[1].getRegion();
        assertEquals(0, region.x);
        assertEquals(1160, region.y);
        assertEquals(512, region.width);
        assertEquals(40, region.height);

        region = slabs[2].getRegion();
        assertEquals(0, region.x);
        assertEquals(1240, region.y);
        assertEquals(512, region.width);
        assertEquals(40, region.height);

        verify(storage, times(1)).readRasterData(eq(0), eq(1160), eq(512), eq(40), anyObject());
        verify(storage, times(1)).readRasterData(eq(0), eq(1200), eq(512), eq(40), anyObject());
        verify(storage, times(1)).readRasterData(eq(0), eq(1240), eq(512), eq(40), anyObject());
        verify(storage, times(3)).createBuffer(eq(20480));
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void testCopyData_one_slab_region_inside_float() {
        final Slab slab = new Slab(new Rectangle(0, 0, 10, 10));
        slab.setData(create(100, 1.0f));
        final Slab[] slabs = {slab};

        final Rectangle destRect = new Rectangle(2, 3, 4, 3);
        final ProductData destBuffer = create(12, 2.f);

        SlabCache.copyData(destBuffer, destRect, slabs);

        // just check corners
        assertEquals(1.f, destBuffer.getElemFloatAt(0), 1e-8);
        assertEquals(1.f, destBuffer.getElemFloatAt(3), 1e-8);
        assertEquals(1.f, destBuffer.getElemFloatAt(8), 1e-8);
        assertEquals(1.f, destBuffer.getElemFloatAt(11), 1e-8);
    }

    @Test
    public void testCopyData_two_slabs_region_in_one_inside_float() {
        // actually this should never happen, this test just verifies that there is no crash and data is copied as expected tb 2020-03-13

        final Slab slab_0 = new Slab(new Rectangle(0, 0, 10, 10));
        slab_0.setData(create(100, 2.0f));

        final Slab slab_1 = new Slab(new Rectangle(10, 0, 10, 10));
        slab_1.setData(create(100, 3.0f));

        final Slab[] slabs = {slab_0, slab_1};

        // is contained in second slab
        final Rectangle destRect = new Rectangle(12, 3, 4, 3);
        final ProductData destBuffer = create(12, 4.f);

        SlabCache.copyData(destBuffer, destRect, slabs);

        // just check corners
        assertEquals(3.f, destBuffer.getElemFloatAt(0), 1e-8);
        assertEquals(3.f, destBuffer.getElemFloatAt(3), 1e-8);
        assertEquals(3.f, destBuffer.getElemFloatAt(8), 1e-8);
        assertEquals(3.f, destBuffer.getElemFloatAt(11), 1e-8);
    }

    @Test
    public void testCopyData_two_slabs_region_intersects_horizontally_byte() {
        final Slab slab_0 = new Slab(new Rectangle(0, 10, 10, 10));
        slab_0.setData(create(100, (byte)3));

        final Slab slab_1 = new Slab(new Rectangle(0, 20, 10, 10));
        slab_1.setData(create(100, (byte)4));

        final Slab[] slabs = {slab_0, slab_1};

        // overlaps both slabs
        final Rectangle destRect = new Rectangle(3, 18, 4, 4);
        final ProductData destBuffer = create(16, (byte)5);

        SlabCache.copyData(destBuffer, destRect, slabs);

        assertEquals(3, destBuffer.getElemIntAt(0));    // (0,0)
        assertEquals(3, destBuffer.getElemIntAt(5));    // (1,1)
        assertEquals(4, destBuffer.getElemIntAt(10));   // (2,2)
        assertEquals(4, destBuffer.getElemIntAt(15));   // (3,3)
    }

    @Test
    public void testCopyData_two_slabs_region_intersects_vertically_byte() {
        final Slab slab_0 = new Slab(new Rectangle(0, 10, 10, 10));
        slab_0.setData(create(100, (byte)4));

        final Slab slab_1 = new Slab(new Rectangle(10, 10, 10, 10));
        slab_1.setData(create(100, (byte)5));

        final Slab[] slabs = {slab_0, slab_1};

        // overlaps both slabs
        final Rectangle destRect = new Rectangle(8, 12, 4, 5);
        final ProductData destBuffer = create(20, (byte)6);

        SlabCache.copyData(destBuffer, destRect, slabs);

        assertEquals(4, destBuffer.getElemIntAt(0));    // (0,0)
        assertEquals(4, destBuffer.getElemIntAt(5));    // (1,1)
        assertEquals(5, destBuffer.getElemIntAt(10));   // (2,2)
        assertEquals(5, destBuffer.getElemIntAt(15));   // (3,3)
        assertEquals(4, destBuffer.getElemIntAt(16));   // (0,4)
        assertEquals(5, destBuffer.getElemIntAt(19));   // (3,4)
    }

    @Test
    public void testCopyData_four_slabs_region_intersects_center_short() {
        final Slab slab_0 = new Slab(new Rectangle(100, 120, 10, 10));
        slab_0.setData(create(100, (short)5));

        final Slab slab_1 = new Slab(new Rectangle(110, 120, 10, 10));
        slab_1.setData(create(100, (short)6));

        final Slab slab_2 = new Slab(new Rectangle(100, 130, 10, 10));
        slab_2.setData(create(100, (short)7));

        final Slab slab_3 = new Slab(new Rectangle(110, 130, 10, 10));
        slab_3.setData(create(100, (short)8));

        final Slab[] slabs = {slab_0, slab_1, slab_2, slab_3};

        // overlaps all four slabs
        final Rectangle destRect = new Rectangle(108, 129, 5, 5);
        final ProductData destBuffer = create(25, (short) 9);

        SlabCache.copyData(destBuffer, destRect, slabs);

        assertEquals(5, destBuffer.getElemIntAt(0));    // (0,0)
        assertEquals(5, destBuffer.getElemIntAt(1));    // (1,0)
        assertEquals(6, destBuffer.getElemIntAt(2));    // (2,0)
        assertEquals(7, destBuffer.getElemIntAt(6));    // (1,1)
        assertEquals(8, destBuffer.getElemIntAt(7));    // (2,1)
        assertEquals(7, destBuffer.getElemIntAt(20));   // (4,0)
        assertEquals(8, destBuffer.getElemIntAt(24));   // (4,4)
    }

    @Test
    public void testCopyData_scanline_caching_short() {
        final Slab slab_0 = new Slab(new Rectangle(0, 120, 1145, 1));
        slab_0.setData(create(1145, (short)6));

        final Slab slab_1 = new Slab(new Rectangle(0, 121, 1145, 1));
        slab_1.setData(create(1145, (short)7));

        final Slab slab_2 = new Slab(new Rectangle(0, 122, 1145, 1));
        slab_2.setData(create(1145, (short)8));

        final Slab slab_3 = new Slab(new Rectangle(0, 123, 1145, 1));
        slab_3.setData(create(1145, (short)9));

        final Slab slab_4 = new Slab(new Rectangle(0, 124, 1145, 1));
        slab_4.setData(create(1145, (short)9));

        final Slab[] slabs = {slab_0, slab_1, slab_2, slab_3, slab_4};

        final Rectangle destRect = new Rectangle(1102, 121, 10, 3);
        final ProductData destBuffer = create(30, (short) 10);

        SlabCache.copyData(destBuffer, destRect, slabs);

        assertEquals(7, destBuffer.getElemIntAt(0));    // (0,0)
        assertEquals(8, destBuffer.getElemIntAt(10));   // (1,1)
        assertEquals(9, destBuffer.getElemIntAt(21));   // (2,2)
    }

    @Test
    public void tetSizeInBytes_empty() {
        final SlabCache slabCache = new SlabCache(200, 300, 10, 10, storage);

        assertEquals(0L, slabCache.getSizeInBytes());
    }

    @Test
    public void testGetSizeInBytes_oneSlab() {
        final SlabCache slabCache = new SlabCache(200, 300, 10, 10, storage);
        when(storage.createBuffer(anyInt())).thenReturn(create(200, (short)7));

        final Slab slab = new Slab(new Rectangle(1, 121, 100, 2));
        slab.setData(create(200, (short)-1));
        // trigger slab creation
        slabCache.get(1, 122, 1 ,1);

        assertEquals(424L, slabCache.getSizeInBytes());
    }

    @Test
    public void testGetSizeInBytes_threeSlabs() {
        final SlabCache slabCache = new SlabCache(100, 300, 10, 10, storage);
        when(storage.createBuffer(anyInt())).thenReturn(create(200, (short)7));

        final Slab slab = new Slab(new Rectangle(1, 121, 100, 2));
        slab.setData(create(200, (short)-1));
        // trigger slab creation
        slabCache.get(1, 122, 1 ,1);
        slabCache.get(11, 122, 1 ,1);
        slabCache.get(21, 122, 1 ,1);

        assertEquals(1272L, slabCache.getSizeInBytes());
    }

    private ProductData create(int size, float fillValue) {
        final float[] data = new float[size];
        for (int i = 0; i < data.length;i++) {
            data[i] = fillValue;
        }

        return ProductData.createInstance(data);
    }

    private ProductData create(int size, byte fillValue) {
        final byte[] data = new byte[size];
        for (int i = 0; i < data.length;i++) {
            data[i] = fillValue;
        }

        return ProductData.createInstance(data);
    }

    private ProductData create(int size, short fillValue) {
        final short[] data = new short[size];
        for (int i = 0; i < data.length;i++) {
            data[i] = fillValue;
        }

        return ProductData.createInstance(data);
    }
}
