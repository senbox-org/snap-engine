package eu.esa.snap.core.dataio.cache;

import java.util.ArrayList;
import java.util.List;

public class CacheManager implements MemoryUsageTracker {

    private static CacheManager instance = null;

    private final List<ProductCache> productCaches;
    // @todo 1 tb/tb make adjustable 2026-02-09
    private final long memoryLimit = 1024L * 1024 * 1024 * 2;
    private long disposeThreshold = 1024L * 1024;
    private long allocatedMemory;


    public static CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }

    public static void dispose() {
        if (instance != null) {
            instance.disposeCache();
        }
        instance = null;
    }

    CacheManager() {
        productCaches = new ArrayList<>();
        allocatedMemory = 0;
    }

    public void register(ProductCache productCache) {
        productCache.setMemoryUsageTracker(this);
        productCaches.add(productCache);

        increaseAllocatedMemory(productCache.getSizeInBytes());
    }

    public int getNumProductCaches() {
        return productCaches.size();
    }

    public long getSizeInBytes() {
        long sizeInBytes = 0;
        for (ProductCache productCache : productCaches) {
            sizeInBytes += productCache.getSizeInBytes();
        }
        return sizeInBytes;
    }

    public void remove(ProductCache productCache) {
        boolean removed = productCaches.remove(productCache);
        if (removed) {
            decreaseAllocatedMemory(productCache.getSizeInBytes());
            productCache.dispose();
        }
    }

    @Override
    public void allocated(long numBytes) {
        increaseAllocatedMemory(numBytes);
    }

    @Override
    public void released(long numBytes) {
        decreaseAllocatedMemory(numBytes);
    }

    public long getAllocatedMemory() {
        return allocatedMemory;
    }

    void disposeCache() {
        for (final ProductCache productCache : productCaches) {
            productCache.dispose();
        }
    }

    private synchronized void increaseAllocatedMemory(long size) {
        allocatedMemory += size;
        if (allocatedMemory > memoryLimit) {
            final long toDispose = allocatedMemory - memoryLimit;
            if (toDispose < disposeThreshold) {
                return;
            }

            // @todo 1 Move to separate worker thread tb 2026-03-09
            long lastAccessTime = Long.MIN_VALUE;
            ProductCache oldestProduct = null;
            for (ProductCache productCache : productCaches) {
                final long productLastAccessTime = productCache.getLastAccessTime();
                if (productLastAccessTime > lastAccessTime) {
                    oldestProduct = productCache;
                }
            }

            if (oldestProduct != null) {
                final long released = oldestProduct.release(toDispose);

                // @todo 1 tb check if released data is larger or equal than requested. If not
                // continue with next product, until goal is reached. tb 2026-03-09
                allocatedMemory -= released;
            }

        }
    }

    private synchronized void decreaseAllocatedMemory(long numBytes) {
        allocatedMemory -= numBytes;
    }
}
