package eu.esa.snap.core.dataio.cache;

class CacheIndex {

    private final int cacheRow;
    private final int cacheCol;
    private final int cacheLayer;

    CacheIndex(int row, int col) {
        cacheRow = row;
        cacheCol = col;
        cacheLayer = -1;
    }

    CacheIndex(int layer, int row, int col) {
        cacheLayer = layer;
        cacheRow = row;
        cacheCol = col;
    }

    int getCacheRow() {
        return cacheRow;
    }

    int getCacheCol() {
        return cacheCol;
    }

    int getCacheLayer() {
        return cacheLayer;
    }
}
