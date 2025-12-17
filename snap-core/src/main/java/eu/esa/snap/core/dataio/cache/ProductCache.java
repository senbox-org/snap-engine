package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ProductCache {

    private final ConcurrentHashMap<String, VariableCache2D> variableCacheMap;
    private final CacheDataProvider dataProvider;

    public ProductCache(CacheDataProvider dataProvider) {
        this.dataProvider = dataProvider;
        variableCacheMap = new ConcurrentHashMap<>();
    }

    public void dispose() {
        variableCacheMap.values().forEach(VariableCache2D::dispose);
        variableCacheMap.clear();
    }

    public ProductData read(String bandName, ProductData targetBuffer, int[] offsets, int[] shapes, int[] targetOffsets, int[] targetShapes) throws IOException {
        final VariableCache2D variableCache = variableCacheMap.computeIfAbsent(bandName, s -> {
            try {
                final VariableDescriptor variableDescriptor = dataProvider.getVariableDescriptor(bandName);
                if (variableDescriptor.layers < 1) {
                    return createVariableCache2D(bandName);
                } else {
                   // return createVariableCache3D(bandName);
                    throw new RuntimeException("not implemented");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return variableCache.read(offsets, shapes, targetOffsets, targetShapes, targetBuffer);
    }

    long getSizeInBytes() {
        long sizeInBytes = 0;
        Collection<VariableCache2D> values = variableCacheMap.values();
        for (VariableCache2D variableCache2D : values) {
            sizeInBytes += variableCache2D.getSizeInBytes();
        }

        return sizeInBytes;
    }

    private VariableCache2D createVariableCache2D(String bandName) throws IOException {
        final VariableDescriptor variableDescriptor = dataProvider.getVariableDescriptor(bandName);
        variableDescriptor.name = bandName;

        return new VariableCache2D(variableDescriptor, dataProvider);
    }
}
