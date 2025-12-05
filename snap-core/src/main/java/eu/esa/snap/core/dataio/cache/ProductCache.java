package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;
import java.util.HashMap;

public class ProductCache {

    private final HashMap<String, VariableCache2D> variableCacheMap;
    private final CacheDataProvider dataProvider;

    public ProductCache(CacheDataProvider dataProvider) {
        this.dataProvider = dataProvider;
        variableCacheMap = new HashMap<>();
    }

    public void dispose() {
        variableCacheMap.values().forEach(VariableCache2D::dispose);
        variableCacheMap.clear();
    }

    public ProductData read(String bandName, ProductData targetBuffer, int[] offsets, int[] shapes, int[] targetOffsets, int[] targetShapes) throws IOException {
        VariableCache2D variableCache = variableCacheMap.get(bandName);
        if (variableCache == null) {
            final VariableDescriptor variableDescriptor = dataProvider.getVariableDescriptor(bandName);
            variableDescriptor.name = bandName;

            variableCache = new VariableCache2D(variableDescriptor, dataProvider);
            variableCacheMap.put(bandName, variableCache);
            // @todo send allocation message
        }

        return variableCache.read(offsets, shapes, targetOffsets, targetShapes, targetBuffer);
    }
}
