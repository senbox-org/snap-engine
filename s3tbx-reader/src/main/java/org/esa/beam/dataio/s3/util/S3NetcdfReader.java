package org.esa.beam.dataio.s3.util;

import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.SampleCoding;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.io.FileUtils;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class S3NetcdfReader {

    //todo make specific solution for dimension "channel" more generic?
    //todo work better with "channel" metadata - after it is no longer experimental

    private static final String product_type = "product_type";
    private static final String flag_values = "flag_values";
    private static final String flag_masks = "flag_masks";
    private static final String flag_meanings = "flag_meanings";
    private static final String fillValue = "_FillValue";
    private final NetcdfFile netcdfFile;
    private final String pathToFile;

    public S3NetcdfReader(String pathToFile) throws IOException {
        netcdfFile = NetcdfFileOpener.open(pathToFile);
        this.pathToFile = pathToFile;
    }

    public Product readProduct() throws IOException {
        final String productType = readProductType();
        int productWidth = getWidth();
        int productHeight = getHeight();
        final File file = new File(pathToFile);

        final Product product = new Product(FileUtils.getFilenameWithoutExtension(file), productType, productWidth, productHeight);
        product.setFileLocation(file);
        addGlobalMetadata(product);
        addBands(product);
        for (final Band band : product.getBands()) {
            if (band instanceof VirtualBand) {
                continue;
            }
            band.setSourceImage(createSourceImage(band));
        }
        return product;
    }

    protected RenderedImage createSourceImage(Band band) {
        final int bufferType = ImageManager.getDataBufferType(band.getDataType());
        final int sourceWidth = band.getSceneRasterWidth();
        final int sourceHeight = band.getSceneRasterHeight();
        final java.awt.Dimension tileSize = band.getProduct().getPreferredTileSize();
        final String bandName = band.getName();
        String variableName = bandName;
        Variable variable;
        int dimensionIndex = -1;
        String dimensionName = "";
        if(bandName.contains("_channel")) {
            variableName = bandName.substring(0, variableName.indexOf("_channel"));
            variable = netcdfFile.findVariable(variableName);
            dimensionName = "channel";
            dimensionIndex = Integer.parseInt(bandName.substring(bandName.length() - 1)) - 1;
        } else {
            variable = netcdfFile.findVariable(variableName);
        }
        return new S3VariableOpImage(variable, bufferType, sourceWidth, sourceHeight, tileSize,
                                        ResolutionLevel.MAXRES, dimensionName, dimensionIndex);
    }

    private void addGlobalMetadata(Product product) {
        final MetadataElement globalAttributesElement = new MetadataElement("Global_Attributes");
        final List<Attribute> globalAttributes = netcdfFile.getGlobalAttributes();
        for (final Attribute attribute : globalAttributes) {
            int type = DataTypeUtils.getEquivalentProductDataType(attribute.getDataType(), false, false);
            final ProductData attributeData = getAttributeData(attribute, type);
            final MetadataAttribute metadataAttribute =
                    new MetadataAttribute(attribute.getFullName(), attributeData, true);
            globalAttributesElement.addAttribute(metadataAttribute);
        }
        product.getMetadataRoot().addElement(globalAttributesElement);
        final MetadataElement variableAttributesElement = new MetadataElement("Variable_Attributes");
        product.getMetadataRoot().addElement(variableAttributesElement);
    }

    protected void addBands(Product product) {
        final List<Variable> variables = netcdfFile.getVariables();
        for (final Variable variable : variables) {
            if (variable.findDimensionIndex("rows") != -1 && variable.findDimensionIndex("columns") != -1) {
                final String variableName = variable.getFullName();
                if(variable.findDimensionIndex("channel") != - 1) {
                    final Dimension channelDimension = variable.getDimension(variable.findDimensionIndex("channel"));
                    for(int i = 0; i < channelDimension.getLength(); i++) {
                        createBand(product, variable, variableName + "_channel" + (i + 1));
                    }
                } else {
                    createBand(product, variable, variableName);
                }
            }
            addVariableMetadata(variable, product);
        }
    }

    protected void createBand(Product product, Variable variable, String variableName) {
        int type = DataTypeUtils.getEquivalentProductDataType(variable.getDataType(), false, false);
        final Band band = product.addBand(variableName, type);
        band.setDescription(variable.getDescription());
        final Attribute fillValueAttribute = variable.findAttribute(fillValue);
        if (fillValueAttribute != null) {
            band.setNoDataValue(fillValueAttribute.getNumericValue().doubleValue());
            band.setNoDataValueUsed(true);
        }
        final Attribute flagValuesAttribute = variable.findAttribute(flag_values);
        final Attribute flagMasksAttribute = variable.findAttribute(flag_masks);
        final Attribute flagMeaningsAttribute = variable.findAttribute(flag_meanings);
        if (flagValuesAttribute != null && flagMasksAttribute != null) {
            final FlagCoding flagCoding = getFlagCoding(product, variableName, flagMeaningsAttribute, flagMasksAttribute);
            band.setSampleCoding(flagCoding);
            final String indexCodingName = variableName + "_index";
            final IndexCoding indexCoding = getIndexCoding(product, indexCodingName,
                                                           flagMeaningsAttribute, flagValuesAttribute);
            final VirtualBand virtualBand = new VirtualBand(indexCodingName, band.getDataType(),
                                                            band.getSceneRasterWidth(), band.getSceneRasterHeight(),
                                                            band.getName());
            virtualBand.setSampleCoding(indexCoding);
            product.addBand(virtualBand);
        } else if (flagValuesAttribute != null) {
            final IndexCoding indexCoding = getIndexCoding(product, variableName, flagMeaningsAttribute, flagValuesAttribute);
            band.setSampleCoding(indexCoding);
        } else if (flagMasksAttribute != null) {
            final FlagCoding flagCoding = getFlagCoding(product, variableName, flagMeaningsAttribute, flagMasksAttribute);
            band.setSampleCoding(flagCoding);
        }
    }

    private IndexCoding getIndexCoding(Product product, String variableName, Attribute flagMeaningsAttribute,
                                       Attribute flagValuesAttribute) {
        final IndexCoding indexCoding = new IndexCoding(variableName);
        addSamples(indexCoding, flagMeaningsAttribute, flagValuesAttribute);
        if (!product.getIndexCodingGroup().contains(variableName)) {
            product.getIndexCodingGroup().add(indexCoding);
        }
        return indexCoding;
    }

    private FlagCoding getFlagCoding(Product product, String variableName, Attribute flagMeaningsAttribute,
                                     Attribute flagMasksAttribute) {
        final FlagCoding flagCoding = new FlagCoding(variableName);
        addSamples(flagCoding, flagMeaningsAttribute, flagMasksAttribute);
        if (!product.getFlagCodingGroup().contains(variableName)) {
            product.getFlagCodingGroup().add(flagCoding);
        }
        return flagCoding;
    }

    private static void addSamples(SampleCoding sampleCoding, Attribute sampleMeanings, Attribute sampleValues) {
        final String[] meanings = getSampleMeanings(sampleMeanings);
        final int sampleCount = Math.min(meanings.length, sampleValues.getLength());

        for (int i = 0; i < sampleCount; i++) {
            final String sampleName = replaceNonWordCharacters(meanings[i]);
            switch (sampleValues.getDataType()) {
                case BYTE:
                    sampleCoding.addSample(sampleName,
                                           DataType.unsignedByteToShort(
                                                   sampleValues.getNumericValue(i).byteValue()), null
                    );
                    break;
                case SHORT:
                    sampleCoding.addSample(sampleName,
                                           DataType.unsignedShortToInt(
                                                   sampleValues.getNumericValue(i).shortValue()), null
                    );
                    break;
                case INT:
                    sampleCoding.addSample(sampleName, sampleValues.getNumericValue(i).intValue(), null);
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

    protected void addVariableMetadata(Variable variable, Product product) {
        final MetadataElement variableElement = new MetadataElement(variable.getFullName());
        final List<Attribute> attributes = variable.getAttributes();
        for (Attribute attribute : attributes) {
            int type = DataTypeUtils.getEquivalentProductDataType(attribute.getDataType(), false, false);
            final ProductData attributeData = getAttributeData(attribute, type);
            final MetadataAttribute metadataAttribute =
                    new MetadataAttribute(attribute.getFullName(), attributeData, true);
            variableElement.addAttribute(metadataAttribute);
        }
        product.getMetadataRoot().getElement("Variable_Attributes").addElement(variableElement);
    }

    protected ProductData getAttributeData(Attribute attribute, int type) {
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

//    private double[] attributeValuesToDoubleArray(Array attributeValues) {
//        final Object[] array = (Object[]) attributeValues.copyTo1DJavaArray();
//        final int length = array.length;
//        double[] res = new double[length];
//        for (int i = 0; i < length; i++) {
//            res[i] = (Double) array[i];
//        }
//        return res;
//    }

    protected int getWidth() {
        final Dimension widthDimension = netcdfFile.findDimension("columns");
        if (widthDimension != null) {
            return widthDimension.getLength();
        }
        return 0;
    }

    protected int getHeight() {
        final Dimension heightDimension = netcdfFile.findDimension("rows");
        if (heightDimension != null) {
            return heightDimension.getLength();
        }
        return 0;
    }

    String readProductType() {
        Attribute typeAttribute = netcdfFile.findGlobalAttribute(product_type);
        String productType;
        if (typeAttribute != null) {
            productType = typeAttribute.getStringValue();
            if (productType != null && productType.trim().length() > 0) {
                productType = productType.trim();
            }
        } else {
            typeAttribute = netcdfFile.findGlobalAttribute("Conventions");
            if (typeAttribute != null) {
                productType = typeAttribute.getStringValue();
            } else {
                productType = Constants.FORMAT_NAME;
            }
        }
        return productType;
    }

    protected NetcdfFile getNetcdfFile() {
        return netcdfFile;
    }

}
