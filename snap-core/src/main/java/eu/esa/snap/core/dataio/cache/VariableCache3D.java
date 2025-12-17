package eu.esa.snap.core.dataio.cache;

class VariableCache3D {
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
}
