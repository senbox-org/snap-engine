package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CacheIndexTest {

    @Test
    @STTM("SNAP-4121")
    public void testConstruction() {
        final CacheIndex cacheIndex = new CacheIndex(1, 2);
        assertEquals(1, cacheIndex.getCacheRow());
        assertEquals(2, cacheIndex.getCacheCol());
        assertEquals(-1 , cacheIndex.getCacheLayer());
    }

    @Test
    @STTM("SNAP-4121")
    public void testConstruction_allParameter() {
        final CacheIndex cacheIndex = new CacheIndex(1, 2, 3);
        assertEquals(1 , cacheIndex.getCacheLayer());
        assertEquals(2, cacheIndex.getCacheRow());
        assertEquals(3, cacheIndex.getCacheCol());
    }
}
