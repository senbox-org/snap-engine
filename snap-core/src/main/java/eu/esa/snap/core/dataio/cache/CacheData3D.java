package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;

import static eu.esa.snap.core.dataio.cache.CacheData.intersectingRange;

class CacheData3D implements CacheData {

    private static final int SIZE_WITHOUT_BUFFER = 384;

    private final int xMin;
    private final int xMax;
    private final int yMin;
    private final int yMax;
    private final int zMin;
    private final int zMax;

    private DataBuffer data;
    private CacheContext context;
    private Cuboid boundingCuboid;
    long timeStamp;

    CacheData3D(int[] offsets, int[] shapes) {
        xMin = offsets[2];
        xMax = xMin + shapes[2] - 1;
        yMin = offsets[1];
        yMax = yMin + shapes[1] - 1;
        zMin = offsets[0];
        zMax = zMin + shapes[0] - 1;
        timeStamp = System.currentTimeMillis();
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

    public long getTimeStamp() {
        return timeStamp;
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
    static void copyDataBuffer(int[] offsets, DataBuffer cacheBuffer, int[] targetOffsets, int[] targetShapes, DataBuffer targetBuffer) {
        final int numLayers = targetShapes[0];
        final int numRows = targetShapes[1];

        int[] srcShapes = cacheBuffer.getShapes();

        final int srcLayerSize = srcShapes[1] * srcShapes[2];    // z-layer size: y*x
        final int srcWidth = srcShapes[2];
        final int rowSize = targetShapes[2];
        final int targetWidth = targetBuffer.getWidth();
        final int targetLayerSize = targetWidth * targetBuffer.getHeight();

        int srcOffset = offsets[0] * srcLayerSize + offsets[1] * srcWidth + offsets[2];
        int destOffset = targetOffsets[0] * targetLayerSize + targetOffsets[1] * targetWidth + targetOffsets[2];

        for (int layer = 0; layer < numLayers; layer++) {
            for (int row = 0; row < numRows; row++) {
                System.arraycopy(cacheBuffer.getData().getElems(), srcOffset, targetBuffer.getData().getElems(), destOffset, rowSize);
                srcOffset += srcWidth;
                destOffset += targetWidth;
            }
        }
    }

    @Override
    public int getSizeInBytes() {
        int size = SIZE_WITHOUT_BUFFER;
        if (data != null) {
            final ProductData productData = data.getData();
            size += productData.getNumElems() * productData.getElemSize();
        }
        return size;
    }

    Cuboid getBoundingCuboid() {
        if (boundingCuboid == null) {
            final int[] offsets = {zMin, yMin, xMin};
            final int[] shapes = {zMax - zMin + 1, yMax - yMin + 1, xMax - xMin + 1};
            boundingCuboid = new Cuboid(offsets, shapes);
        }
        return boundingCuboid;
    }

    void setCacheContext(CacheContext context) {
        this.context = context;
    }

    ProductData getData() {
        if (data == null) {
            return null;
        }
        return data.getData();
    }

    void copyData(int[] srcOffsets, int[] destOffsets, int[] intersectionShapes, DataBuffer targetData) throws IOException {
        ensureData();

        copyDataBuffer(srcOffsets, data, destOffsets, intersectionShapes, targetData);

        // @todo 2 tb maybe encapsulate and add a notify mechanism 2026-01-23
        timeStamp = System.currentTimeMillis();
    }

    private void ensureData() throws IOException {
        synchronized (this) {
            if (data == null) {
                final String name = context.getVariableDescriptor().name;
                final int[] offsets = {zMin, yMin, xMin};
                final Cuboid bounds = getBoundingCuboid();
                final int[] shapes = {bounds.getDepth(), bounds.getHeight(), bounds.getWidth()};
                final CacheDataProvider dataProvider = context.getDataProvider();
                data = dataProvider.readCacheBlock(name, offsets, shapes, null);
                // @todo 2 tb add allocation notification - take care of threading issues! 2026-01-23
            }
        }
    }
}
