package eu.esa.snap.core.dataio.cache;

import java.util.Comparator;

public class CacheDataComparator implements Comparator<AbstractCacheData> {

    @Override
    public int compare(AbstractCacheData lhs, AbstractCacheData rhs) {
        if (lhs.getLastAccessTime() > rhs.getLastAccessTime()) {
            return 1;
        } else if (lhs.getLastAccessTime() < rhs.getLastAccessTime()) {
            return -1;
        }

        return 0;
    }
}
