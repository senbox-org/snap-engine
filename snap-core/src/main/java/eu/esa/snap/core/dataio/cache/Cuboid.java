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

    Cuboid intersection(Cuboid other) {
        return new Cuboid(new int[]{0, 0, 0}, new int[]{0, 0, 0});
    }

    boolean isEmpty() {
        return width <= 0 || height <= 0 || depth <= 0;
    }
}
