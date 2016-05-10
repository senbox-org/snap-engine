package org.esa.s3tbx.slstr.pdu.stitching;

import com.bc.ceres.binding.converters.DateFormatConverter;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.nc.NWritableFactory;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tonio Fincke
 */
class NcFileStitcher {

    private static final String PRODUCT_NAME = "product_name";
    private static final String CREATION_TIME = "creation_time";

    static File stitchNcFiles(String fileName, File targetDirectory, Date creationDate,
                              File[] ncFiles, ImageSize targetImageSize, ImageSize[] imageSizes) throws IOException, PDUStitchingException {
        NetcdfFile[] inputFiles = new NetcdfFile[ncFiles.length];
        List<Attribute>[] globalAttributes = new ArrayList[ncFiles.length];
        List<Dimension>[] dimensions = new List[ncFiles.length];
        List<Variable>[] variables = new List[ncFiles.length];
        for (int i = 0; i < ncFiles.length; i++) {
            inputFiles[i] = NetcdfFileOpener.open(ncFiles[i]);
            globalAttributes[i] = inputFiles[i].getGlobalAttributes();
            dimensions[i] = inputFiles[i].getDimensions();
            variables[i] = inputFiles[i].getVariables();
        }
        final File file = new File(targetDirectory, fileName);
        final NFileWriteable netcdfWriteable = NWritableFactory.create(file.getAbsolutePath(), "netcdf4");
        setGlobalAttributes(netcdfWriteable, globalAttributes, targetDirectory.getName(), creationDate);
        setDimensions(netcdfWriteable, dimensions, targetImageSize, variables);
        final Map<String, Array> variableToArrayMap =
                defineVariables(netcdfWriteable, variables, targetImageSize, imageSizes);
        netcdfWriteable.create();
        for (String variableName : variableToArrayMap.keySet()) {
            netcdfWriteable.findVariable(variableName).writeFully(variableToArrayMap.get(variableName));
        }
        netcdfWriteable.close();
        for (NetcdfFile inputFile : inputFiles) {
            inputFile.close();
        }
        return file;
    }

    private static Map<String, Array> defineVariables(NFileWriteable netcdfWriteable, List<Variable>[] variableLists,
                                                      ImageSize targetImageSize, ImageSize[] imageSizes)
            throws PDUStitchingException, IOException {
        Map<String, Array> variableToArray = new HashMap<>();
        List<String> namesOfAddedVariables = new ArrayList<>();
        for (int i = 0; i < variableLists.length; i++) {
            List<Variable> variables = variableLists[i];
            for (Variable variable : variables) {
                final String variableName = variable.getFullName();
                //todo maybe there is a need to support variables without dimensions
                if (!namesOfAddedVariables.contains(variableName) && variable.getDimensions().size() > 0) {
                    checkWhetherVariableHasSameDimensionsAcrossFiles(i, variable, variableLists);
                    addVariableToWritable(netcdfWriteable, variable);
                    final NVariable nVariable = netcdfWriteable.findVariable(variableName);
                    final int indexOfRowDimension = getIndexOfRowDimension(variable.getDimensions());
                    if (indexOfRowDimension < 0) {
                        variableToArray.put(variableName, getValidArrayFromVariable(variable));
                    } else {
                        Array nVariableArray =
                                createStitchedArray(variable, targetImageSize, imageSizes, indexOfRowDimension, variableLists);
                        variableToArray.put(variableName, nVariableArray);
                    }
                    addVariableAttributes(nVariable, variable, i, variableLists);
                    namesOfAddedVariables.add(variableName);
                }
            }
        }
        return variableToArray;
    }

