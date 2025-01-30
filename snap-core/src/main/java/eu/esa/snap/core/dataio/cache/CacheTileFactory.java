package eu.esa.snap.core.dataio.cache;

import static eu.esa.snap.core.dataio.cache.TileType.TYPE_NETCDF_ARRAY;
import static eu.esa.snap.core.dataio.cache.TileType.TYPE_PRODUCT_DATA;

public class CacheTileFactory {

    private static TileType tileType = null;

    public static void initialize(TileType desiredType) {
        tileType = desiredType;
    }

    // rectangle - region
    // (optional) layer
    // data type
    public static CacheTile create() {
        if (tileType == TYPE_PRODUCT_DATA) {
            return new ProductDataTile();
        } else if (tileType == TYPE_NETCDF_ARRAY) {
            return new NCArrayTile();
        }

        throw new RuntimeException("CacheTileFactory not initialized");
    }
}
