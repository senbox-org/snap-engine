package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ProductCache {

    private final ConcurrentHashMap<String, VariableCache> variableCacheMap;
    private  CacheDataProvider dataProvider;
    private  MemoryUsageTracker memoryUsageTracker;

    public ProductCache(CacheDataProvider dataProvider) {
        this.dataProvider = dataProvider;
        memoryUsageTracker = null;
        variableCacheMap = new ConcurrentHashMap<>();
    }

    public void dispose() {
        variableCacheMap.values().forEach(VariableCache::dispose);
        variableCacheMap.clear();
        dataProvider = null;
        memoryUsageTracker = null;
    }

    public ProductData read(String bandName, int[] offsets, int[] shapes, DataBuffer targetBuffer) throws IOException {
        final VariableCache variableCache = variableCacheMap.computeIfAbsent(bandName, s -> {
            try {
                final VariableDescriptor variableDescriptor = dataProvider.getVariableDescriptor(bandName);
                final CacheContext cacheContext = new CacheContext(variableDescriptor, dataProvider, memoryUsageTracker);
                if (variableDescriptor.layers <= 1) {
                    return new VariableCache2D(cacheContext);
                } else {
                    return new VariableCache3D(cacheContext);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return variableCache.read(offsets, shapes, targetBuffer);
    }

    long getSizeInBytes() {
        long sizeInBytes = 0;
        final Collection<VariableCache> values = variableCacheMap.values();
        for (VariableCache variableCache : values) {
            sizeInBytes += variableCache.getSizeInBytes();
        }

        return sizeInBytes;
    }

    void setMemoryUsageTracker(MemoryUsageTracker memoryUsageTracker) {
        this.memoryUsageTracker = memoryUsageTracker;
    }

    long getLastAccessTime() {
        long lastAccessTime = Long.MIN_VALUE;
        final Collection<VariableCache> variableCaches = variableCacheMap.values();
        for (VariableCache variableCache : variableCaches) {
            final long variableLastAccessTime = variableCache.getLastAccessTime();
            if (variableLastAccessTime > lastAccessTime) {
                lastAccessTime = variableLastAccessTime;
            }
        }
        return lastAccessTime;
    }

    long release(long bytesToRelease) {
        // get oldest variable, i.e. the one with the smallest lastAccessTime
        long lastAccessTime = Long.MAX_VALUE;
        VariableCache cacheToDispose = null;
        final Collection<VariableCache> variableCaches = variableCacheMap.values();
        for (VariableCache variableCache : variableCaches) {
            final long variableLastAccessTime = variableCache.getLastAccessTime();
            if (variableLastAccessTime < lastAccessTime) {
                lastAccessTime = variableLastAccessTime;
                cacheToDispose = variableCache;
            }
        }
        if (cacheToDispose == null) {
            return 0;
        }

        // let it drop the requested amount
        long bytesRelease = cacheToDispose.release(bytesToRelease);
        // if it can't fulfill all - continue with the next.

        // return the number of bytes released
        return 0;
    }
}
