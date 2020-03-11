package org.esa.snap.core.dataio.cache;

class TileRegion {

    private final int tile_X_min;
    private final int tile_X_max;
    private final int tile_Y_min;
    private final int tile_Y_max;

    public TileRegion(int tile_X_min, int tile_X_max, int tile_Y_min, int tile_Y_max) {
        this.tile_X_min = tile_X_min;
        this.tile_X_max = tile_X_max;
        this.tile_Y_min = tile_Y_min;
        this.tile_Y_max = tile_Y_max;
    }

    public int getTile_X_min() {
        return tile_X_min;
    }

    public int getTile_X_max() {
        return tile_X_max;
    }

    public int getTile_Y_min() {
        return tile_Y_min;
    }

    public int getTile_Y_max() {
        return tile_Y_max;
    }
}
