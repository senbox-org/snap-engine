package org.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;

class SlabCache {

    private static final long NO_DATA = -1L;

    private final DataStorage dataStorage;
    private final TileIndexCalculator tileCalculator;
    private final TileBoundaryCalculator tileBoundsCalculator;
    // package access for testing only tb 2020-03-17
    final List<Slab> cache;

    private long lastAccess;
    private AllocationListener listener;

    /**
     * Constructs a SlabCache.
     *
     * @param rasterWidth  - the width of the underlying raster
     * @param rasterHeight - the height of the underlying raster
     * @param tileWidth    - the width of a raster tile
     * @param tileHeight   - the height of a raster tile
     * @param dataStorage  - data access interface
     */
    SlabCache(int rasterWidth, int rasterHeight, int tileWidth, int tileHeight, DataStorage dataStorage) {
        this.dataStorage = dataStorage;
        this.tileCalculator = new TileIndexCalculator(tileWidth, tileHeight);
        this.tileBoundsCalculator = new TileBoundaryCalculator(rasterWidth, rasterHeight, tileWidth, tileHeight);

        cache = new ArrayList<>();
        lastAccess = NO_DATA;
        listener = new NoOpAllocationListener();
    }

    /**
     * Read data covering the raster region passed in. The size of the buffer must match the requested raster size.
     *
     * @param destRect   - the destination rectangle
     * @param destBuffer - the buffer to contain the data
     */
    void read(Rectangle destRect, ProductData destBuffer) {
        final Slab[] slabs = get(destRect.x, destRect.y, destRect.width, destRect.height);
        copyData(destBuffer, destRect, slabs);
    }

    /**
     * Retrieves all slabs that intersect with the loading region supplied.
     *
     * @param x      loading region x coordinate
     * @param y      loading region y coordinate
     * @param width  loading region width
     * @param height loading region hight
     * @return all slabs intersection with the coordinates
     */
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
                        lastAccess = System.currentTimeMillis();
                        slab.setLastAccess(lastAccess);
                        listener.allocated(slab.getSizeInBytes());

                        synchronized (cache) {
                            cache.add(slab);
                        }
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
     * Sets the allocation listener.
     *
     * @param listener - the listener
     */
    void setAllocationListener(AllocationListener listener) {
        this.listener = listener;
    }

    /**
     * Retrieves the total size in bytes allocated by this cache.
     *
     * @return - the size in bytes
     */
    long getSizeInBytes() {
        long totalSize = 0;

        for (final Slab slab : cache) {
            totalSize += slab.getSizeInBytes();
        }
        return totalSize;
    }

    /**
     * Retrieves the last access time for this cache in millisecs since epoch.
     *
     * @return - the last access time
     */
    long getLastAccess() {
        return lastAccess;
    }

    /**
     * Requests to release the number of bytes from the cached data.
     *
     * @param sizeInBytes - the requested size to be freed
     * @return - the actual data size in bytes released - can be larger than the requested size
     */
    long release(long sizeInBytes) {
        long releasedBytes = 0L;

        synchronized (cache) {
            cache.sort((o1, o2) -> (int) (o1.getLastAccess() - o2.getLastAccess()));
            while (releasedBytes <= sizeInBytes && cache.size() > 0) {
                final Slab slab = cache.get(0);
                releasedBytes += slab.getSizeInBytes();
                cache.remove(0);
            }
        }

        if (cache.size() == 0) {
            lastAccess = NO_DATA;
        }

        return releasedBytes;
    }

    /**
     * Completely clears this cache.
     *
     * @return - the number of bytes released during the clear operation
     */
    long clear() {
        long releasedBytes = 0L;

        synchronized (cache) {
            for (final Slab slab : cache) {
                releasedBytes += slab.getSizeInBytes();
                slab.dispose();
            }

            cache.clear();
        }
        lastAccess = NO_DATA;

        return releasedBytes;
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    static void copyData(ProductData targetBuffer, Rectangle destRegion, Slab[] slabs) {
        final Area destArea = new Area(destRegion);
        for (final Slab slab : slabs) {
            final Rectangle slabRegion = slab.getRegion();
            final Area slabArea = new Area(slabRegion);
            slabArea.intersect(destArea);

            final Rectangle intersectBounds = slabArea.getBounds();
            final int sourceX = intersectBounds.x - slabRegion.x;
            final int sourceY = intersectBounds.y - slabRegion.y;
            final int targetX = intersectBounds.x - destRegion.x;
            final int targetY = intersectBounds.y - destRegion.y;

            final Object srcData = slab.getData().getElems();
            final Object destData = targetBuffer.getElems();

            for (int y = 0; y < intersectBounds.height; y++) {
                final int srcPos = sourceX + (sourceY + y) * slabRegion.width;
                final int destPos = targetX + (targetY + y) * destRegion.width;
                System.arraycopy(srcData, srcPos, destData, destPos, intersectBounds.width);
            }
        }
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
        final ArrayList<Slab> resultList = new ArrayList<>(4);  // assuming we have quadratic tiles tb 2020-03-16
        synchronized (cache) {
            for (Slab slab : cache) {
                final Rectangle slabRegion = slab.getRegion();
                if (searchRegion.intersects(slabRegion)) {
                    searchRegion.subtract(new Area(slabRegion));
                    lastAccess = System.currentTimeMillis();
                    slab.setLastAccess(lastAccess);
                    resultList.add(slab);

                    if (searchRegion.isEmpty()) {
                        break;  // done, we have covered the requested data region tb 2020-03-11
                    }
                }
            }
        }

        return resultList;
    }
}