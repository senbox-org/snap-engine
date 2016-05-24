package org.esa.s3tbx.dataio.s3.util;

import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.SingleBandedOpImage;
import org.esa.snap.dataio.netcdf.util.Constants;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.VariableIF;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class S3ReferencingVariableOpImage extends SingleBandedOpImage {

    private final Variable referencedIndexVariable;
    private final VariableIF variable;
    private final DimensionValuesProvider dimensionValuesProvider;

    //todo use this to display fires in SLSTR L2 LST products when data is available
    public S3ReferencingVariableOpImage(VariableIF variable, int dataBufferType, int sourceWidth, int sourceHeight,
                                        Dimension tileSize, ResolutionLevel level, int[] additionalDimensionIndexes,
                                        Variable referencedIndexVariable, String nameOfReferencingDimension,
                                        String nameOfDisplayedDimension) {
        super(dataBufferType, sourceWidth, sourceHeight, tileSize, null, level);
        this.variable = variable;
        dimensionValuesProvider = getDimensionValuesProvider();
        int displayedDimensionIndex = variable.findDimensionIndex(nameOfDisplayedDimension);
        int referencingDimensionIndex = variable.findDimensionIndex(nameOfReferencingDimension);
        this.referencedIndexVariable = referencedIndexVariable;
        final int numDetectors = variable.getDimension(referencingDimensionIndex).getLength();
        if (displayedDimensionIndex >= 0) {
            int[] variableOrigin = new int[2];
            variableOrigin[displayedDimensionIndex] = additionalDimensionIndexes[0];
            variableOrigin[referencingDimensionIndex] = 0;
            int[] variableShape = new int[2];
            variableShape[displayedDimensionIndex] = 1;
            variableShape[referencingDimensionIndex] = numDetectors;
            dimensionValuesProvider.readValues(variableOrigin, variableShape);
        } else {
            int[] variableOrigin = new int[1];
            variableOrigin[referencingDimensionIndex] = 0;
            int[] variableShape = new int[1];
            variableShape[referencingDimensionIndex] = numDetectors;
            dimensionValuesProvider.readValues(variableOrigin, variableShape);
        }

    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle rectangle) {
        final int[] shape = new int[2];
        final int[] origin = new int[2];
        final int[] stride = new int[2];
        for (int i = 0; i < 2; i++) {
            shape[i] = 1;
            origin[i] = 0;
            stride[i] = 1;
        }
        final int indexX = getIndexX(2);
        final int indexY = getIndexY(2);

        shape[indexX] = getSourceWidth(rectangle.width);
        shape[indexY] = getSourceHeight(rectangle.height);

        origin[indexX] = getSourceX(rectangle.x) + getSourceOriginX();
        origin[indexY] = getSourceY(rectangle.y) + getSourceOriginY();

        final double scale = getScale();
        stride[indexX] = (int) scale;
        stride[indexY] = (int) scale;

        Array referencedValues;
        final Array variableValues = Array.factory(variable.getDataType(), shape);
        try {
            final Section section = new Section(origin, shape, stride);
            synchronized (referencedIndexVariable.getParentGroup().getNetcdfFile()) {
                referencedValues = referencedIndexVariable.read(section);
                dimensionValuesProvider.setVariableValues(referencedValues, variableValues);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        tile.setDataElements(rectangle.x, rectangle.y, rectangle.width, rectangle.height, transformStorage(variableValues));
    }

    protected int getIndexX(int rank) {
        return rank - 1;
    }

    protected int getIndexY(int rank) {
        return rank - 2;
    }

    private DimensionValuesProvider getDimensionValuesProvider() {
        switch (variable.getDataType()) {
            case FLOAT:
                return new FloatDimensionValuesProvider();
            case SHORT:
                return new ShortDimensionValuesProvider();
        }
        return new NullDimensionValuesProvider();
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

    //copied from CfBandPart
    private static Number getNoDataValue(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.FILL_VALUE_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Constants.MISSING_VALUE_ATT_NAME);
        }
        if (attribute != null) {
            return getAttributeValue(attribute);
        }
        return null;
    }

    //copied from CfBandPart
    private static Number getAttributeValue(Attribute attribute) {
        if (attribute.isString()) {
            String stringValue = attribute.getStringValue();
            if (stringValue.endsWith("b")) {
                return Byte.parseByte(stringValue.substring(0, stringValue.length() - 1));
            } else {
                return Double.parseDouble(stringValue);
            }
        } else {
            return attribute.getNumericValue();
        }
    }

    interface DimensionValuesProvider {

        void readValues(int[] variableOrigin, int[] variableShape);

        void setVariableValues(Array referencedValues, Array variableValues);
    }

    private class FloatDimensionValuesProvider implements DimensionValuesProvider {

        private float[] dimensionValues;

        @Override
        public void readValues(int[] variableOrigin, int[] variableShape) {
            try {
                final Section detectorSection = new Section(variableOrigin, variableShape);
                synchronized (variable.getParentGroup().getNetcdfFile()) {
                    final Array dimensionValuesArray = variable.read(detectorSection);
                    dimensionValues = (float[]) dimensionValuesArray.copyTo1DJavaArray();
                }
            } catch (InvalidRangeException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setVariableValues(Array referencedValues, Array variableValues) {
            final float noDataValue = getNoDataValue(referencedIndexVariable).floatValue();
            for (int i = 0; i < referencedValues.getSize(); i++) {
                final int detectorIndex = referencedValues.getInt(i);
                if (detectorIndex > -1) {
                    variableValues.setFloat(i, dimensionValues[detectorIndex]);
                } else {
                    variableValues.setFloat(i, noDataValue);
                }
            }
        }
    }

    private class ShortDimensionValuesProvider implements DimensionValuesProvider {

        private short[] dimensionValues;

        @Override
        public void readValues(int[] variableOrigin, int[] variableShape) {
            try {
                final Section detectorSection = new Section(variableOrigin, variableShape);
                synchronized (variable.getParentGroup().getNetcdfFile()) {
                    final Array dimensionValuesArray = variable.read(detectorSection);
                    dimensionValues = (short[]) dimensionValuesArray.copyTo1DJavaArray();
                }
            } catch (InvalidRangeException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setVariableValues(Array referencedValues, Array variableValues) {
            final short noDataValue = getNoDataValue(referencedIndexVariable).shortValue();
            for (int i = 0; i < referencedValues.getSize(); i++) {
                final int detectorIndex = referencedValues.getInt(i);
                if (detectorIndex > -1) {
                    variableValues.setShort(i, dimensionValues[detectorIndex]);
                } else {
                    variableValues.setShort(i, noDataValue);
                }
            }
        }
    }

    private class NullDimensionValuesProvider implements DimensionValuesProvider {

        @Override
        public void readValues(int[] variableOrigin, int[] variableShape) {
        }

        @Override
        public void setVariableValues(Array referencedValues, Array variableValues) {
        }
    }

}
