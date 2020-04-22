package org.esa.snap.lib.openjpeg.dataio;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import org.esa.snap.core.util.SystemUtils;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for various operations
 *
 * @author  Cosmin Cara
 * @since   5.0.0
 */
public class RasterUtils {

    private static final Map<String, Integer> extensions;

    private static final int SIZE_OF_SHORT = 2;
    private static final int SIZE_OF_INT = 4;

    static {
        extensions = new HashMap<String, Integer>() {{
            put("pgx", 11);
            put("pnm", 10);
            put("pgm", 10);
            put("ppm", 10);
            put("bmp", 12);
            put("tif", 14);
            put("raw", 15);
            put("rawl", 18);
            put("tga", 16);
            put("png", 17);
            put("j2k", 0);
            put("jp2", 1);
            put("jpt", 2);
            put("j2c", 0);
            put("jpc", 0);
        }};
    }

    /**
     * Reads a region of the given file as a byte array.
     *
     * @param path  The source file
     * @param roi   The region of interest
     * @return      A tile image descriptor holding read dimensions and buffer data.
     */
    static TileImageDescriptor<byte[]> readAsByteArray(Path path, Rectangle roi) throws IOException {
        int size = (int) Files.size(path);
        ByteBuffer buf;
        int width, height;
        byte[] values;
        TileImageDescriptor<byte[]> descriptor;
        try (RandomAccessFile in = new RandomAccessFile(path.toFile(), "r")) {
            try (FileChannel file = in.getChannel()) {
                buf = file.map(FileChannel.MapMode.READ_ONLY, 0, size);
                width = buf.getInt();
                height = buf.getInt();
                int offset = 8;
                int byteLen = size - offset;
                if (roi == null) {
                    values = new byte[byteLen];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = buf.get();
                    }
                } else {
                    values = new byte[roi.width * roi.height];
                    int srcPos;
                    int dstPos;
                    int maxVal = Math.min(roi.y + roi.height, height);
                    int fileOffset;
                    for (int col = roi.y; col < maxVal; col++) {
                        try {
                            srcPos = roi.x + col * width;
                            dstPos = (col - roi.y) * roi.width;
                            if (srcPos < byteLen && dstPos < values.length) {
                                fileOffset = offset + srcPos;
                                for (int i = 0; i < Math.min(roi.width, byteLen - srcPos); i++) {
                                    values[dstPos + i] = buf.get(fileOffset + i);
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    width = roi.width;
                    height = roi.height;
                }
                descriptor = new TileImageDescriptor<>(width, height, values);
            }
            in.close();
        }
        return descriptor;
    }
    /**
     * Reads a region of the given file as a short array.
     *
     * @param path  The source file
     * @param roi   The region of interest
     * @return      A tile image descriptor holding read dimensions and buffer data.
     */
    static TileImageDescriptor<short[]> readAsShortArray(Path path, Rectangle roi) throws IOException {
        int size = (int) Files.size(path);
        ByteBuffer buf;
        int width, height;
        short[] values;
        TileImageDescriptor<short[]> descriptor;
        try (RandomAccessFile in = new RandomAccessFile(path.toFile(), "r")) {
            try (FileChannel file = in.getChannel()) {
                buf = file.map(FileChannel.MapMode.READ_ONLY, 0, size);
                width = buf.getInt();
                height = buf.getInt();
                int offset = 2 * SIZE_OF_INT;
                int shortLen = (size - offset) / SIZE_OF_SHORT;
                if (roi == null) {
                    values = new short[shortLen];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = buf.getShort();
                    }
                } else {
                    values = new short[roi.width * roi.height];
                    int srcPos;
                    int dstPos;
                    int maxVal = Math.min(roi.y + roi.height, height);
                    int fileOffset;
                    for (int col = roi.y; col < maxVal; col++) {
                        try {
                            srcPos = roi.x + col * width;
                            dstPos = (col - roi.y) * roi.width;
                            if (srcPos < shortLen && dstPos < values.length) {
                                fileOffset = offset + srcPos * SIZE_OF_SHORT;
                                for (int i = 0; i < Math.min(roi.width, shortLen - srcPos); i++) {
                                    values[dstPos + i] = buf.getShort(fileOffset + i * SIZE_OF_SHORT);
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    width = roi.width;
                    height = roi.height;
                }
                descriptor = new TileImageDescriptor<>(width, height, values);
            }
            in.close();
        }
        return descriptor;
    }
    /**
     * Reads a region of the given file as an int array.
     *
     * @param path  The source file
     * @param roi   The region of interest
     * @return      A tile image descriptor holding read dimensions and buffer data.
     */
    static TileImageDescriptor<int[]> readAsIntArray(Path path, Rectangle roi) throws IOException {
        int size = (int) Files.size(path);
        ByteBuffer buf;
        int width, height;
        int[] values;
        TileImageDescriptor<int[]> descriptor;
        try (RandomAccessFile in = new RandomAccessFile(path.toFile(), "r")) {
            try (FileChannel file = in.getChannel()) {
                buf = file.map(FileChannel.MapMode.READ_ONLY, 0, size);
                width = buf.getInt();
                height = buf.getInt();
                int offset = 2 * SIZE_OF_INT;
                int intLen = (size - offset) / SIZE_OF_INT;
                if (roi == null) {
                    values = new int[intLen];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = buf.getInt();
                    }
                } else {
                    values = new int[roi.width * roi.height];
                    int srcPos;
                    int dstPos;
                    int maxVal = Math.min(roi.y + roi.height, height);
                    int fileOffset;
                    for (int col = roi.y; col < maxVal; col++) {
                        try {
                            srcPos = roi.x + col * width;
                            dstPos = (col - roi.y) * roi.width;
                            if (srcPos < intLen && dstPos < values.length) {
                                fileOffset = offset + srcPos * SIZE_OF_INT;
                                for (int i = 0; i < Math.min(roi.width, intLen - srcPos); i++) {
                                    values[dstPos + i] = buf.getShort(fileOffset + i * SIZE_OF_INT);
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    width = roi.width;
                    height = roi.height;
                }
                descriptor = new TileImageDescriptor<>(width, height, values);
            }
            in.close();
        }
        return descriptor;
    }

    /**
     * Extracts a range from the given int array and returns it as a byte array.
     * @param values    The source values
     * @param offset    The start position (offset) from which to extract values
     * @param length    How many values to extract
     */
    private static byte[] extractRangeAsByteArray(int[] values, int offset, int length) {
        if (values == null || values.length < offset + length || offset < 0 || length <= 0) {
            throw new RuntimeException("Invalid arguments");
        }
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte) values[offset + i];
        }
        return result;
    }
    /**
     * Extracts a range from the given int array and returns it as a short array.
     * @param values    The source values
     * @param offset    The start position (offset) from which to extract values
     * @param length    How many values to extract
     */
    private static short[] extractRangeAsShortArray(int[] values, int offset, int length) {
        if (values == null || values.length < offset + length || offset < 0 || length <= 0) {
            throw new RuntimeException("Invalid arguments");
        }
        short[] result = new short[length];
        for (int i = 0; i < length; i++) {
            result[i] = (short) values[offset + i];
        }
        return result;
    }
    /**
     * Returns a DataBuffer of bytes from the intersection of the given int array and the region of interest.
     * @param pixels        The source values (the length = imageWidth * imageHeight)
     * @param imageWidth    The initial image width
     * @param imageHeight   The initial image height
     * @param roi           The region of interest
     */
    static DataBufferByte extractROIAsByteBuffer(int[] pixels, int imageWidth, int imageHeight, Rectangle roi) {
        byte[] values;
        if (roi != null) {
            values = new byte[roi.width * roi.height];
            int srcPos;
            int dstPos;
            int maxVal = Math.min(roi.y + roi.height, imageHeight);
            for (int col = roi.y; col < maxVal; col++) {
                try {
                    srcPos = roi.x + col * imageWidth;
                    dstPos = (col - roi.y) * roi.width;
                    if (srcPos < pixels.length && dstPos < values.length) {
                        for (int i = 0; i < Math.min(roi.width, pixels.length - srcPos) - dstPos; i++) {
                            values[dstPos + i] = (byte) pixels[srcPos + i];
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    SystemUtils.LOG.warning("ROI fell outside the extracted range [" + e.getMessage() + "]");
                }
            }
        } else {
            values = extractRangeAsByteArray(pixels, 0, pixels.length);
        }
        return new DataBufferByte(values, values.length);
    }
    /**
     * Returns a DataBuffer of unsigned shorts from the intersection of the given int array and the region of interest.
     * @param pixels        The source values (the length = imageWidth * imageHeight)
     * @param imageWidth    The initial image width
     * @param imageHeight   The initial image height
     * @param roi           The region of interest
     */
    static DataBufferUShort extractROIAsUShortBuffer(int[] pixels, int imageWidth, int imageHeight, Rectangle roi) {
        short[] values;
        if (roi != null) {
            values = new short[roi.width * roi.height];
            int srcPos;
            int dstPos;
            int maxVal = Math.min(roi.y + roi.height, imageHeight);
            for (int col = roi.y; col < maxVal; col++) {
                try {
                    srcPos = roi.x + col * imageWidth;
                    dstPos = (col - roi.y) * roi.width;
                    if (srcPos < pixels.length && dstPos < values.length) {
                        for (int i = 0; i < Math.min(roi.width, pixels.length - srcPos); i++) {
                            values[dstPos + i] = (short) pixels[srcPos + i];
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    SystemUtils.LOG.warning("ROI fell outside the extracted range [" + e.getMessage() + "]");
                }
            }
        } else {
            values = extractRangeAsShortArray(pixels, 0, pixels.length);
        }
        return new DataBufferUShort(values, values.length);
    }
    /**
     * Returns a DataBuffer of shorts from the intersection of the given int array and the region of interest.
     * @param pixels        The source values (the length = imageWidth * imageHeight)
     * @param imageWidth    The initial image width
     * @param imageHeight   The initial image height
     * @param roi           The region of interest
     */
    static DataBufferShort extractROIAsShortBuffer(int[] pixels, int imageWidth, int imageHeight, Rectangle roi) {
        short[] values;
        if (roi != null) {
            values = new short[roi.width * roi.height];
            int srcPos;
            int dstPos;
            int maxVal = Math.min(roi.y + roi.height, imageHeight);
            for (int col = roi.y; col < maxVal; col++) {
                try {
                    srcPos = roi.x + col * imageWidth;
                    dstPos = (col - roi.y) * roi.width;
                    if (srcPos < pixels.length && dstPos < values.length) {
                        //for (int i = 0; i < Math.min(roi.width, pixels.length - srcPos) - dstPos; i++) {
                        for (int i = 0; i < Math.min(roi.width, pixels.length - srcPos); i++) {
                            values[dstPos + i] = (short) pixels[srcPos + i];
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    SystemUtils.LOG.warning("ROI fell outside the extracted range [" + e.getMessage() + "]");
                }
            }
        } else {
            values = extractRangeAsShortArray(pixels, 0, pixels.length);
        }
        return new DataBufferShort(values, values.length);
    }
    /**
     * Returns a DataBuffer of ints from the intersection of the given int array and the region of interest.
     * @param pixels        The source values (the length = imageWidth * imageHeight)
     * @param imageWidth    The initial image width
     * @param imageHeight   The initial image height
     * @param roi           The region of interest
     */
    static DataBufferInt extractROI(int[] pixels, int imageWidth, int imageHeight, Rectangle roi) {
        int[] values = new int[roi.width * roi.height];
        int srcPos;
        int dstPos;
        int maxVal = Math.min(roi.y + roi.height, imageHeight);
        for (int col = roi.y; col < maxVal; col++) {
            try {
                srcPos = roi.x + col * imageWidth;
                dstPos = (col - roi.y) * roi.width;
                if (srcPos < pixels.length && dstPos < values.length) {
                    System.arraycopy(pixels, srcPos, values, dstPos, Math.min(roi.width, pixels.length - srcPos));
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                SystemUtils.LOG.warning("ROI fell outside the extracted range [" + e.getMessage() + "]");
            }
        }
        return new DataBufferInt(values, values.length);
    }

    /**
     * Writes the input values array to a file and calls back the given functor when completed.
     * @param width                 The width of the image represented by the int[] values
     * @param height                The height of the image represented by the int[] values
     * @param values                The source values
     * @param dataType              The target data type code (@see DataBuffer types)
     * @param toFile                The target file
     * @param completionCallBack    (optional) The function to be called back when the writing completes
     */
    static Path write(int width, int height, int[] values, int dataType, Path toFile, Function<Path, Void> completionCallBack) {
        int pixSize;
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                pixSize = 1;
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                pixSize = 2;
                break;
            case DataBuffer.TYPE_INT:
            default:
                pixSize = 4;
                break;
        }
        try (RandomAccessFile raf = new RandomAccessFile(toFile.toFile(), "rw")) {
            try (FileChannel file = raf.getChannel()) {
                ByteBuffer buffer = file.map(FileChannel.MapMode.READ_WRITE, 0, pixSize * values.length + 8);
                buffer.putInt(width);
                buffer.putInt(height);
                if (pixSize == 4) {
                    for (int v : values) {
                        buffer.putInt(v);
                    }
                } else if (pixSize == 2) {
                    for (int v : values) {
                        buffer.putShort((short) v);
                    }
                } else {
                    for (int v : values) {
                        buffer.put((byte) v);
                    }
                }
                file.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (completionCallBack != null) {
                completionCallBack.apply(toFile);
            }
        }
        return toFile;
    }

    /**
     * Reads the memory pointed to by the given pointer into the corresponding Java structure.
     * @param tClass    The class of the Java structure
     * @param pointer   The pointer to the native memory
     * @param <T>       The generic type of the Java structure
     */
    static <T extends Structure> T dereference(Class<T> tClass, Pointer pointer) {
        T ref = null;
        if (tClass != null && pointer != null) {
            try {
                Constructor<T> ctor = tClass.getDeclaredConstructor(Pointer.class);
                ref = ctor.newInstance(pointer);
                ref.read();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                SystemUtils.LOG.severe(e.getMessage());
            }
        }
        return ref;
    }

    static int[] readRasterBandasIntArray(Raster raster, int bandIndex) {
        if (raster == null ||
                bandIndex < 0 || bandIndex >= raster.getNumBands()) {
            throw new IllegalArgumentException("Raster not of int type or invalid band index");
        }
        int width = raster.getWidth();
        int height = raster.getHeight();
        //IntBuffer buffer = IntBuffer.allocate(width * height);
        SampleModel sampleModel = raster.getSampleModel();
        DataBuffer dataBuffer = raster.getDataBuffer();
        /*for (int y = 0; y < height; y++) {
            buffer.put(sampleModel.getSamples(0, y, width, 1, bandIndex, (int[])null, dataBuffer));
        }
        return buffer.array();*/
        return sampleModel.getSamples(0, 0, width, height, bandIndex, (int[])null, dataBuffer);
    }

    static int getFileFormat(Path file) {
        if (file == null) {
            return -1;
        }
        String fileName = file.getFileName().toString();
        String ext = fileName.substring(fileName.lastIndexOf("."));
        if (ext.isEmpty()) {
            return -1;
        }
        ext = ext.toLowerCase().replace(".", "");
        Integer code = extensions.get(ext);
        return code != null ? code : -1;
    }

    static int getFormat(String extension) {
        extension = extension.toLowerCase().replace(".", "");
        Integer code = extensions.get(extension);
        return code != null ? code : -1;
    }
}