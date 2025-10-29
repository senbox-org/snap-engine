package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;

import java.util.HashMap;

public class ProductCache {

    private final HashMap<String, VariableCache> variableCacheMap;
    private final CacheDataProvider dataProvider;

    public ProductCache(CacheDataProvider dataProvider) {
        this.dataProvider = dataProvider;
        variableCacheMap = new HashMap<>();
    }

    public void dispose() {
        variableCacheMap.forEach((key, value) -> dispose());
        variableCacheMap.clear();
    }

    public ProductData read(Band band, int[] offsets, int[] shapes) {
        final String bandName = band.getName();
        VariableCache variableCache = variableCacheMap.get(bandName);
        if (variableCache == null) {
            final VariableDescriptor variableDescriptor = dataProvider.getVariableDescriptor(bandName);
            variableCache = new VariableCache(variableDescriptor);
            variableCacheMap.put(bandName, variableCache);
            // @todo send allocation message
        }

        return variableCache.read(offsets, shapes);
    }
}
