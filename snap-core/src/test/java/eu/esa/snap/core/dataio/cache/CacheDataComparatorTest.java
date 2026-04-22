package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

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

        final ReverseTimeComparator cacheDataComparator = new ReverseTimeComparator();
        assertEquals(-1, cacheDataComparator.compare(one, two));
        assertEquals(1, cacheDataComparator.compare(two, one));

        // change time so that both have same timestamp
        two.setLastAccessTime(2000);
        assertEquals(0, cacheDataComparator.compare(one, two));
    }

    @Test
    @STTM("SNAP-4184")
    public void testSnapshotConstructor_isolatesSortFromConcurrentMutation() {
        // Regression for: java.lang.IllegalArgumentException: Comparison method violates its
        // general contract!  — raised from TimSort when access times change mid-sort.
        // The snapshot constructor must freeze the compare keys at construction time.
        final CacheData2D a = new CacheData2D(new int[]{0, 0}, new int[]{10, 10});
        final CacheData2D b = new CacheData2D(new int[]{0, 10}, new int[]{10, 10});
        a.setLastAccessTime(1000);
        b.setLastAccessTime(2000);
        final List<TimeStamped> items = Arrays.asList(a, b);

        final ReverseTimeComparator snapshot = new ReverseTimeComparator(items);

        // Mutate access times after the snapshot is taken — mimics what a concurrent
        // reader thread would do while the sort is running.
        a.setLastAccessTime(5000);
        b.setLastAccessTime(500);

        // compare() must still reflect the original (snapshotted) ordering: a (1000) < b (2000).
        assertEquals(-1, snapshot.compare(a, b));
        assertEquals(1, snapshot.compare(b, a));
        assertEquals(0, snapshot.compare(a, a));
    }
}
