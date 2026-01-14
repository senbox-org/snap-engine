package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.junit.After;
import org.junit.Test;

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

        final ProductCache productCache = new ProductCache(new TestCacheDataProvider());
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

        CacheManager.dispose();
    }

    @Test
    @STTM("SNAP-4121")
    public void testGetSizeInBytes_twoProducts() {
        ProductCache productCache_1 = mock(ProductCache.class);
        when(productCache_1.getSizeInBytes()).thenReturn(5000L);

        ProductCache productCache_2 = mock(ProductCache.class);
        when(productCache_2.getSizeInBytes()).thenReturn(1870L);

        CacheManager cacheManager = CacheManager.getInstance();
        cacheManager.register(productCache_1);
        cacheManager.register(productCache_2);

        assertEquals(6870, cacheManager.getSizeInBytes());

        CacheManager.dispose();
    }
}
