package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.awt.*;
import java.io.IOException;

class CacheData2D extends AbstractCacheData {

    private static final int SIZE_WITHOUT_BUFFER = 384;

    private final int xMin;
    private final int xMax;
    private final int yMin;
    private final int yMax;

    private Rectangle boundingRect;

    CacheData2D(int[] offsets, int[] shapes) {
        this.xMin = offsets[1];
        this.xMax = xMin + shapes[1] - 1;
        this.yMin = offsets[0];
        this.yMax = yMin + shapes[0] - 1;
        boundingRect = null;
    }

    boolean intersects(int[] offsets, int[] shapes) {
        final int yMinRequested = offsets[0];
        final int yMaxRequested = offsets[0] + shapes[0] - 1;

        if (intersects_y(yMinRequested, yMaxRequested)) {
            final int xMinRequested = offsets[1];
            final int xMaxRequested = offsets[1] + shapes[1] - 1;
            return (intersects_x(xMinRequested, xMaxRequested));
        }
        return false;
    }

    boolean intersects_y(int testMin, int testMax) {
        return intersectingRange(testMin, testMax, yMin, yMax);
    }

    boolean intersects_x(int testMin, int testMax) {
        return intersectingRange(testMin, testMax, xMin, xMax);
    }

    int getxMin() {
        return xMin;
    }

    int getxMax() {
        return xMax;
    }

    int getyMin() {
        return yMin;
    }

    int getyMax() {
        return yMax;
    }

    void copyData(int[] offsets, int[] targetOffsets, int[] targetShapes, int targetWidth, ProductData targetData) throws IOException {
        // Capture the buffer reference under the same lock used by ensureData / release,
        // so a concurrent release() can't null out `data` between ensureData and the copy.
        final DataBuffer localData = ensureData();

        copyDataBuffer(offsets, localData, targetOffsets, targetShapes, targetWidth, targetData);
    }

    @Override
    public int getSizeInBytes() {
        int size = SIZE_WITHOUT_BUFFER;
        // Snapshot the reference so a concurrent release() can't null it between
        // the null check and the getData() call.
        final DataBuffer snapshot = data;
        if (snapshot != null) {
            final ProductData productData = snapshot.getData();
            size += productData.getNumElems() * productData.getElemSize();
        }
        return size;
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    // package access for testing only tb 2025-12-04
    static void copyDataBuffer(int[] offsets, DataBuffer cacheBuffer, int[] targetOffsets, int[] targetShapes, int targetWidth, ProductData targetData) {
        final int numRows = targetShapes[0];
        final int numCols = targetShapes[1];

        final int srcWidth = cacheBuffer.getWidth();

        int srcOffset = offsets[0] * srcWidth + offsets[1];
        int destOffset = targetOffsets[0] * targetWidth + targetOffsets[1];
        for (int line = 0; line < numRows; line++) {
            System.arraycopy(cacheBuffer.getData().getElems(), srcOffset, targetData.getElems(), destOffset, numCols);
            srcOffset += srcWidth;
            destOffset += targetWidth;
        }
    }

    private DataBuffer ensureData() throws IOException {
        final DataBuffer localData;
        synchronized (this) {
            if (data == null) {
                final String name = context.getVariableDescriptor().name;
                final int[] offsets = {yMin, xMin};
                final Rectangle bounds = getBoundingRect();
                final int[] shapes = {bounds.height, bounds.width};
                final CacheDataProvider dataProvider = context.getDataProvider();
                data = dataProvider.readCacheBlock(name, offsets, shapes, null);
                lastAccessTime = System.currentTimeMillis();
            }
            localData = data;
        }
        trackAllocation(localData);
        return localData;
    }

    private void trackAllocation(DataBuffer localData) {
        final MemoryUsageTracker memoryUsageTracker = context.getMemoryUsageTracker();
        if (memoryUsageTracker != null) {
            memoryUsageTracker.allocated(localData.getSizeInBytes());
        }
    }

    Rectangle getBoundingRect() {
        if (boundingRect == null) {
            boundingRect = new Rectangle(xMin, yMin, xMax - xMin + 1, yMax - yMin + 1);
        }
        return boundingRect;
    }
}
