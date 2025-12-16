package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

class CacheData3D {

    private final int xMin;
    private final int xMax;
    private final int yMin;
    private final int yMax;
    private final int zMin;
    private final int zMax;

    CacheData3D(int[] offsets, int[] shapes) {
        xMin = offsets[2];
        xMax = xMin + shapes[2] - 1;
        yMin = offsets[1];
        yMax = yMin + shapes[1] - 1;
        zMin = offsets[0];
        zMax = zMin + shapes[0] - 1;
    }

    boolean inside_z(int z) {
        return z >= zMin && z <= zMax;
    }

    boolean inside_y(int y) {
        return y >= yMin && y <= yMax;
    }

    boolean inside_x(int x) {
        return x >= xMin && x <= xMax;
    }

     boolean intersects(int[] offsets, int[] shapes) {
        final int zMin = offsets[0];
        final int zMax = offsets[0] + shapes[0] - 1;

        if (inside_z(zMin)|| inside_z(zMax)) {
            final int yMin = offsets[1];
            final int yMax = offsets[1] + shapes[1] - 1;

            if (inside_y(yMin) || inside_y(yMax)) {
                final int xMin = offsets[2];
                final int xMax = offsets[2] + shapes[2] - 1;

                return inside_x(xMin) | inside_x(xMax);
            }
        }
        return false;
    }

    // package access for testing only tb 2025-12-04
    static void copyDataBuffer(int[] offsets, int srcWidth, ProductData cacheBuffer, int[] targetOffsets, int[] targetShapes, int[] targetBufferSizes, ProductData targetBuffer) {
        final int numLayers = targetShapes[0];
        final int numRows = targetShapes[1];
        //final int numCols = targetShapes[2];

        final int layerSize = targetShapes[1] * targetShapes[2];    // z-layer size: y*x
        final int rowSize = targetShapes[2];
        int srcOffset = offsets[0] * layerSize + offsets[1] * rowSize + offsets[2];

        final int targetLayerSize = targetBufferSizes[1] * targetBufferSizes[2];
        final int targetWidth = targetBufferSizes[2];
        int destOffset = targetOffsets[0] * targetLayerSize + targetOffsets[1] * targetWidth + targetOffsets[2];
        for (int layer = 0; layer < numLayers; layer++) {
            for (int row = 0; row < numRows; row++) {
                System.arraycopy(cacheBuffer.getElems(), srcOffset, targetBuffer.getElems(), destOffset, rowSize);
                srcOffset += srcWidth;
                destOffset += targetWidth;
            }
        }
    }

}
