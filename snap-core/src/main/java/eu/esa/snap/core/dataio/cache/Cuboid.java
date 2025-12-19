package eu.esa.snap.core.dataio.cache;

class Cuboid {

    private final int z;
    private final int y;
    private final int x;
    private final int depth;
    private final int height;
    private final int width;

    Cuboid(int[] offsets, int[] shapes) {
        z = offsets[0];
        y = offsets[1];
        x = offsets[2];

        depth = shapes[0];
        height = shapes[1];
        width = shapes[2];
    }

    int getZ() {
        return z;
    }

    int getY() {
        return y;
    }

    int getX() {
        return x;
    }

    int getDepth() {
        return depth;
    }

    int getHeight() {
        return height;
    }

    int getWidth() {
        return width;
    }

    /**
     * This method extends the intersection algorithm used by AWT Rectangle to three dimensions
     *
     * @param other the Cuboid to intersect with
     * @return the intersection or empty Cuboid if no intersection
     */
    Cuboid intersection(Cuboid other) {
        int x1 = x;
        if (x < other.x) {
            x1 = other.x;
        }
        int y1 = y;
        if (y < other.y){
            y1 = other.y;
        }
        int z1 = z;
        if (z < other.z) {
            z1 = other.z;
        }

        long x2 = x + width;
        long ox2 = other.x + other.width;
        if (x2 > ox2) {
            x2 = ox2;
        }

        long y2 = y + height;
        long oy2 = other.y + other.height;
        if (y2 > oy2) {
            y2 = oy2;
        }

        long z2 = z + depth;
        long oz2 = other.z + other.depth;
        if (z2 > oz2) {
            z2 = oz2;
        }

        x2 -= x1;
        y2 -= y1;
        z2 -= z1;

        if (x2 < Integer.MIN_VALUE) {
            x2 = Integer.MIN_VALUE;
        }
        if (y2 < Integer.MIN_VALUE) {
            y2 = Integer.MIN_VALUE;
        }
        if (z2 < Integer.MIN_VALUE) {
            z2 = Integer.MIN_VALUE;
        }

        return new Cuboid(new int[]{z1, y1, x1}, new int[]{(int) z2, (int) y2, (int) x2});
    }

    boolean isEmpty() {
        return width <= 0 || height <= 0 || depth <= 0;
    }
}
