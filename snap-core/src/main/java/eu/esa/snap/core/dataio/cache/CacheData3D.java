package eu.esa.snap.core.dataio.cache;

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

    public boolean intersects(int[] offsets, int[] shapes) {
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
}
