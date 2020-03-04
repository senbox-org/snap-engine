package org.esa.snap.core.dataio.cache;

import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

public class SlabCacheTest {

    @Test
    public void testGet_oneSlab_created() {
        final SlabCache slabCache = new SlabCache(100, 200, 10, 10);

        final Slab[] slabs = slabCache.get(2, 3, 3, 3);
        assertEquals(1, slabs.length);

        final Rectangle region = slabs[0].getRegion();
        assertEquals(0, region.x);
        assertEquals(0, region.y);
        assertEquals(10, region.width);
        assertEquals(10, region.height);

        assertTrue(slabs[0].getLastAccess() > -1);
    }

    @Test
    public void testGet_oneSlab_fromCached() {
        final SlabCache slabCache = new SlabCache(100, 200, 10, 10);

        final Slab[] slabs = slabCache.get(2, 3, 3, 3);
        assertEquals(1, slabs.length);
        assertTrue(slabs[0].getLastAccess() > -1);

        final Slab[] slabsSecond = slabCache.get(2, 3, 3, 3);
        assertEquals(1, slabsSecond.length);
        // @todo 1 tb/tb continue here 2020-03-04
//        assertSame(slabs[0], slabsSecond[0]);
        //assertTrue(slabsSecond[0].getLastAccess() > -1);
    }

//    @Test
//    public void testGet_twoSlabs() {
//        final SlabCache slabCache = new SlabCache(100, 200, 10, 10);
//
//        final Slab[] slabs = slabCache.get(2, 3, 3, 3);
//        assertEquals(1, slabs.length);
//
//        final Rectangle region = slabs[0].getRegion();
//        assertEquals(0, region.x);
//        assertEquals(0, region.y);
//        assertEquals(10, region.width);
//        assertEquals(10, region.height);
//
//        assertTrue(slabs[0].getLastAccess() > -1);
//    }
}
