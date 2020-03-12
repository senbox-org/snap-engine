package org.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;

class SlabCache {

    private final DataStorage dataStorage;
    private final TileIndexCalculator tileCalculator;
    private final TileBoundaryCalculator tileBoundsCalculator;

    private final List<Slab> cache;

    SlabCache(int rasterWidth, int rasterHeight, int tileWidth, int tileHeight, DataStorage dataStorage) {
        this.dataStorage = dataStorage;
        this.tileCalculator = new TileIndexCalculator(tileWidth, tileHeight);
        this.tileBoundsCalculator = new TileBoundaryCalculator(rasterWidth, rasterHeight, tileWidth, tileHeight);

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
                    final TileRegion bounds = tileBoundsCalculator.getBounds(tileX, tileY);

                    final int regionXMin = bounds.getTile_X_min();
                    final int regionYMin = bounds.getTile_Y_min();
                    final int regionWidth = bounds.getTile_X_max() - regionXMin + 1;
                    final int regionHeight = bounds.getTile_Y_max() - regionYMin + 1;
                    final Rectangle boundsRect = new Rectangle(regionXMin, regionYMin, regionWidth, regionHeight);

                    if (searchRegion.intersects(boundsRect)) {
                        final Slab slab = new Slab(boundsRect);

                        final ProductData buffer = dataStorage.createBuffer(regionWidth * regionHeight);
                        synchronized (dataStorage) {
                            dataStorage.readRasterData(regionXMin, regionYMin, regionWidth, regionHeight, buffer);
                        }
                        slab.setData(buffer);
                        slab.setLastAccess(System.currentTimeMillis());

                        cache.add(slab);
                        resultList.add(slab);

                        searchRegion.subtract(new Area(boundsRect));
                        if (searchRegion.isEmpty()) {
                            // we have covered all search area - we can leave here tb 2020-03-12
                            resultList.toArray(new Slab[0]);
                        }
                    }
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