package eu.esa.snap.core.dataio.cache;

class CacheData2D {

    private final int xMin;
    private final int xMax;
    private final int yMin;
    private final int yMax;

    CacheData2D(int xMin, int xMax, int yMin, int yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
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
}
