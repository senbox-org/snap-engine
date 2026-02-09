package eu.esa.snap.core.dataio.cache;

import java.util.ArrayList;
import java.util.List;

public class CacheManager implements MemoryUsageTracker {

    private static CacheManager instance = null;

    private final List<ProductCache> productCaches;
    // @todo 1 tb/tb make adjustable 2026-02-09
    private final long memoryLimit = 1024 * 1024 * 1024 * 2;
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
    public void allocate(long numBytes) {
        increaseAllocatedMemory(numBytes);
    }

    @Override
    public void free(long numBytes) {
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
            // @todo start dispose oldest cache blocks
            final long toDispose = allocatedMemory - memoryLimit;
        }
    }

    private synchronized void decreaseAllocatedMemory(long numBytes) {
        allocatedMemory -= numBytes;
    }
}
