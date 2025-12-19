package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;

import static eu.esa.snap.core.dataio.cache.CacheData.intersectingRange;

class CacheData3D implements CacheData {

    private static final int SIZE_WITHOUT_BUFFER = 192;

    private final int xMin;
    private final int xMax;
    private final int yMin;
    private final int yMax;
    private final int zMin;
    private final int zMax;

    private ProductData data;
    private CacheContext context;

    CacheData3D(int[] offsets, int[] shapes) {
        xMin = offsets[2];
        xMax = xMin + shapes[2] - 1;
        yMin = offsets[1];
        yMax = yMin + shapes[1] - 1;
        zMin = offsets[0];
        zMax = zMin + shapes[0] - 1;
    }

    boolean intersects_z(int testMin, int testMax) {
        return intersectingRange(testMin, testMax, zMin, zMax);
    }

    boolean intersects_y(int testMin, int testMax) {
        return intersectingRange(testMin, testMax, yMin, yMax);
    }

    boolean intersects_x(int testMin, int testMax) {
        return intersectingRange(testMin, testMax, xMin, xMax);
    }

    public int getxMin() {
        return xMin;
    }

    public int getxMax() {
        return xMax;
    }

    public int getyMin() {
        return yMin;
    }

    public int getyMax() {
        return yMax;
    }

    public int getzMin() {
        return zMin;
    }

    public int getzMax() {
        return zMax;
    }

    boolean intersects(int[] offsets, int[] shapes) {
        final int zMin = offsets[0];
        final int zMax = offsets[0] + shapes[0] - 1;

        if (intersects_z(zMin, zMax)) {
            final int yMin = offsets[1];
            final int yMax = offsets[1] + shapes[1] - 1;

            if (intersects_y(yMin, yMax)) {
                final int xMin = offsets[2];
                final int xMax = offsets[2] + shapes[2] - 1;

                return intersects_x(xMin, xMax);
            }
        }
        return false;
    }

    // package access for testing only tb 2025-12-04
    @SuppressWarnings("SuspiciousSystemArraycopy")
    static void copyDataBuffer(int[] offsets, int srcWidth, ProductData cacheBuffer, int[] targetOffsets, int[] targetShapes, int[] targetBufferSizes, ProductData targetBuffer) {
        final int numLayers = targetShapes[0];
        final int numRows = targetShapes[1];

        final int layerSize = targetShapes[1] * targetShapes[2];    // z-layer size: y*x
        final int rowSize = targetShapes[2];
        int srcOffset = offsets[0] * layerSize + offsets[1] * rowSize + offsets[2];

        final int targetLayerSize = targetBufferSizes[1] * targetBufferSizes[2];
        final int targetWidth = targetBufferSizes[2];

        for (int layer = 0; layer < numLayers; layer++) {
            int destOffset = (layer + targetOffsets[0]) * targetLayerSize + targetOffsets[1] * targetWidth + targetOffsets[2];
            for (int row = 0; row < numRows; row++) {
                System.arraycopy(cacheBuffer.getElems(), srcOffset, targetBuffer.getElems(), destOffset, rowSize);
                srcOffset += srcWidth;
                destOffset += targetWidth;
            }
        }
    }

    @Override
    public int getSizeInBytes() {
        int size = SIZE_WITHOUT_BUFFER;
        if (data != null) {
            size += data.getNumElems() * data.getElemSize();
        }
        return size;
    }

    Cuboid getBoundingCuboid() {
        final int[] offsets = {zMin, yMin, xMin};
        final int[] shapes = {zMax - zMin + 1, yMax - yMin + 1, xMax - xMin + 1};
        return new Cuboid(offsets, shapes);
    }

    void setCacheContext(CacheContext context) {
        this.context = context;
    }

    ProductData getData() {
        return data;
    }

    void copyData(int[] srcOffsets, int[] destOffsets, int[] intersectionShapes, int[] targetShapes, ProductData targetData) throws IOException {
        ensureData();

        copyDataBuffer(srcOffsets, xMax - xMin + 1, data, destOffsets, targetShapes, intersectionShapes, targetData);
    }

    private void ensureData() throws IOException {
        synchronized (this) {
            if (data == null) {
                final String name = context.getVariableDescriptor().name;
                final int[] offsets = {zMin, yMin, xMin};
                Cuboid bounds = getBoundingCuboid();
                final int[] shapes = {bounds.getDepth(), bounds.getHeight(), bounds.getWidth()};
                final CacheDataProvider dataProvider = context.getDataProvider();
                data = dataProvider.readCacheBlock(name, offsets, shapes, data);
            }
        }
    }
}
