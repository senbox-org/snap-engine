package org.esa.snap.lib.openjpeg.dataio;

/**
 * Wrapper class for raw tile image data and image dimensions, using the image data type.
 * <code>T</code> is the type of image data buffer and it can be one of: byte[], short[], int[].
 *
 * @author  Cosmin Cara
 * @since   5.0.0
 */
public class TileImageDescriptor<T> {
    private final int width;
    private final int height;
    private final T dataArray;

    public TileImageDescriptor(int width, int height, T dataArray) {
        this.width = width;
        this.height = height;
        this.dataArray = dataArray;
    }

    /**
     * The expected width of the image.
     */
    public int getWidth() {
        return width;
    }

    /**
     * The expected height of the image.
     */
    public int getHeight() {
        return height;
    }

    /**
     * The image data.
     */
    public T getDataArray() {
        return dataArray;
    }
}
