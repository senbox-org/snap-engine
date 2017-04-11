package org.esa.s3tbx.dataio.s3.util;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.SampleCoding;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class S3NetcdfReader extends AbstractProductReader {

    //todo check whether this class is still necessary as most of its functionality is also provided by the default netcdf reader
    private static final String product_type = "product_type";
    private static final String flag_values = "flag_values";
    private static final String flag_masks = "flag_masks";
    private static final String flag_meanings = "flag_meanings";
    private static final String fillValue = "_FillValue";
    private NetcdfFile netcdfFile;

    public S3NetcdfReader() {
        super(null);
    }

    public String[] getSuffixesForSeparatingDimensions() {
        return new String[0];
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        File inputFile = getInputFile();
        netcdfFile = NetcdfFileOpener.open(inputFile);

        final String productType = readProductType();
        int productWidth = getWidth();
        int productHeight = getHeight();

        final Product product = new Product(FileUtils.getFilenameWithoutExtension(inputFile), productType, productWidth, productHeight);
        product.setFileLocation(inputFile);
        addGlobalMetadata(product);
        addBands(product);
        addGeoCoding(product);
        for (final Band band : product.getBands()) {
            if (band instanceof VirtualBand) {
                continue;
            }
            band.setSourceImage(createSourceImage(band));
        }
        return product;
    }

    @Override
    public void close() throws IOException {
        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
        super.close();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("All bands use images as source for its data, readBandRasterDataImpl should not be called.");
    }

    protected void addGeoCoding(Product product) {

    }

    private File getInputFile() {
        final Object input = getInput();

        if (input instanceof String) {
            return new File((String) input);
        }
        if (input instanceof File) {
            return (File) input;
        }

        throw new IllegalArgumentException(MessageFormat.format("Illegal input: {0}", input));
    }

    protected String[] getSeparatingDimensions() {
        return new String[0];
    }

    protected String[][] getRowColumnNamePairs() {
        return new String[][]{{"rows", "columns"}, {"tie_rows", "tie_columns"}};
    }

    protected RenderedImage createSourceImage(Band band) {
        final String bandName = band.getName();
        String variableName = bandName;
        if (variableName.endsWith("_lsb")) {
            variableName = variableName.substring(0, variableName.indexOf("_lsb"));
        } else if (variableName.endsWith("_msb")) {
            variableName = variableName.substring(0, variableName.indexOf("_msb"));
        }
        Variable variable = null;
        List<String> dimensionNameList = new ArrayList<>();
        List<Integer> dimensionIndexList = new ArrayList<>();
        final String[] separatingDimensions = getSeparatingDimensions();
        final String[] suffixesForSeparatingThirdDimensions = getSuffixesForSeparatingDimensions();
        int lowestSuffixIndex = Integer.MAX_VALUE;
        for (int i = 0; i < separatingDimensions.length; i++) {
            final String dimension = separatingDimensions[i];
            final String suffix = suffixesForSeparatingThirdDimensions[i];
            if (bandName.contains(suffix)) {
                final int suffixIndex = bandName.indexOf(suffix) - 1;
                if (suffixIndex < lowestSuffixIndex) {
                    lowestSuffixIndex = suffixIndex;
                }
                dimensionNameList.add(dimension);
                dimensionIndexList.add(getDimensionIndexFromBandName(bandName));
            }
        }
        if (lowestSuffixIndex < bandName.length()) {
            variableName = bandName.substring(0, lowestSuffixIndex);
            variable = netcdfFile.findVariable(variableName);
        }
        if (variable == null) {
            variable = netcdfFile.findVariable(variableName);
        }
        Dimension widthDimension = getWidthDimension();
        int xIndex = -1;
        int yIndex = -1;
        if (widthDimension != null) {
            xIndex = variable.findDimensionIndex(widthDimension.getFullName());
        }
        Dimension heightDimension = getHeightDimension();
        if (heightDimension != null) {
            yIndex = variable.findDimensionIndex(heightDimension.getFullName());
        }
        String[] dimensionNames = dimensionNameList.toArray(new String[dimensionNameList.size()]);
        int[] dimensionIndexes = new int[dimensionIndexList.size()];
        for (int i = 0; i < dimensionIndexList.size(); i++) {
            dimensionIndexes[i] = dimensionIndexList.get(i);
        }
        return new S3MultiLevelOpImage(band, variable, dimensionNames, dimensionIndexes, xIndex, yIndex);
    }

    protected int getDimensionIndexFromBandName(String bandName) {
        return Integer.parseInt(bandName.substring(bandName.lastIndexOf("_") + 1)) - 1;
    }

    private void addGlobalMetadata(Product product) {
        final MetadataElement globalAttributesElement = new MetadataElement("Global_Attributes");
        final List<Attribute> globalAttributes = netcdfFile.getGlobalAttributes();
        for (final Attribute attribute : globalAttributes) {
            if (attribute.getValues() != null) {
                final ProductData attributeData = getAttributeData(attribute);
                final MetadataAttribute metadataAttribute = new MetadataAttribute(attribute.getFullName(), attributeData, true);
                globalAttributesElement.addAttribute(metadataAttribute);
            }
        }
        product.getMetadataRoot().addElement(globalAttributesElement);
        final MetadataElement variableAttributesElement = new MetadataElement("Variable_Attributes");
        product.getMetadataRoot().addElement(variableAttributesElement);
    }

    protected void addBands(Product product) {
        final List<Variable> variables = netcdfFile.getVariables();
        for (final Variable variable : variables) {
            final String[][] rowColumnNamePairs = getRowColumnNamePairs();
            for (String[] rowColumnNamePair : rowColumnNamePairs) {
                if (variable.findDimensionIndex(rowColumnNamePair[0]) != -1 &&
                    variable.findDimensionIndex(rowColumnNamePair[1]) != -1) {
                    final String variableName = variable.getFullName();
                    final String[] dimensions = getSeparatingDimensions();
                    final String[] suffixes = getSuffixesForSeparatingDimensions();
                    boolean variableMustStillBeAdded = true;
                    for (int i = 0; i < dimensions.length; i++) {
                        String dimensionName = dimensions[i];
                        if (variable.findDimensionIndex(dimensionName) != -1) {
                            final Dimension dimension =
                                    variable.getDimension(variable.findDimensionIndex(dimensionName));
                            for (int j = 0; j < dimension.getLength(); j++) {
                                addVariableAsBand(product, variable, variableName + "_" + suffixes[i] + "_" + (j + 1), false);
                            }
                            variableMustStillBeAdded = false;
                            break;
                        }
                    }
                    if (variableMustStillBeAdded) {
                        addVariableAsBand(product, variable, variableName, false);
                    }
                }
            }
            addVariableMetadata(variable, product);
        }
    }

    private static int getRasterDataType(Variable variable) {
        int rasterDataType = DataTypeUtils.getRasterDataType(variable);
        if (rasterDataType == -1 && variable.getDataType() == DataType.LONG) {
            rasterDataType = variable.isUnsigned() ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
        }
        return rasterDataType;
    }

    protected void addVariableAsBand(Product product, Variable variable, String variableName, boolean synthetic) {
        int type = getRasterDataType(variable);
        //todo if the datatype is unsigned long it might even be necessary to split it into three bands. This case is yet theoretical, though
        if (type == ProductData.TYPE_INT64) {
            final Band lowerBand = product.addBand(variableName + "_lsb", ProductData.TYPE_UINT32);
            lowerBand.setDescription(variable.getDescription() + "(least significant bytes)");
            lowerBand.setUnit(variable.getUnitsString());
            lowerBand.setScalingFactor(getScalingFactor(variable));
            lowerBand.setScalingOffset(getAddOffset(variable));
            lowerBand.setSynthetic(synthetic);
            addFillValue(lowerBand, variable);
            addSampleCodings(product, lowerBand, variable, false);
            final Band upperBand = product.addBand(variableName + "_msb", ProductData.TYPE_UINT32);
            upperBand.setDescription(variable.getDescription() + "(most significant bytes)");
            upperBand.setUnit(variable.getUnitsString());
            upperBand.setScalingFactor(getScalingFactor(variable));
            upperBand.setScalingOffset(getAddOffset(variable));
            upperBand.setSynthetic(synthetic);
            addFillValue(upperBand, variable);
            addSampleCodings(product, upperBand, variable, true);
        } else {
            final Band band = product.addBand(variableName, type);
            band.setDescription(variable.getDescription());
            band.setUnit(variable.getUnitsString());
            band.setScalingFactor(getScalingFactor(variable));
            band.setScalingOffset(getAddOffset(variable));
            band.setSynthetic(synthetic);
            addSampleCodings(product, band, variable, false);
            addFillValue(band, variable);
        }
    }

    protected void addFillValue(Band band, Variable variable) {
        final Attribute fillValueAttribute = variable.findAttribute(fillValue);
        if (fillValueAttribute != null) {
            //todo double is not always correct
            band.setNoDataValue(fillValueAttribute.getNumericValue().doubleValue());
            // enable only if it is not a flag band
            band.setNoDataValueUsed(!band.isFlagBand());
        }
    }

    protected void addSampleCodings(Product product, Band band, Variable variable, boolean msb) {
        final Attribute flagValuesAttribute = variable.findAttribute(flag_values);
        final Attribute flagMasksAttribute = variable.findAttribute(flag_masks);
        final Attribute flagMeaningsAttribute = variable.findAttribute(flag_meanings);
        if (flagValuesAttribute != null && flagMasksAttribute != null) {
            final FlagCoding flagCoding =
                    getFlagCoding(product, band.getName(), flagMeaningsAttribute, flagValuesAttribute,
                                  flagMasksAttribute, msb);
            band.setSampleCoding(flagCoding);
        } else if (flagValuesAttribute != null) {
            final IndexCoding indexCoding =
                    getIndexCoding(product, band.getName(), flagMeaningsAttribute, flagValuesAttribute, msb);
            band.setSampleCoding(indexCoding);
        } else if (flagMasksAttribute != null) {
            final FlagCoding flagCoding = getFlagCoding(product, band.getName(), flagMeaningsAttribute, flagMasksAttribute, msb);
            band.setSampleCoding(flagCoding);
        }
    }

    private IndexCoding getIndexCoding(Product product, String indexCodingName, Attribute flagMeaningsAttribute,
                                       Attribute flagValuesAttribute, boolean msb) {
        final IndexCoding indexCoding = new IndexCoding(indexCodingName);
        addSamples(indexCoding, flagMeaningsAttribute, flagValuesAttribute, msb);
        if (!product.getIndexCodingGroup().contains(indexCodingName)) {
            product.getIndexCodingGroup().add(indexCoding);
        }
        return indexCoding;
    }

    private FlagCoding getFlagCoding(Product product, String flagCodingName, Attribute flagMeaningsAttribute,
                                     Attribute flagMasksAttribute, boolean msb) {
        final FlagCoding flagCoding = new FlagCoding(flagCodingName);
        addSamples(flagCoding, flagMeaningsAttribute, flagMasksAttribute, msb);
        if (!product.getFlagCodingGroup().contains(flagCodingName)) {
            product.getFlagCodingGroup().add(flagCoding);
        }
        return flagCoding;
    }

    private FlagCoding getFlagCoding(Product product, String flagCodingName, Attribute flagMeaningsAttribute,
                                     Attribute flagValuesAttribute, Attribute flagMasksAttribute, boolean msb) {
        final FlagCoding flagCoding = new FlagCoding(flagCodingName);
        addSamples(flagCoding, flagMeaningsAttribute, flagValuesAttribute, flagMasksAttribute, msb);
        if (!product.getFlagCodingGroup().contains(flagCodingName)) {
            product.getFlagCodingGroup().add(flagCoding);
        }
        return flagCoding;
    }

    private static void addSamples(SampleCoding sampleCoding, Attribute sampleMeanings, Attribute sampleValues,
                                   boolean msb) {
        final String[] meanings = getSampleMeanings(sampleMeanings);
        final int sampleCount = Math.min(meanings.length, sampleValues.getLength());
        for (int i = 0; i < sampleCount; i++) {
            final String sampleName = replaceNonWordCharacters(meanings[i]);
            switch (sampleValues.getDataType()) {
                case BYTE:
                    sampleCoding.addSample(sampleName,
                                           DataType.unsignedByteToShort(sampleValues.getNumericValue(i).byteValue()),
                                           null);
                    break;
                case SHORT:
                    sampleCoding.addSample(sampleName,
                                           DataType.unsignedShortToInt(sampleValues.getNumericValue(i).shortValue()), null);
                    break;
                case INT:
                    sampleCoding.addSample(sampleName, sampleValues.getNumericValue(i).intValue(), null);
                    break;
                case LONG:
                    final long longValue = sampleValues.getNumericValue(i).longValue();
                    if (msb) {
                        long shiftedValue = longValue >>> 32;
                        if (shiftedValue > 0) {
                            sampleCoding.addSample(sampleName, (int) shiftedValue, null);
                        }
                    } else {
                        long shiftedValue = longValue & 0x00000000FFFFFFFFL;
                        if (shiftedValue > 0 || longValue == 0L) {
                            sampleCoding.addSample(sampleName, (int) shiftedValue, null);
                        }
                    }
                    break;
            }
        }
    }

    private static void addSamples(SampleCoding sampleCoding, Attribute sampleMeanings, Attribute sampleValues,
                                   Attribute sampleMasks, boolean msb) {
        final String[] meanings = getSampleMeanings(sampleMeanings);
        final int sampleCount = Math.min(meanings.length, sampleMasks.getLength());
        for (int i = 0; i < sampleCount; i++) {
            final String sampleName = replaceNonWordCharacters(meanings[i]);
            switch (sampleMasks.getDataType()) {
                case BYTE:
                    int[] byteValues = {
                            DataType.unsignedByteToShort(sampleMasks.getNumericValue(i).byteValue()),
                            DataType.unsignedByteToShort(sampleValues.getNumericValue(i).byteValue())
                    };
                    if (byteValues[0] == byteValues[1]) {
                        sampleCoding.addSample(sampleName, byteValues[0], null);
                    } else {
                        sampleCoding.addSamples(sampleName, byteValues, null);
                    }
                    break;
                case SHORT:
                    int[] shortValues = {
                            DataType.unsignedShortToInt(sampleMasks.getNumericValue(i).shortValue()),
                            DataType.unsignedShortToInt(sampleValues.getNumericValue(i).shortValue())
                    };
                    if (shortValues[0] == shortValues[1]) {
                        sampleCoding.addSample(sampleName, shortValues[0], null);
                    } else {
                        sampleCoding.addSamples(sampleName, shortValues, null);
                    }
                    break;
                case INT:
                    int[] intValues = {
                            sampleMasks.getNumericValue(i).intValue(),
                            sampleValues.getNumericValue(i).intValue()
                    };
                    if (intValues[0] == intValues[1]) {
                        sampleCoding.addSample(sampleName, intValues[0], null);
                    } else {
                        sampleCoding.addSamples(sampleName, intValues, null);
                    }
                    sampleCoding.addSamples(sampleName, intValues, null);
                    break;
                case LONG:
                    long[] longValues = {
                            sampleMasks.getNumericValue(i).longValue(),
                            sampleValues.getNumericValue(i).longValue()
                    };
                    if (msb) {
                        int[] intLongValues =
                                {(int) (longValues[0] >>> 32), (int) (longValues[1] >>> 32)};
                        if (longValues[0] > 0) {
                            if (intLongValues[0] == intLongValues[1]) {
                                sampleCoding.addSample(sampleName, intLongValues[0], null);
                            } else {
                                sampleCoding.addSamples(sampleName, intLongValues, null);
                            }
                        }
                    } else {
                        int[] intLongValues =
                                {(int) (longValues[0] & 0x00000000FFFFFFFFL), (int) (longValues[1] & 0x00000000FFFFFFFFL)};
                        if (intLongValues[0] > 0 || longValues[0] == 0L) {
                            if (intLongValues[0] == intLongValues[1]) {
                                sampleCoding.addSample(sampleName, intLongValues[0], null);
                            } else {
                                sampleCoding.addSamples(sampleName, intLongValues, null);
                            }
                        }
                    }
                    break;
            }
        }
    }

    private static String[] getSampleMeanings(Attribute sampleMeanings) {
        final int sampleMeaningsCount = sampleMeanings.getLength();
        if (sampleMeaningsCount == 0) {
            return new String[sampleMeaningsCount];
        }
        if (sampleMeaningsCount > 1) {
            // handle a common misunderstanding of CF conventions, where flag meanings are stored as array of strings
            final String[] strings = new String[sampleMeaningsCount];
            for (int i = 0; i < strings.length; i++) {
                strings[i] = sampleMeanings.getStringValue(i);
            }
            return strings;
        }
        return sampleMeanings.getStringValue().split(" ");
    }

    static String replaceNonWordCharacters(String flagName) {
        return flagName.replaceAll("\\W+", "_");
    }

    protected static double getScalingFactor(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.SCALE_FACTOR_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Constants.SLOPE_ATT_NAME);
        }
        if (attribute == null) {
            attribute = variable.findAttribute("scaling_factor");
        }
        if (attribute != null) {
            return getAttributeValue(attribute).doubleValue();
        }
        return 1.0;
    }

    protected static double getAddOffset(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.ADD_OFFSET_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Constants.INTERCEPT_ATT_NAME);
        }
        if (attribute != null) {
            return getAttributeValue(attribute).doubleValue();
        }
        return 0.0;
    }

    private static Number getAttributeValue(Attribute attribute) {
        if (attribute.isString()) {
            String stringValue = attribute.getStringValue();
            if (stringValue.endsWith("b")) {
                // Special management for bytes; Can occur in e.g. ASCAT files from EUMETSAT
                return Byte.parseByte(stringValue.substring(0, stringValue.length() - 1));
            } else {
                return Double.parseDouble(stringValue);
            }
        } else {
            return attribute.getNumericValue();
        }
    }

    protected void addVariableMetadata(Variable variable, Product product) {
        final MetadataElement variableElement = new MetadataElement(variable.getFullName());
        final List<Attribute> attributes = variable.getAttributes();
        for (Attribute attribute : attributes) {
            if (attribute.getFullName().equals("flag_meanings")) {
                final String[] flagMeanings = attribute.getStringValue().split(" ");
                for (int i = 0; i < flagMeanings.length; i++) {
                    String flagMeaning = flagMeanings[i];
                    final ProductData attributeData = ProductData.createInstance(flagMeaning);
                    final MetadataAttribute metadataAttribute =
                            new MetadataAttribute(attribute.getFullName() + "." + i, attributeData, true);
                    variableElement.addAttribute(metadataAttribute);
                }
            } else {
                if (attribute.getValues() != null) {
                    final ProductData attributeData = getAttributeData(attribute);
                    final MetadataAttribute metadataAttribute = new MetadataAttribute(attribute.getFullName(), attributeData, true);
                    variableElement.addAttribute(metadataAttribute);
                }
            }
        }
        List<Dimension> variableDimensions = variable.getDimensions();
        if (variableDimensions.size() == 1) {
            try {
                Object data = variable.read().copyTo1DJavaArray();
                MetadataAttribute variableAttribute = null;
                if (data instanceof float[]) {
                    variableAttribute = new MetadataAttribute("value", ProductData.createInstance((float[]) data), true);
                } else if (data instanceof double[]) {
                    variableAttribute = new MetadataAttribute("value", ProductData.createInstance((double[]) data), true);
                } else if (data instanceof byte[]) {
                    variableAttribute = new MetadataAttribute("value", ProductData.createInstance((byte[]) data), true);
                } else if (data instanceof short[]) {
                    variableAttribute = new MetadataAttribute("value", ProductData.createInstance((short[]) data), true);
                } else if (data instanceof int[]) {
                    variableAttribute = new MetadataAttribute("value", ProductData.createInstance((int[]) data), true);
                } else if (data instanceof long[]) {
                    variableAttribute = new MetadataAttribute("value", ProductData.createInstance((long[]) data), true);
                }
                if (variableAttribute != null) {
                    variableAttribute.setUnit(variable.getUnitsString());
                    variableAttribute.setDescription(variable.getDescription());
                    variableElement.addAttribute(variableAttribute);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        product.getMetadataRoot().getElement("Variable_Attributes").addElement(variableElement);
    }

    private int getProductDataType(Attribute attribute) {
        return DataTypeUtils.getEquivalentProductDataType(attribute.getDataType(), false, false);
    }

    protected ProductData getAttributeData(Attribute attribute) {
        int type = getProductDataType(attribute);
        final Array attributeValues = attribute.getValues();
        ProductData productData = null;
        switch (type) {
            case ProductData.TYPE_ASCII: {
                productData = ProductData.createInstance(attributeValues.toString());
                break;
            }
            case ProductData.TYPE_INT8: {
                productData = ProductData.createInstance((byte[]) attributeValues.copyTo1DJavaArray());
                break;
            }
            case ProductData.TYPE_INT16: {
                productData = ProductData.createInstance((short[]) attributeValues.copyTo1DJavaArray());
                break;
            }
            case ProductData.TYPE_INT32: {
                productData = ProductData.createInstance((int[]) attributeValues.copyTo1DJavaArray());
                break;
            }
            case ProductData.TYPE_UINT32: {
                Object array = convertLongToIntArray(attributeValues.copyTo1DJavaArray());
                productData = ProductData.createUnsignedInstance((int[]) array);
                break;
            }
            case ProductData.TYPE_INT64: {
                productData = ProductData.createInstance((long[]) attributeValues.copyTo1DJavaArray());
                break;
            }
            case ProductData.TYPE_FLOAT32: {
                productData = ProductData.createInstance((float[]) attributeValues.copyTo1DJavaArray());
                break;
            }
            case ProductData.TYPE_FLOAT64: {
                productData = ProductData.createInstance((double[]) attributeValues.copyTo1DJavaArray());
                break;
            }
            default: {
                break;
            }
        }
        return productData;
    }

    private Object convertLongToIntArray(Object array) {
        if (array instanceof long[]) {
            long[] longArray = (long[]) array;
            int[] newArray = new int[longArray.length];
            for (int i = 0; i < longArray.length; i++) {
                newArray[i] = (int) longArray[i];
            }
            array = newArray;
        }
        return array;
    }

    private int getWidth() {
        Dimension widthDimension = getWidthDimension();
        if (widthDimension != null) {
            return widthDimension.getLength();
        }
        return 0;
    }

    private Dimension getWidthDimension() {
        final String[][] rowColumnNamePairs = getRowColumnNamePairs();
        for (String[] rowColumnNamePair : rowColumnNamePairs) {
            final Dimension widthDimension = netcdfFile.findDimension(rowColumnNamePair[1]);
            if (widthDimension != null) {
                return widthDimension;
            }
        }
        return null;
    }

    private int getHeight() {
        Dimension heightDimension = getHeightDimension();
        if (heightDimension != null) {
            return heightDimension.getLength();
        }
        return 0;
    }

    private Dimension getHeightDimension() {
        final String[][] rowColumnNamePairs = getRowColumnNamePairs();
        for (String[] rowColumnNamePair : rowColumnNamePairs) {
            final Dimension heightDimension = netcdfFile.findDimension(rowColumnNamePair[0]);
            if (heightDimension != null) {
                return heightDimension;
            }
        }
        return null;
    }

    private String readProductType() {
        Attribute typeAttribute = netcdfFile.findGlobalAttribute(product_type);
        if (typeAttribute == null) {
            typeAttribute = netcdfFile.findGlobalAttribute("Conventions");
        }
        String type = null;
        if (typeAttribute != null) {
            type = typeAttribute.getStringValue();
        }
        String productType = Constants.FORMAT_NAME;
        if (type != null && type.trim().length() > 0) {
            productType = type.trim();
        }
        return productType;
    }

    protected NetcdfFile getNetcdfFile() {
        return netcdfFile;
    }

}