    private static Array getValidArrayFromVariable(Variable variable) throws IOException {
        if (variable.getDataType().isString()) {
            Array array;
            try {
                array = variable.read();
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw e;
                } else {
                    array = Array.factory(variable.getDataType(), variable.getShape());
                }
            }
            final IndexIterator indexIterator = array.getIndexIterator();
            while (indexIterator.hasNext()) {
                if (indexIterator.next() == null) {
                    indexIterator.setObjectCurrent("");
                }
            }
            return array;
        }
        return variable.read();
    }

    private static void addVariableToWritable(NFileWriteable netcdfWriteable, Variable variable) throws IOException {
        if (variable.getDataType().isString()) {
            netcdfWriteable.addVariable(variable.getFullName(), variable.getDataType(), variable.isUnsigned(),
                                        null, variable.getDimensionsString(), 0);
        } else {
            netcdfWriteable.addVariable(variable.getFullName(), variable.getDataType(), variable.isUnsigned(),
                                        null, variable.getDimensionsString());
        }
    }

    private static Array createStitchedArray(Variable variable, ImageSize targetImageSize, ImageSize[] imageSizes,
                                             int indexOfRowDimension, List<Variable>[] variableLists) throws IOException {
        final String variableName = variable.getFullName();
        final int[] sectionSizes = new int[variableLists.length];
        final int[][] sourceOffsets = new int[variableLists.length][];
        final Variable[] fileVariables = new Variable[variableLists.length];
        for (int i = 0; i < variableLists.length; i++) {
            final Variable fileVariable = getVariableFromList(variableName, variableLists[i]);
            if (fileVariable != null) {
                fileVariables[i] = fileVariable;
                sectionSizes[i] = determineSectionSize(indexOfRowDimension, fileVariable);
                sourceOffsets[i] = determineSourceOffsets(sectionSizes[i], fileVariable);
            }
        }
        int[] rowOffsets = new int[imageSizes.length];
        int[] numberOfRows = new int[imageSizes.length];
        for (int j = 0; j < imageSizes.length; j++) {
            rowOffsets[j] = (imageSizes[j].getStartOffset() - targetImageSize.getStartOffset());
            numberOfRows[j] = imageSizes[j].getRows();
        }
        final int[][] destinationOffsets = determineDestinationOffsets(rowOffsets, numberOfRows, sectionSizes, sourceOffsets);
        int[] nVariableShape = new int[variable.getDimensions().size()];
        for (int j = 0; j < nVariableShape.length; j++) {
            if (variable.getDimensions().get(j).getFullName().equals("rows")) {
                nVariableShape[j] = targetImageSize.getRows();
            } else {
                nVariableShape[j] = variable.getDimensions().get(j).getLength();
            }
        }
        final Array nVariableArray = Array.factory(variable.getDataType(), nVariableShape);
        //todo prefill array with no data value
        for (int j = 0; j < variableLists.length; j++) {
            final Variable fileVariable = fileVariables[j];
            if (fileVariable != null) {
                final Array fileArray = fileVariable.read();
                for (int l = 0; l < sourceOffsets[j].length; l++) {
                    Array.arraycopy(fileArray, sourceOffsets[j][l], nVariableArray,
                                    destinationOffsets[j][l], sectionSizes[j]);
                }
            }
        }
        return nVariableArray;
    }

    private static void addVariableAttributes(NVariable nVariable, Variable variable, int variableIndex,
                                              List<Variable>[] variableLists) throws IOException {
        final String variableName = variable.getFullName();
        final List<Attribute> variableAttributes = variable.getAttributes();
        if (variableIndex < variableLists.length) {
            for (final Attribute variableAttribute : variableAttributes) {
                for (int k = variableIndex; k < variableLists.length; k++) {
                    final Variable otherVariable = getVariableFromList(variableName, variableLists[k]);
                    if (otherVariable != null) {
                        final List<Attribute> otherVariableAttributes = otherVariable.getAttributes();
                        final Attribute otherAttribute =
                                getAttributeFromList(variableAttribute.getFullName(), otherVariableAttributes);
                        if (otherAttribute != null) {
                            if (!areAttributeValuesEqual(variableAttribute, otherAttribute)) {
                                addAttributeToNVariable(nVariable, otherAttribute, k);
                            }
                        }
                    }
                }
                addAttributeToNVariable(nVariable, variableAttribute);
            }
        }
    }

    private static void checkWhetherVariableHasSameDimensionsAcrossFiles(int listIndex, Variable variable,
                                                                         List<Variable>[] variableLists)
            throws PDUStitchingException {
        final String variableName = variable.getFullName();
        for (int j = listIndex; j < variableLists.length; j++) {
            final Variable otherVariable = getVariableFromList(variableName, variableLists[j]);
            if (otherVariable != null &&
                    !otherVariable.getDimensionsString().equals(variable.getDimensionsString())) {
                throw new PDUStitchingException("Variable " + variableName + " has different dimensions" +
                                                        "across input files");
            }
        }
    }

    private static int getIndexOfRowDimension(List<Dimension> variableDimensions) {
        int indexOfRowDimension = -1;
        for (int j = 0; j < variableDimensions.size(); j++) {
            Dimension variableDimension = variableDimensions.get(j);
            if (variableDimension.getFullName().equals("rows")) {
                indexOfRowDimension = j;
                break;
            }
        }
        return indexOfRowDimension;
    }

    private static void addAttributeToNVariable(NVariable nVariable, Attribute referenceAttribute, int index) throws IOException {
        addAttributeToNVariable(nVariable, referenceAttribute.getFullName() + "_" + index, referenceAttribute);
    }

    private static void addAttributeToNVariable(NVariable nVariable, Attribute referenceAttribute) throws IOException {
        addAttributeToNVariable(nVariable, referenceAttribute.getFullName(), referenceAttribute);
    }

    private static void addAttributeToNVariable(NVariable nVariable, String name, Attribute referenceAttribute) throws IOException {
        if (referenceAttribute.isArray()) {
            nVariable.addAttribute(name, referenceAttribute.getValues());
        } else if (referenceAttribute.isString()) {
            nVariable.addAttribute(name, referenceAttribute.getStringValue());
        } else {
            nVariable.addAttribute(name, referenceAttribute.getNumericValue(), referenceAttribute.isUnsigned());
        }
    }

    private static Dimension getDimensionFromList(String name, List<Dimension> dimensionList) {
        for (Dimension dimension : dimensionList) {
            if (dimension.getFullName().equals(name)) {
                return dimension;
            }
        }
        return null;
    }

    private static Attribute getAttributeFromList(String name, List<Attribute> attributeList) {
        for (Attribute attribute : attributeList) {
            if (attribute.getFullName().equals(name)) {
                return attribute;
            }
        }
        return null;
    }

    private static Variable getVariableFromList(String name, List<Variable> variableList) {
        for (Variable aVariableList : variableList) {
            if (aVariableList.getFullName().equals(name)) {
                return aVariableList;
            }
        }
        return null;
    }

    static int[][] determineDestinationOffsets(int[] rowOffsets, int[] numberOfRows,
                                               int[] sectionSizes, int[][] sourceOffsets) {
        int[][] destinationOffsets = new int[sectionSizes.length][];
        int allSectionsSize = 0;
        for (int sectionSize : sectionSizes) {
            allSectionsSize += sectionSize;
        }
        for (int i = 0; i < sectionSizes.length; i++) {
            final int fileOffset = rowOffsets[i] * (sectionSizes[i] / numberOfRows[i]);
            destinationOffsets[i] = new int[sourceOffsets[i].length];
            for (int j = 0; j < sourceOffsets[i].length; j++) {
                destinationOffsets[i][j] = fileOffset + j * allSectionsSize;
            }
        }
        return destinationOffsets;
    }

    static int[] determineSourceOffsets(int sectionSize, Variable variable) {
        int totalSize = 1;
        for (int i = 0; i < variable.getDimensions().size(); i++) {
            totalSize *= variable.getDimension(i).getLength();
        }
        final int numberOfSections = totalSize / sectionSize;
        int[] sourceOffsets = new int[numberOfSections];
        for (int i = 0; i < numberOfSections; i++) {
            sourceOffsets[i] = i * sectionSize;
        }
        return sourceOffsets;
    }

    static int determineSectionSize(int indexOfRowDimension, Variable variable) {
        int size = 1;
        for (int i = indexOfRowDimension; i < variable.getDimensions().size(); i++) {
            size *= variable.getDimension(i).getLength();
        }
        return size;
    }

    static void setDimensions(NFileWriteable nFileWriteable, List<Dimension>[] dimensionLists,
                              ImageSize targetImageSize, List<Variable>[] variableLists)
            throws PDUStitchingException, IOException {
        List<String> namesOfAddedDimensions = new ArrayList<>();
        for (int i = 0; i < dimensionLists.length; i++) {
            List<Dimension> dimensions = dimensionLists[i];
            for (Dimension dimension : dimensions) {
                final String dimensionName = dimension.getFullName();
                if (!namesOfAddedDimensions.contains(dimensionName)) {
                    switch (dimensionName) {
                        case "rows":
                            nFileWriteable.addDimension("rows", targetImageSize.getRows());
                            break;
                        case "columns":
                            nFileWriteable.addDimension("columns", targetImageSize.getColumns());
                            break;
                        default:
                            checkWhetherEquallyNamedVariablesContainEqualValues(dimensionName, i, variableLists);
                            checkWhetherDimensionLengthIsEqualAcrossAllEquallyNamedDimensions(dimension, i, dimensionLists);
                            nFileWriteable.addDimension(dimensionName, dimension.getLength());
                            break;
                    }
                    namesOfAddedDimensions.add(dimensionName);
                }
            }
        }
    }

    private static void checkWhetherEquallyNamedVariablesContainEqualValues(String dimensionName, int dimensionIndex,
                                                                            List<Variable>[] variableLists)
            throws IOException, PDUStitchingException {
        Array referenceVariableArray = null;
        for (int a = dimensionIndex; a < variableLists.length; a++) {
            final Variable variable = getVariableFromList(dimensionName, variableLists[a]);
            if (variable != null) {
                final Array variableArray = variable.read();
                if (referenceVariableArray != null &&
                        !areArraysEqual(variableArray, referenceVariableArray)) {
                    throw new PDUStitchingException("Values for " + variable.getFullName() +
                                                            " are different across input files");
                }
                referenceVariableArray = variableArray;
            }
        }
    }

    private static void checkWhetherDimensionLengthIsEqualAcrossAllEquallyNamedDimensions(Dimension dimension,
                                                                                          int dimensionIndex,
                                                                                          List<Dimension>[] dimensionLists)
            throws PDUStitchingException {
        final int dimensionLength = dimension.getLength();
        final String dimensionName = dimension.getFullName();
        for (int j = dimensionIndex; j < dimensionLists.length; j++) {
            final Dimension otherDimension = getDimensionFromList(dimensionName, dimensionLists[j]);
            if (otherDimension != null && dimensionLength != otherDimension.getLength()) {
                throw new PDUStitchingException("Dimension " + dimensionName +
                                                        " has different lengths across input files");
            }
        }
    }

    static void setGlobalAttributes(NFileWriteable nFileWriteable, List<Attribute>[] globalAttributeLists,
                                    String parentDirectoryName, Date creationDate) throws IOException {
        final DateFormatConverter globalAttributesDateFormatConverter =
                new DateFormatConverter(new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'"));
        List<String> namesOfAddedAttributes = new ArrayList<>();
        for (int i = 0; i < globalAttributeLists.length; i++) {
            List<Attribute> globalAttributes = globalAttributeLists[i];
            for (Attribute globalAttribute : globalAttributes) {
                final String globalAttributeName = globalAttribute.getFullName();
                if (!namesOfAddedAttributes.contains(globalAttributeName)) {
                    if (globalAttributeName.equals(PRODUCT_NAME)) {
                        nFileWriteable.addGlobalAttribute(PRODUCT_NAME, parentDirectoryName);
                    } else if (globalAttributeName.equals(CREATION_TIME)) {
                        nFileWriteable.addGlobalAttribute(CREATION_TIME,
                                                          globalAttributesDateFormatConverter.format(creationDate));
                    } else if (globalAttribute.isArray()) {
                        final Array values = globalAttribute.getValues();
                        for (int j = i; j < globalAttributeLists.length; j++) {
                            final Attribute otherGlobalAttribute =
                                    getAttributeFromList(globalAttributeName, globalAttributeLists[j]);
                            if (otherGlobalAttribute != null && values != otherGlobalAttribute.getValues()) {
                                nFileWriteable.addGlobalAttribute(globalAttributeName + "_" + j,
                                                                  otherGlobalAttribute.getValues().toString());
                                break;
                            }
                        }
                        nFileWriteable.addGlobalAttribute(globalAttributeName, values.toString());
                    } else if (globalAttribute.getDataType().isNumeric()) {
                        final Number value = globalAttribute.getNumericValue();
                        for (int j = i; j < globalAttributeLists.length; j++) {
                            final Attribute otherGlobalAttribute =
                                    getAttributeFromList(globalAttributeName, globalAttributeLists[j]);
                            if (otherGlobalAttribute != null && !value.equals(otherGlobalAttribute.getNumericValue())) {
                                //todo write this as numeric, not as string - tf 20160122
                                nFileWriteable.addGlobalAttribute(globalAttributeName + "_" + j,
                                                                  otherGlobalAttribute.getNumericValue().toString());
                                break;
                            }
                        }
                        nFileWriteable.addGlobalAttribute(globalAttributeName, value.toString());
                    } else {
                        final String value = globalAttribute.getStringValue();
                        for (int j = i; j < globalAttributeLists.length; j++) {
                            final Attribute otherGlobalAttribute =
                                    getAttributeFromList(globalAttributeName, globalAttributeLists[j]);
                            if (otherGlobalAttribute != null && !value.equals(otherGlobalAttribute.getStringValue())) {
                                nFileWriteable.addGlobalAttribute(globalAttributeName + "_" + j,
                                                                  otherGlobalAttribute.getStringValue());
                                break;
                            }
                        }
                        nFileWriteable.addGlobalAttribute(globalAttributeName, value);
                    }
                }
                namesOfAddedAttributes.add(globalAttributeName);
            }
        }
    }

    private static boolean areAttributeValuesEqual(Attribute attribute1, Attribute attribute2) {
        if (attribute1.isArray()) {
            final Array values1 = attribute1.getValues();
            final Array values2 = attribute2.getValues();
            return areArraysEqual(values1, values2);
        } else if (attribute1.isString()) {
            final String value1 = attribute1.getStringValue();
            final String value2 = attribute2.getStringValue();
            return value1.equals(value2);
        }
        final Number numericValue1 = attribute1.getNumericValue();
        final Number numericValue2 = attribute2.getNumericValue();
        return numericValue1.equals(numericValue2);
    }

    private static boolean areArraysEqual(Array array1, Array array2) {
        for (int i = 0; i < array1.getSize(); i++) {
            if (!array1.getObject(i).equals(array2.getObject(i))) {
                return false;
            }
        }
        return true;
    }

}
