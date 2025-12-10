package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.awt.*;
import java.io.IOException;

class CacheData2D {

    private final int xMin;
    private final int xMax;
    private final int yMin;
    private final int yMax;

    private ProductData data;
    private Rectangle boundingRect;
    private CacheContext context;

    CacheData2D(int xMin, int xMax, int yMin, int yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        boundingRect = null;
    }

    public void setCacheContext(CacheContext context) {
        this.context = context;
    }

    boolean intersects(int[] offsets, int[] shapes) {
        final int yMinRequested = offsets[0];
        final int xMinRequested = offsets[1];
        final int yMaxRequested = offsets[0] + shapes[0] - 1;
        final int xMaxRequested = offsets[1] + shapes[1] - 1;

        if (inside_y(yMinRequested) || inside_y(yMaxRequested)) {
            return (inside_x(xMinRequested) || inside_x(xMaxRequested));
        }
        return false;
    }

    boolean inside_y(int y) {
        return y >= yMin && y <= yMax;
    }

    boolean inside_x(int x) {
        return x >= xMin && x <= xMax;
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

    // only for testing tb 2025-12-09
    ProductData getData() {
        return data;
    }

    void copyData(int[] offsets, int[] targetOffsets, int[] targetShapes, int targetWidth, ProductData targetData) throws IOException {
        ensureData();

        final int cacheWidth = getBoundingRect().width;
        copyDataBuffer(offsets, cacheWidth, data, targetOffsets, targetShapes, targetWidth, targetData);
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    // package access for testing only tb 2025-12-04
    static void copyDataBuffer(int[] offsets, int srcWidth, ProductData cacheBuffer, int[] targetOffsets, int[] targetShapes, int targetWidth, ProductData targetData) {
        final int numLines = targetShapes[0];
        final int numCols = targetShapes[1];

        int srcOffset = offsets[0] * srcWidth + offsets[1];
        int destOffset = targetOffsets[0] * targetWidth + targetOffsets[1];
        for (int line = 0; line < numLines; line++) {
            System.arraycopy(cacheBuffer.getElems(), srcOffset, targetData.getElems(), destOffset, numCols);
            srcOffset += srcWidth;
            destOffset += targetWidth;
        }
    }

    private void ensureData() throws IOException {
        synchronized (this) {
            if (data == null) {
                final String name = context.getVariableDescriptor().name;
                final int[] offsets = {yMin, xMin};
                final Rectangle bounds = getBoundingRect();
                final int[] shapes = {bounds.height, bounds.width};
                final CacheDataProvider dataProvider = context.getDataProvider();
                data = dataProvider.readCacheBlock(name, offsets, shapes, data);
            }
        }
    }

    Rectangle getBoundingRect() {
        if (boundingRect == null) {
            boundingRect = new Rectangle(xMin, yMin, xMax - xMin + 1, yMax - yMin + 1);
        }
        return boundingRect;
    }
}
