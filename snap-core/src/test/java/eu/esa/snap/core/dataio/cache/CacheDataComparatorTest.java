package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CacheDataComparatorTest {

    @Test
    @STTM("SNAP-4121")
    public void testCompare() {
        int[] offsets = new int[]{350, 200};
        int[] shapes = new int[]{10, 10};
        final CacheData2D one = new CacheData2D(offsets, shapes);
        one.setLastAccessTime(2000);

        offsets = new int[]{340, 190};
        shapes = new int[]{10, 10};
        final CacheData2D two = new CacheData2D(offsets, shapes);
        two.setLastAccessTime(3000);

        final CacheDataComparator cacheDataComparator = new CacheDataComparator();
        assertEquals(-1, cacheDataComparator.compare(one, two));
        assertEquals(1, cacheDataComparator.compare(two, one));

        // change time so that both have same timestamp
        two.setLastAccessTime(2000);
        assertEquals(0, cacheDataComparator.compare(one, two));
    }
}
