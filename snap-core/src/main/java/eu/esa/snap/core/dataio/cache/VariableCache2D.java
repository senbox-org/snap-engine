package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

class VariableCache2D implements VariableCache {

    private final VariableDescriptor variableDescriptor;
    private final CacheDataProvider dataProvider;
    private final MemoryUsageTracker memoryUsageTracker;
    private CacheData2D[][] cacheData;
    private long lastAccessTime;

    VariableCache2D(CacheContext cacheContext) {
        variableDescriptor = cacheContext.getVariableDescriptor();
        dataProvider = cacheContext.getDataProvider();
        memoryUsageTracker = cacheContext.getMemoryUsageTracker();
        cacheData = initiateCache(variableDescriptor);

        final long sizeInBytes = getSizeInBytes();
        memoryUsageTracker.allocated(sizeInBytes);
        lastAccessTime = System.currentTimeMillis();
    }

    static CacheData2D[][] initiateCache(VariableDescriptor variableDescriptor) {
        final int numTilesX = (int) Math.ceil((float) variableDescriptor.width / variableDescriptor.tileWidth);
        final int numTilesY = (int) Math.ceil((float) variableDescriptor.height / variableDescriptor.tileHeight);
        final CacheData2D[][] cacheData2D = new CacheData2D[numTilesY][numTilesX];

        int startY = 0;
        int startX = 0;
        for (int j = 0; j < cacheData2D.length; j++) {
            for (int i = 0; i < cacheData2D[j].length; i++) {
                int xMax = startX + variableDescriptor.tileWidth - 1;
                if (xMax >= variableDescriptor.width) {
                    xMax = variableDescriptor.width - 1;
                }

                int yMax = startY + variableDescriptor.tileHeight - 1;
                if (yMax >= variableDescriptor.height) {
                    yMax = variableDescriptor.height - 1;
                }
                final int[] offsets = {startY, startX};
                final int[] shapes = {yMax - startY + 1, xMax - startX + 1};
                cacheData2D[j][i] = new CacheData2D(offsets, shapes);
                startX += variableDescriptor.tileWidth;
            }
            // next tile row tb 2025-12-02
            startX = 0;
            startY += variableDescriptor.tileHeight;
        }

        return cacheData2D;
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public long getSizeInBytes() {
        long sizeInBytes = 0;

        for (int i = 0; i < cacheData.length; i++) {
            for (int j = 0; j < cacheData[i].length; j++) {
                if (cacheData[i][j] != null) {
                    sizeInBytes += cacheData[i][j].getSizeInBytes();
                }
            }
        }
        return sizeInBytes;
    }

    @Override
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void dispose() {
        for (CacheData2D[] cacheRow : cacheData) {
            long sizeInBytes = 0;
            for (CacheData2D cacheData : cacheRow) {
                sizeInBytes += cacheData.getSizeInBytes();
            }
            Arrays.fill(cacheRow, null);
            memoryUsageTracker.released(sizeInBytes);
        }
        cacheData = null;
    }

    @Override
    public long release(long bytesToRelease) {
        long released = 0;

        final ArrayList<CacheData2D> timeOrderedList = getTimeOrderedList();
        for (CacheData2D cacheData2D : timeOrderedList) {
            released += cacheData2D.release(bytesToRelease);
            // update time stamp tb 2026-03-09
            cacheData2D.setLastAccessTime(System.currentTimeMillis());
            if (released >= bytesToRelease) {
                break;
            }
        }

        lastAccessTime = System.currentTimeMillis();

        return released;
    }

    public ProductData read(int[] offsets, int[] shapes, DataBuffer targetBuffer) throws IOException {
        lastAccessTime = System.currentTimeMillis();

        final CacheContext cacheContext = new CacheContext(variableDescriptor, dataProvider, memoryUsageTracker);
        final Rectangle targetRect = new Rectangle(targetBuffer.getOffsetX(), targetBuffer.getOffsetY(), targetBuffer.getWidth(), targetBuffer.getHeight());

        final CacheIndex[] tileLocations = getAffectedCacheLocations(offsets, shapes);
        for (CacheIndex tileLocation : tileLocations) {
            final int row = tileLocation.getCacheRow();
            final int col = tileLocation.getCacheCol();
            final CacheData2D cacheData2D = cacheData[row][col];

            final Rectangle cacheRect = cacheData2D.getBoundingRect();
            final Rectangle intersection = cacheRect.intersection(targetRect);
            if (intersection.isEmpty()) {
                continue;
            }

            cacheData2D.setCacheContext(cacheContext); // @todo 2 tb/tb bad design, think of something more clever 2025-12-03

            final int[] srcOffsets = new int[]{intersection.y - cacheData2D.getyMin(), intersection.x - cacheData2D.getxMin()};
            final int[] destOffsets = new int[]{intersection.y - targetBuffer.getOffsetY(), intersection.x - targetBuffer.getOffsetX()};
            final int[] intersectionShapes = new int[]{intersection.height, intersection.width};
            cacheData2D.copyData(srcOffsets, destOffsets, intersectionShapes, targetBuffer.getWidth(), targetBuffer.getData());
            cacheData2D.setLastAccessTime(lastAccessTime);
        }

        return targetBuffer.getData();
    }

    // only for testing tb 2025-12-10
    CacheData2D[][] getCacheData() {
        return cacheData;
    }

    CacheIndex[] getAffectedCacheLocations(int[] offsets, int[] shapes) {
        final ArrayList<CacheIndex> cacheIndices = new ArrayList<>();

        for (int y = 0; y < cacheData.length; y++) {
            for (int x = 0; x < cacheData[y].length; x++) {
                final CacheData2D current = cacheData[y][x];
                if (current.intersects(offsets, shapes)) {
                    cacheIndices.add(new CacheIndex(y, x));
                }
            }
        }

        return cacheIndices.toArray(new CacheIndex[0]);
    }

    ArrayList<CacheData2D> getTimeOrderedList() {
        final ArrayList<CacheData2D> cacheDataList = new ArrayList<>(cacheData.length * cacheData[0].length);
        for (CacheData2D[] cacheLine : cacheData) {
            Collections.addAll(cacheDataList, cacheLine);
        }

        cacheDataList.sort(new ReverseTimeComparator());

        return cacheDataList;
    }
}

