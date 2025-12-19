package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

class VariableCache2D {

    private final VariableDescriptor variableDescriptor;
    private final CacheDataProvider dataProvider;
    private CacheData2D[][] cacheData;

    VariableCache2D(VariableDescriptor variableDescriptor, CacheDataProvider dataProvider) {
        this.variableDescriptor = variableDescriptor;
        this.dataProvider = dataProvider;
        cacheData = initiateCache(variableDescriptor);
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
    long getSizeInBytes() {
        long sizeInBytes = 0;

        for (int i = 0; i < cacheData.length; i++) {
            for (int j = 0; j < cacheData[i].length; j++) {
                sizeInBytes += cacheData[i][j].getSizeInBytes();
            }
        }
        return sizeInBytes;
    }

    void dispose() {
        for (CacheData2D[] Row : cacheData) {
            Arrays.fill(Row, null);
        }
        cacheData = null;
    }

    ProductData read(int[] offsets, int[] shapes, int[] targetOffsets, int[] targetShapes, ProductData targetData) throws IOException {
        // check if buffer supplied
        // @todo check if we want this - maybe we should require a valid buffer. Thus we keep allocations outside this cache tb 2025-12-18
        if (targetData == null) {
            final int size = targetShapes[0] * targetShapes[1];
            targetData = ProductData.createInstance(variableDescriptor.dataType, size);
        }

        final CacheContext cacheContext = new CacheContext(variableDescriptor, dataProvider);
        final Rectangle targetRect = new Rectangle(targetOffsets[1], targetOffsets[0], targetShapes[1], targetShapes[0]);
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
            final int[] destOffsets = new int[]{intersection.y - targetOffsets[0], intersection.x - targetOffsets[1]};
            final int[] intersectionShapes = new int[]{intersection.height, intersection.width};
            cacheData2D.copyData(srcOffsets, destOffsets, intersectionShapes, targetShapes[1], targetData);
        }

        return targetData;
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
}
