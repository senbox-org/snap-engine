package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

public abstract class AbstractCacheData implements CacheData, TimeStamped {

    DataBuffer data;
    CacheContext context;
    long lastAccessTime;


    static boolean intersectingRange(int testMin, int testMax, int min, int max) {
        return testMax >= min && testMax <= max || testMin >= min && testMin <= max || testMin <= min && testMax >= max;
    }

    // only for testing tb 2025-12-09
    ProductData getData() {
        if (data == null) {
            return null;
        }
        return data.getData();
    }

    @Override
    public long release(long bytesToRelease) {
        if (data == null) {
            return 0;
        }
        final ProductData productData = data.getData();
        final long size = (long) productData.getNumElems() * productData.getElemSize();
        data = null;
        return size;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    void setCacheContext(CacheContext context) {
        this.context = context;
    }
}
