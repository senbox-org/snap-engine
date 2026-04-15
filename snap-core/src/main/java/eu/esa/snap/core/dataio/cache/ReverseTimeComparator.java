package eu.esa.snap.core.dataio.cache;

import java.util.Comparator;

public class ReverseTimeComparator implements Comparator<TimeStamped> {

    @Override
    public int compare(TimeStamped lhs, TimeStamped rhs) {
        if (lhs.getLastAccessTime() > rhs.getLastAccessTime()) {
            return 1;
        } else if (lhs.getLastAccessTime() < rhs.getLastAccessTime()) {
            return -1;
        }

        return 0;
    }
}
