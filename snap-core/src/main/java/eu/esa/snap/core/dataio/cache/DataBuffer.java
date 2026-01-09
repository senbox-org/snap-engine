package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

public class DataBuffer {

    private final ProductData buffer;
    private int[] offsets;
    private int[] shapes;

    public DataBuffer(int productDataType, int[] offsets, int[] shapes) {
        final int size = getSize(shapes);

        buffer = ProductData.createInstance(productDataType, size);
        assignCoordinates(offsets, shapes);
    }

    static int getSize(int[] shapes) {
        int size = 1;
        for (int shape : shapes) {
            size *= shape;
        }
        return size;
    }

    public DataBuffer(ProductData productData, int[] offsets, int[] shapes) {
        final int size = getSize(shapes);
        if (size != productData.getNumElems()) {
            throw new RuntimeException("Buffer size mismatch");
        }

        buffer = productData;
        assignCoordinates(offsets, shapes);
    }

    public ProductData getData() {
        return buffer;
    }

    public int getOffsetX() {
        return offsets[2];
    }

    public int getOffsetY() {
        return offsets[1];
    }

    public int getOffsetLayer() {
        return offsets[0];
    }

    public int getWidth() {
        return shapes[2];
    }

    public int getHeight() {
        return shapes[1];
    }

    public int getNumLayers() {
        return shapes[0];
    }

    public int[] getOffsets() {
        return offsets;
    }

    public int[] getShapes() {
        return shapes;
    }

    private void assignCoordinates(int[] offsets, int[] shapes) {
        if (offsets.length == 2) {
            this.offsets = new int[]{-1, offsets[0], offsets[1]};
            this.shapes = new int[]{-1, shapes[0], shapes[1]};
        } else if (offsets.length == 3) {
            this.offsets = offsets;
            this.shapes = shapes;
        } else {
            throw new RuntimeException("Unsupported offset dimensionality");
        }
    }
}
