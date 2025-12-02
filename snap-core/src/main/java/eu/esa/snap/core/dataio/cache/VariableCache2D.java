package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.util.ArrayList;
import java.util.Arrays;

public class VariableCache2D {

    private CacheData2D[][] cacheData;

    public VariableCache2D(VariableDescriptor variableDescriptor) {
        cacheData = initiateCache(variableDescriptor);
    }

    // package access for testing only tb 2025-11-27
    static CacheData2D[][] initiateCache(VariableDescriptor variableDescriptor) {
        int numTilesX = (int) Math.ceil((float) variableDescriptor.width / variableDescriptor.tileWidth);
        int numTilesY = (int) Math.ceil((float) variableDescriptor.height / variableDescriptor.tileHeight);
        return new CacheData2D[numTilesY][numTilesX];
    }

    public void dispose() {
        for (CacheData2D[] Row : cacheData) {
            Arrays.fill(Row, null);
        }
        cacheData = null;
    }

    public ProductData read(int[] offsets, int[] shapes) {
        // detect tileX/tileY affected

        // if tile is not cached - load from file
        // for each tile
        // - detect intersecting area with requested geometry
        // - copy data to target buffer
        // - update timestamp
        // return buffer
        return null;
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
