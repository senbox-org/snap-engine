package org.esa.snap.core.dataio.cache;

import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;

class SlabCache {

    private final int rasterWidth;
    private final int rasterHeight;
    private final int tileWidth;
    private final int tileHeight;

    private final DataStorage dataStorage;
    private final TileIndexCalculator tileCalculator;

    private final List<Slab> cache;

    SlabCache(int rasterWidth, int rasterHeight, int tileWidth, int tileHeight, DataStorage dataStorage) {
        this.rasterWidth = rasterWidth;
        this.rasterHeight = rasterHeight;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.dataStorage = dataStorage;
        this.tileCalculator = new TileIndexCalculator(tileWidth, tileHeight);

        cache = new ArrayList<>();
    }

    Slab[] get(int x, int y, int width, int height) {
        final Area searchRegion = new Area(new Rectangle(x, y, width, height));

        // collect everything we have in the cache tb 2020-03-11
        final ArrayList<Slab> resultList = getCachedData(searchRegion);

        // check if we need to load data tb 2020-03-11
        if (!searchRegion.isEmpty()) {
            final TileRegion tileRegion = tileCalculator.getTileIndexRegion(searchRegion);

            for (int tileX = tileRegion.getTile_X_min(); tileX <= tileRegion.getTile_X_max(); tileX++) {
                for (int tileY = tileRegion.getTile_Y_min(); tileY <= tileRegion.getTile_Y_max(); tileY++) {
                    final int x_load = tileX * tileWidth;
                    final int y_load = tileY * tileHeight;
                    // @todo 1 tb/tb calculate slab region by intersecting with product boundaries. There might be tiles smaller
                    //  than the expected size - on the right and lower product boundary
                    final Slab slab = new Slab(new Rectangle(x_load, y_load, tileWidth, tileHeight));

                    synchronized (dataStorage) {
                        // @todo 1 tb/tb create appropriate buffer and read 2020-03-11
                        dataStorage.readRasterData(x_load, y_load, tileWidth, tileHeight, null);
                        // @todo 1 tb/tb set data to slab 2020-03-11
                    }

                    slab.setLastAccess(System.currentTimeMillis());
                    cache.add(slab);
                    resultList.add(slab);
                }
            }
        }

        return resultList.toArray(new Slab[0]);
    }

    /**
     * Retrieves a list of Slabs covering the searchRegion from Cache
     * ATTENTION!!! - the searchRegion is modified in this method. for every resulting Slab, the region covered is
     * subtracted from the searchRegion so that when the method returns, the searchRegion contains the remaining area
     * not covered by cached data. Or it is empty, which means that all requested data was in the cache.
     *
     * @param searchRegion the region data is requested for
     * @return a list of slabs from cache intersecting with the region
     */
    private ArrayList<Slab> getCachedData(Area searchRegion) {
        final ArrayList<Slab> resultList = new ArrayList<>(4);
        for (Slab slab : cache) {
            final Rectangle slabRegion = slab.getRegion();
            if (searchRegion.intersects(slabRegion)) {
                searchRegion.subtract(new Area(slabRegion));
                resultList.add(slab);

                if (searchRegion.isEmpty()) {
                    break;  // done, we have covered the requested data region tb 2020-03-11
                }
            }
        }

        return resultList;
    }
}