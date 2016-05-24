package org.esa.s3tbx.dataio.s3.util;

import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.SingleBandedOpImage;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Section;
import ucar.nc2.VariableIF;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;

/**
 * @author Tonio Fincke
 */
public class S3VariableOpImage extends SingleBandedOpImage {

    private final VariableIF variable;
    private ArrayConverter converter;
    private int xIndex;
    private int yIndex;
    private int[] additionalDimensionIndexes;
    private String[] additionalDimensionNames;

    public S3VariableOpImage(VariableIF variable, int dataBufferType, int sourceWidth, int sourceHeight,
                             Dimension tileSize, ResolutionLevel level,
                             String[] additionalDimensionNames, int[] additionalDimensionIndexes,
                             int xIndex, int yIndex
    ) {
        this(variable, dataBufferType, sourceWidth, sourceHeight, tileSize, level,
             additionalDimensionNames, additionalDimensionIndexes, xIndex, yIndex,
             ArrayConverter.IDENTITY);
    }

    static S3VariableOpImage createS3VariableOpImage(VariableIF variable, int dataBufferType, int sourceWidth, int sourceHeight,
                                                     Dimension tileSize, ResolutionLevel level,
                                                     String[] additionalDimensionNames, int[] additionalDimensionIndexes,
                                                     int xIndex, int yIndex,
                                                     boolean msb) {
        if (msb) {
            return new S3VariableOpImage(variable, dataBufferType, sourceWidth, sourceHeight, tileSize, level,
                                         additionalDimensionNames, additionalDimensionIndexes, xIndex, yIndex,
                                         ArrayConverter.MSB);
        } else {
            return new S3VariableOpImage(variable, dataBufferType, sourceWidth, sourceHeight, tileSize, level,
                                         additionalDimensionNames, additionalDimensionIndexes, xIndex, yIndex,
                                         ArrayConverter.LSB);
        }
    }

    public S3VariableOpImage(VariableIF variable, int dataBufferType, int sourceWidth, int sourceHeight,
                             Dimension tileSize, ResolutionLevel level,
                             String[] additionalDimensionNames, int[] additionalDimensionIndexes,
                             int xIndex, int yIndex, ArrayConverter converter) {
        super(dataBufferType, sourceWidth, sourceHeight, tileSize, null, level);
        this.variable = variable;
        this.additionalDimensionNames = additionalDimensionNames;
        this.additionalDimensionIndexes = additionalDimensionIndexes;
        this.xIndex = xIndex;
        this.yIndex = yIndex;
        this.converter = converter;
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle rectangle) {
//        System.out.println(variable.getFullName() + ": " + tile.getMinX() + ", " + tile.getMinY() + ", " +
//                                   tile.getWidth() + ", " + tile.getHeight());
        final int rank = variable.getRank();
        final int[] origin = new int[rank];
        final int[] shape = new int[rank];
        final int[] stride = new int[rank];
        for (int i = 0; i < rank; i++) {
            shape[i] = 1;
            origin[i] = 0;
            stride[i] = 1;
        }
        final int indexX = xIndex;
        final int indexY = yIndex;

        shape[indexX] = getSourceWidth(rectangle.width);
        shape[indexY] = getSourceHeight(rectangle.height);

        origin[indexX] = getSourceX(rectangle.x) + getSourceOriginX();
        origin[indexY] = getSourceY(rectangle.y) + getSourceOriginY();

        final double scale = getScale();
        stride[indexX] = (int) scale;
        stride[indexY] = (int) scale;

        for (int i = 0; i < additionalDimensionIndexes.length; i++) {
            final int dimensionIndex1 = variable.findDimensionIndex(additionalDimensionNames[i]);
            origin[dimensionIndex1] = additionalDimensionIndexes[i];
            stride[dimensionIndex1] = (int) scale;
        }

        Array array;
        try {
            synchronized (variable.getParentGroup().getNetcdfFile()) {
                final Section section = new Section(origin, shape, stride);
                array = variable.read(section);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        array = converter.convert(array);
        tile.setDataElements(rectangle.x, rectangle.y, rectangle.width, rectangle.height, transformStorage(array));
    }

    /**
     * Returns the origin of the x dimension of the variable, which
     * provides the image data.
     *
     * @return the origin of the x dimension.
     */
    protected int getSourceOriginX() {
        return 0;
    }

    /**
     * Returns the origin of the y dimension of the variable, which
     * provides the image data.
     *
     * @return the origin of the y dimension.
     */
    protected int getSourceOriginY() {
        return 0;
    }

    /**
     * Transforms the primitive storage of the array supplied as argument.
     * <p>
     * The default implementation merely returns the primitive storage of
     * the array supplied as argument, which is fine when the sequence of
     * variable dimensions is (..., y, x).
     * <p>
     * Implementations have to transpose the storage when the sequence of
     * variable dimensions is (..., x, y) instead of (..., y, x).
     * <p>
     *
     * @param array An array.
     * @return the transformed primitive storage of the array supplied as
     * argument.
     */
    protected Object transformStorage(Array array) {
        return array.getStorage();
    }

    interface ArrayConverter {

        public ArrayConverter IDENTITY = new ArrayConverter() {
            @Override
            public Array convert(Array array) {
                return array;
            }
        };

        public ArrayConverter LSB = new ArrayConverter() {
            @Override
            public Array convert(Array array) {
                final Array convertedArray = Array.factory(DataType.INT, array.getShape());
                for (int i = 0; i < convertedArray.getSize(); i++) {
                    convertedArray.setInt(i, (int) (array.getLong(i) & 0x00000000FFFFFFFFL));
                }
                return convertedArray;
            }
        };

        public ArrayConverter MSB = new ArrayConverter() {
            @Override
            public Array convert(Array array) {
                final Array convertedArray = Array.factory(DataType.INT, array.getShape());
                for (int i = 0; i < convertedArray.getSize(); i++) {
                    convertedArray.setInt(i, (int) (array.getLong(i) >>> 32));
                }
                return convertedArray;
            }
        };

        public ArrayConverter UINTCONVERTER = new ArrayConverter() {
            @Override
            public Array convert(Array array) {
                final Array convertedArray = Array.factory(DataType.FLOAT, array.getShape());
                for (int i = 0; i < convertedArray.getSize(); i++) {
                    convertedArray.setFloat(i, array.getFloat(i));
                }
                return convertedArray;
            }
        };

        Array convert(Array array);
    }

}
