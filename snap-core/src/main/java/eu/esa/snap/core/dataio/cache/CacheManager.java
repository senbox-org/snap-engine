package eu.esa.snap.core.dataio.cache;

import org.esa.snap.runtime.Config;

import java.util.ArrayList;
import java.util.List;

public class CacheManager implements MemoryUsageTracker {

    private static final long TwoGB = 1024L * 1024 * 1024 * 2;
    private static final int OneMb = 1024 * 1024;
    private static CacheManager instance = null;

    private final List<ProductCache> productCaches;
    private long memoryLimit;
    private long disposeThreshold;
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

        memoryLimit = Config.instance("snap").preferences().getLong("snap.dataio.cache.memoryLimit", TwoGB);
        disposeThreshold = Config.instance("snap").preferences().getLong("snap.dataio.cache.disposeThreshold", OneMb);
    }

    public void setMemoryLimit(long memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public void setDisposeThreshold(long disposeThreshold) {
        this.disposeThreshold = disposeThreshold;
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
        productCaches.clear();
    }

    private synchronized void increaseAllocatedMemory(long size) {
        allocatedMemory += size;
        if (allocatedMemory > memoryLimit) {
            final long toDispose = allocatedMemory - memoryLimit;
            if (toDispose < disposeThreshold) {
                return;
            }

            long disposed = 0;
            // Snapshot access times so the sort is stable even if other threads update
            // them concurrently while this sort runs (prevents TimSort contract violation).
            productCaches.sort(new ReverseTimeComparator(productCaches));
            for (ProductCache productCache : productCaches) {
                disposed += productCache.release(toDispose - disposed);
                if (disposed >= toDispose) {
                    break;
                }
            }
            decreaseAllocatedMemory(disposed);
        }
    }

    private synchronized void decreaseAllocatedMemory(long numBytes) {
        allocatedMemory -= numBytes;
    }
}
