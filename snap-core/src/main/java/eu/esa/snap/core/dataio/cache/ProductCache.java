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
        // @todo run over map and dispose VariableCaches
        variableCacheMap.clear();
    }

    ProductData read(Band band, int[] offsets, int[] shapes) {
        final String bandName = band.getName();
        final VariableCache variableCache = variableCacheMap.get(bandName);
        if (variableCache == null) {
            // @todo create VariableCache, attach to dataProvider
        }

        return variableCache.read(offsets, shapes);
    }
}
