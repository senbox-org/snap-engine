package org.esa.snap.core.dataio.cache;

class TileBoundaryCalculator {

    private final int tileWidth;
    private final int tileHeight;
    private final int maxRasterX;
    private final int maxRasterY;

    TileBoundaryCalculator(int rasterWidth, int rasterHeight, int tileWidth, int tileHeight) {
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.maxRasterX = rasterWidth -1;
        this.maxRasterY = rasterHeight - 1;
    }

    public TileRegion getBounds(int tileX, int tileY) {
        final int minX = tileX * tileWidth;
        int maxX = (tileX + 1) * tileWidth - 1;
        if (maxX > maxRasterX) {
            maxX = maxRasterX;
        }
        final int minY = tileY * tileHeight;
        int maxY = (tileY +1) * tileHeight - 1;
        if (maxY > maxRasterY) {
            maxY = maxRasterY;
        }
        return new TileRegion(minX, maxX, minY, maxY);
    }
}
