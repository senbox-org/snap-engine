package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ProductCache {

    private final ConcurrentHashMap<String, VariableCache> variableCacheMap;
    private final CacheDataProvider dataProvider;

    public ProductCache(CacheDataProvider dataProvider) {
        this.dataProvider = dataProvider;
        variableCacheMap = new ConcurrentHashMap<>();
    }

    public void dispose() {
        variableCacheMap.values().forEach(VariableCache::dispose);
        variableCacheMap.clear();
    }

    public ProductData read(String bandName, ProductData targetBuffer, int[] offsets, int[] shapes, int[] targetOffsets, int[] targetShapes) throws IOException {
        final VariableCache variableCache = variableCacheMap.computeIfAbsent(bandName, s -> {
            try {
                final VariableDescriptor variableDescriptor = dataProvider.getVariableDescriptor(bandName);
                variableDescriptor.name = bandName;
                if (variableDescriptor.layers <= 1) {
                    return new VariableCache2D(variableDescriptor, dataProvider);
                } else {
                    return new VariableCache3D(variableDescriptor, dataProvider);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return variableCache.read(offsets, shapes, targetOffsets, targetShapes, targetBuffer);
    }

    long getSizeInBytes() {
        long sizeInBytes = 0;
        Collection<VariableCache> values = variableCacheMap.values();
        for (VariableCache variableCache : values) {
            sizeInBytes += variableCache.getSizeInBytes();
        }

        return sizeInBytes;
    }
}
