package eu.esa.snap.core.dataio.cache;

import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;

public class ReverseTimeComparator implements Comparator<TimeStamped> {

    private final IdentityHashMap<TimeStamped, Long> timeSnapshot;

    /**
     * Creates a comparator that reads each item's access time live on every compare.
     * <p>
     * NOT SAFE when items may be updated by other threads during a sort: the comparator
     * contract can be violated (Java TimSort then throws "Comparison method violates its
     * general contract!"). Prefer {@link #ReverseTimeComparator(Collection)} whenever the
     * list is sorted while readers may concurrently touch the items.
     */
    public ReverseTimeComparator() {
        this.timeSnapshot = null;
    }

    /**
     * Creates a comparator that captures a snapshot of each item's access time at
     * construction. compare() uses the snapshot, so the ordering is stable across the
     * whole sort even if other threads update the items' access times concurrently.
     */
    public ReverseTimeComparator(Collection<? extends TimeStamped> items) {
        this.timeSnapshot = new IdentityHashMap<>(items.size());
        for (TimeStamped item : items) {
            this.timeSnapshot.put(item, item.getLastAccessTime());
        }
    }

    @Override
    public int compare(TimeStamped lhs, TimeStamped rhs) {
        final long lTime = timeSnapshot != null ? timeSnapshot.get(lhs) : lhs.getLastAccessTime();
        final long rTime = timeSnapshot != null ? timeSnapshot.get(rhs) : rhs.getLastAccessTime();
        return Long.compare(lTime, rTime);
    }
}
