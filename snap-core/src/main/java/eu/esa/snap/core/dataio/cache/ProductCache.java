package eu.esa.snap.core.dataio.cache;

import org.apache.commons.collections.comparators.ReverseComparator;
import org.esa.snap.core.datamodel.ProductData;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ProductCache implements TimeStamped {

    private final ConcurrentHashMap<String, VariableCache> variableCacheMap;
    private CacheDataProvider dataProvider;
    private MemoryUsageTracker memoryUsageTracker;

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

    public long getLastAccessTime() {
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
        long bytesReleased = 0;

        final ArrayList<VariableCache> timeOrderedList = getTimeOrderedList();

        for (VariableCache variableCache : timeOrderedList) {
            bytesReleased += variableCache.release(bytesToRelease);
            if (bytesReleased >= bytesToRelease) {
                return bytesReleased;
            }
        }
        return bytesReleased;
    }

    private @NonNull ArrayList<VariableCache> getTimeOrderedList() {
        final ArrayList<VariableCache> variableCaches = new ArrayList<>(variableCacheMap.values());
        // Snapshot access times so the sort is stable even if other threads update them
        // concurrently while this sort runs (prevents TimSort contract violation).
        variableCaches.sort(new ReverseTimeComparator(variableCaches));
        return variableCaches;
    }
}
