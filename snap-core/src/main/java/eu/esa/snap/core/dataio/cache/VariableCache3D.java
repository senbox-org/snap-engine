package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

class VariableCache3D {

    private CacheData3D[][][] cacheData;
    private final CacheDataProvider dataProvider;
    private final VariableDescriptor variableDescriptor;

    VariableCache3D(VariableDescriptor variableDescriptor, CacheDataProvider dataProvider) {
        this.dataProvider = dataProvider;
        this.variableDescriptor = variableDescriptor;

        cacheData = initiateCache(variableDescriptor);
    }

    // only for testing tb 2025-12-18
    CacheData3D[][][] getCacheData() {
        return cacheData;
    }

    void dispose() {
        if (cacheData != null) {
            for (CacheData3D[][] cacheLayer : cacheData) {
                for (CacheData3D[] cacheLine : cacheLayer) {
                    Arrays.fill(cacheLine, null);
                }
            }
        }
        cacheData = null;
    }

    static CacheData3D[][][] initiateCache(VariableDescriptor descriptor) {
        final int numTilesX = (int) Math.ceil((float) descriptor.width / descriptor.tileWidth);
        final int numTilesY = (int) Math.ceil((float) descriptor.height / descriptor.tileHeight);
        final int numTilesZ = (int) Math.ceil((float) descriptor.layers / descriptor.tileLayers);

        int startZ = 0;
        int startY = 0;
        int startX = 0;

        final CacheData3D[][][] cacheData3D = new CacheData3D[numTilesZ][numTilesY][numTilesX];
        for (int k = 0; k < cacheData3D.length; k++) {
            for (int j = 0; j < cacheData3D[0].length; j++) {
                for (int i = 0; i < cacheData3D[0][0].length; i++) {
                    int xMax = startX + descriptor.tileWidth - 1;
                    if (xMax >= descriptor.width) {
                        xMax = descriptor.width - 1;
                    }

                    int yMax = startY + descriptor.tileHeight - 1;
                    if (yMax >= descriptor.height) {
                        yMax = descriptor.height - 1;
                    }

                    int zMax = startZ + descriptor.tileLayers - 1;
                    if (zMax >= descriptor.layers) {
                        zMax = descriptor.layers - 1;
                    }

                    final int[] offsets = {startZ, startY, startX};
                    final int[] shapes = {zMax - startZ + 1, yMax - startY + 1, xMax - startX + 1};
                    cacheData3D[k][j][i] = new CacheData3D(offsets, shapes);
                    startX += descriptor.tileWidth;
                }
                startX = 0;
                startY += descriptor.tileHeight;
            }
            startY = 0;
            startZ += descriptor.tileLayers;
        }

        return cacheData3D;
    }

    CacheIndex[] getAffectedCacheLocations(int[] offsets, int[] shapes) {
        final ArrayList<CacheIndex> cacheIndices = new ArrayList<>();

        for (int z = 0; z < cacheData.length; z++) {
            for (int y = 0; y < cacheData[0].length; y++) {
                for(int x = 0; x < cacheData[0][0].length; x++) {
                    final CacheData3D current = cacheData[z][y][x];
                    if (current.intersects(offsets, shapes)) {
                        cacheIndices.add(new CacheIndex(z, y, x));
                    }
                }
            }
        }
        return cacheIndices.toArray(new CacheIndex[0]);
    }

    long getSizeInBytes() {
        long sizeInBytes = 0;

        for (int z = 0; z < cacheData.length; z++) {
            for (int y = 0; y < cacheData[z].length; y++) {
                for (int x = 0; x < cacheData[z][y].length; x++) {
                    sizeInBytes += cacheData[z][y][x].getSizeInBytes();
                }
            }
        }
        return sizeInBytes;
    }

    ProductData read(int[] offsets, int[] shapes, int[] targetOffsets, int[] targetShapes, ProductData targetData) throws IOException {
        final CacheContext cacheContext = new CacheContext(variableDescriptor, dataProvider);
        final CacheIndex[] tileLocations = getAffectedCacheLocations(offsets, shapes);
        for (CacheIndex tileLocation : tileLocations) {
            final int row = tileLocation.getCacheRow();
            final int col = tileLocation.getCacheCol();
            final int layer = tileLocation.getCacheLayer();
            final CacheData3D cacheData3D = cacheData[layer][row][col];

            // calculate intersection between cache-cube and target-cube
        }
        return targetData;
    }
}
