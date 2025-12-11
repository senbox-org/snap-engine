package eu.esa.snap.core.dataio.cache;

import java.util.ArrayList;
import java.util.List;

public class CacheManager {

    private static CacheManager instance = null;

    private final List<ProductCache> productCaches;

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
    }

    public void register(ProductCache productCache) {
        productCaches.add(productCache);
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
            productCache.dispose();
        }
    }

    void disposeCache() {
        for (final ProductCache productCache : productCaches) {
            productCache.dispose();
        }
    }
}
