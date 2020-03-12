package org.esa.snap.core.dataio.cache;

import java.awt.*;
import java.awt.geom.Area;

class TileIndexCalculator {

    private final int tileWidth;
    private final int tileHeight;

    TileIndexCalculator(int tileWidth, int tileHeight) {
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
    }

    TileRegion getTileIndexRegion(Area searchRegion) {
        final Rectangle searchRegionBounds = searchRegion.getBounds();

        final int tileX_min = searchRegionBounds.x / tileWidth;
        final int tileX_max = (searchRegionBounds.x + searchRegionBounds.width - 1) / tileWidth;
        final int tileY_min = searchRegionBounds.y / tileHeight;
        final int tileY_max = (searchRegionBounds.y + searchRegionBounds.height - 1) / tileHeight;

        return new TileRegion(tileX_min, tileX_max, tileY_min, tileY_max);
    }
}
