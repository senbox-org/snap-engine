package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class VariableCache2D {

    private final VariableDescriptor variableDescriptor;
    private final CacheDataProvider dataProvider;
    private CacheData2D[][] cacheData;

    public VariableCache2D(VariableDescriptor variableDescriptor, CacheDataProvider dataProvider) {
        this.variableDescriptor = variableDescriptor;
        this.dataProvider = dataProvider;
        cacheData = initiateCache(variableDescriptor);
    }

    // package access for testing only tb 2025-11-27
    static CacheData2D[][] initiateCache(VariableDescriptor variableDescriptor) {
        final int numTilesX = (int) Math.ceil((float) variableDescriptor.width / variableDescriptor.tileWidth);
        final int numTilesY = (int) Math.ceil((float) variableDescriptor.height / variableDescriptor.tileHeight);
        final CacheData2D[][] cacheData2D = new CacheData2D[numTilesY][numTilesX];

        int startY = 0;
        int startX = 0;
        for (int i = 0; i < cacheData2D.length; i++) {
            for (int j = 0; j < cacheData2D[i].length; j++) {
                int xMax = startX + variableDescriptor.tileWidth - 1;
                if (xMax >= variableDescriptor.width) {
                    xMax = variableDescriptor.width - 1;
                }

                int yMax = startY + variableDescriptor.tileHeight - 1;
                if (yMax >= variableDescriptor.height) {
                    yMax = variableDescriptor.height - 1;
                }
                cacheData2D[i][j] = new CacheData2D(startX, xMax, startY, yMax);
                startX += variableDescriptor.tileWidth;
            }
            // next tile row tb 2025-12-02
            startX = 0;
            startY += variableDescriptor.tileHeight;
        }

        return cacheData2D;
    }

    public void dispose() {
        for (CacheData2D[] Row : cacheData) {
            Arrays.fill(Row, null);
        }
        cacheData = null;
    }

    public ProductData read(int[] offsets, int[] shapes, int[] targetOffsets, int[] targetShapes, ProductData targetData) throws IOException {
        // check if buffer supplied
        if (targetData == null) {
            final int size = targetShapes[0] * targetShapes[1];
            targetData = ProductData.createInstance(variableDescriptor.dataType, size);
        }

        final Rectangle targetRect = new Rectangle(targetOffsets[1], targetOffsets[0], targetShapes[1], targetShapes[0]);

        final RowCol[] tileLocations = getAffectedTileLocations(offsets, shapes);
        for (RowCol tileLocation : tileLocations) {
            final int row = tileLocation.getCacheRow();
            final int col = tileLocation.getCacheCol();
            final CacheData2D cacheData2D = cacheData[row][col];

            cacheData2D.setCacheDataProvider(dataProvider); // @todo 2 tb/tb bad design, think of something more clever 2025-12-03

            final Rectangle cacheRect = cacheData2D.getBoundingRect();
            final Rectangle intersection = cacheRect.intersection(targetRect);
            if (!intersection.isEmpty()) {
                final int[] srcOffsets = new int[]{intersection.y - cacheData2D.getyMin(), intersection.x - cacheData2D.getxMin()};
                final int[] destOffsets = new int[]{intersection.y - targetOffsets[0], intersection.x - targetOffsets[1]};
                final int[] intersectionShapes = new int[]{intersection.height, intersection.width};
                cacheData2D.copyData(srcOffsets, destOffsets, intersectionShapes, targetShapes[1], targetData);
            }
        }

        // if tile is not cached - load from file
        // for each tile
        // - detect intersecting area with requested geometry
        // - copy data to target buffer
        // - update timestamp
        // return buffer
        return targetData;
    }

    CacheData2D[][] getCacheData() {
        return cacheData;
    }

    RowCol[] getAffectedTileLocations(int[] offsets, int[] shapes) {
        final ArrayList<RowCol> rowCols = new ArrayList<>();

        for (int i = 0; i < cacheData.length; i++) {
            for (int j = 0; j < cacheData[i].length; j++) {
                final CacheData2D current = cacheData[i][j];
                if (current != null) {
                    if (current.intersects(offsets, shapes)) {
                        rowCols.add(new RowCol(i, j));
                    }
                }
            }
        }

        return rowCols.toArray(new RowCol[0]);
    }
}
