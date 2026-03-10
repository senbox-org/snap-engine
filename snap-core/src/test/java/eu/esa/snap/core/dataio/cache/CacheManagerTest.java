package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CacheManagerTest {

    @After
    public void tearDown() {
        CacheManager.dispose();
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetInstance() {
        final CacheManager instance_1 =  CacheManager.getInstance();
        assertNotNull(instance_1);

        final CacheManager instance_2 =  CacheManager.getInstance();
        assertSame(instance_1, instance_2);
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetInstanceAndDispose() {
        final CacheManager instance_1 =  CacheManager.getInstance();
        assertNotNull(instance_1);

        CacheManager.dispose();

        final CacheManager instance_2 =  CacheManager.getInstance();
        assertNotSame(instance_1, instance_2);
    }

    @Test
    @STTM("SNAP-4121")
    public void testRegisterAndRemove() {
        final CacheManager cacheManager = CacheManager.getInstance();
        assertEquals(0, cacheManager.getNumProductCaches());

        final ProductCache productCache = new ProductCache(new TestCacheDataProvider(new int[] {20, 20}, new int[] {5, 10}, ProductData.TYPE_FLOAT64));
        cacheManager.register(productCache);

        assertEquals(1, cacheManager.getNumProductCaches());

        cacheManager.remove(productCache);
        assertEquals(0, cacheManager.getNumProductCaches());
    }

    @Test
    @STTM("SNAP-4121")
    public void testDisposeIsCalledOnRemove() {
        final CacheManager cacheManager = CacheManager.getInstance();
        final ProductCache productCache = mock(ProductCache.class);

        cacheManager.register(productCache);
        cacheManager.remove(productCache);
        verify(productCache, times(1)).dispose();
    }

    @Test
    @STTM("SNAP-4121")
    public void testDisposeIsPropagatedToProductCaches() {
        ProductCache productCache_1 = mock(ProductCache.class);
        ProductCache productCache_2 = mock(ProductCache.class);

        final CacheManager cacheManager = CacheManager.getInstance();
        cacheManager.register(productCache_1);
        cacheManager.register(productCache_2);

        CacheManager.dispose();

        verify(productCache_1, times(1)).dispose();
        verify(productCache_2, times(1)).dispose();
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetSizeInBytes_empty() {
        CacheManager cacheManager = CacheManager.getInstance();

        assertEquals(0, cacheManager.getSizeInBytes());
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetSizeInBytes_twoProducts() {
        ProductCache productCache_1 = mock(ProductCache.class);
        when(productCache_1.getSizeInBytes()).thenReturn(5000L);

        ProductCache productCache_2 = mock(ProductCache.class);
        when(productCache_2.getSizeInBytes()).thenReturn(1870L);

        final CacheManager cacheManager = CacheManager.getInstance();
        cacheManager.register(productCache_1);
        cacheManager.register(productCache_2);

        assertEquals(6870, cacheManager.getSizeInBytes());
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetAllocated_empty() {
        CacheManager cacheManager = CacheManager.getInstance();

        assertEquals(0, cacheManager.getAllocatedMemory());
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetAllocated_allocatedAndDispose() {
        CacheManager cacheManager = CacheManager.getInstance();

        cacheManager.allocated(1000);
        assertEquals(1000, cacheManager.getAllocatedMemory());

        cacheManager.allocated(2500);
        assertEquals(3500, cacheManager.getAllocatedMemory());

        cacheManager.released(800);
        assertEquals(2700, cacheManager.getAllocatedMemory());

        cacheManager.released(2700);
        assertEquals(0, cacheManager.getAllocatedMemory());
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetAllocated_allocatedAndDispose_productAddsSize() {
        final CacheManager cacheManager = CacheManager.getInstance();

        final ProductCache productCache = mock(ProductCache.class);
        when(productCache.getSizeInBytes()).thenReturn(2698L);

        cacheManager.register(productCache);

        assertEquals(2698, cacheManager.getAllocatedMemory());
    }

    @Test
    @STTM("SNAP-4121")
    public void testRelease() throws IOException {
        final CacheManager cacheManager = CacheManager.getInstance();
        cacheManager.setMemoryLimit(30000);
        cacheManager.setDisposeThreshold(1000);

        final ProductCache productCache = new ProductCache(new TestCacheDataProvider(new int[] {40, 200}, new int[] {10, 20}, ProductData.TYPE_FLOAT32));
        cacheManager.register(productCache);

        // read to start cache filling
        DataBuffer dataBuffer = new DataBuffer(ProductData.createInstance(ProductData.TYPE_FLOAT32, 2000), new int[]{0, 0}, new int[] {20, 100});
        productCache.read("who_cares", new int[]{0,0}, new int[] {20, 100}, dataBuffer );

        long sizeInBytes = cacheManager.getSizeInBytes();
        assertEquals(23360, sizeInBytes);

        // read at different location to trigger cache filling
        dataBuffer = new DataBuffer(ProductData.createInstance(ProductData.TYPE_FLOAT32, 2000), new int[]{30, 70}, new int[] {20, 100});
        productCache.read("who_cares", new int[]{30,70}, new int[] {20, 100}, dataBuffer );

        sizeInBytes = cacheManager.getSizeInBytes();
        assertEquals(28160, sizeInBytes);

        // now trigger release 0peration, next allocation is 4k and will overshoot
        dataBuffer = new DataBuffer(ProductData.createInstance(ProductData.TYPE_FLOAT32, 2000), new int[]{20, 80}, new int[] {20, 100});
        productCache.read("who_cares", new int[]{20, 80}, new int[] {20, 100}, dataBuffer );

        sizeInBytes = cacheManager.getSizeInBytes();
        assertEquals(25760, sizeInBytes);
    }
}
