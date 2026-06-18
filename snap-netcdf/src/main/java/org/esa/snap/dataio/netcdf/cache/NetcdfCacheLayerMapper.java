package org.esa.snap.dataio.netcdf.cache;

import org.esa.snap.dataio.netcdf.ProfileReadContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class NetcdfCacheLayerMapper {

    private static final String CACHE_LAYER_MAPPINGS_PROPERTY = "cacheLayerMappings";

    private NetcdfCacheLayerMapper() {
    }

    public static void mapBand(ProfileReadContext ctx, String bandName, String cacheKey, int layer) {
        getOrCreateMappings(ctx).put(bandName, new LayerReference(cacheKey, layer));
    }

    public static Map<String, LayerReference> getMappings(ProfileReadContext ctx) {
        final Map<String, LayerReference> mappings = getMappingsOrNull(ctx);
        if (mappings == null) {
            return Collections.emptyMap();
        }
        return new HashMap<>(mappings);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, LayerReference> getOrCreateMappings(ProfileReadContext ctx) {
        Map<String, LayerReference> mappings = getMappingsOrNull(ctx);
        if (mappings == null) {
            mappings = new HashMap<>();
            ctx.setProperty(CACHE_LAYER_MAPPINGS_PROPERTY, mappings);
        }
        return mappings;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, LayerReference> getMappingsOrNull(ProfileReadContext ctx) {
        return (Map<String, LayerReference>) ctx.getProperty(CACHE_LAYER_MAPPINGS_PROPERTY);
    }

    public static final class LayerReference {
        private final String cacheKey;
        private final int layer;

        private LayerReference(String cacheKey, int layer) {
            this.cacheKey = cacheKey;
            this.layer = layer;
        }

        public String getCacheKey() {
            return cacheKey;
        }

        public int getLayer() {
            return layer;
        }
    }
}
