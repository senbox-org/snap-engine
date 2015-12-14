/*
 * $Id: AuxFile.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Guardian;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Represents a physical auxiliary database file.
 */
public class AuxFile {

    private final AuxFileInfo _fileInfo;
    private final File _file;

    private ImageInputStream _inputStream;
    private int[] _recordCounts; // _recordCounts.length = #numTables
    private long[] _datasetOffsets; // _datasetOffsets.length = #numTables
    private long _computedFileSize;

    /**
     * Opens an auxiliary database file.
     *
     * @param typeId the database file type ID
     * @param file   the actual file
     * @return an instance of an open auxiliary database file
     * @throws IOException if an I/O error occurs
     */
    public static AuxFile open(char typeId, File file) throws IOException {
        final AuxFileInfo fileInfo = AuxDatabase.getInstance().getFileInfo(typeId);
        final AuxFile auxFile = new AuxFile(fileInfo, file);
        auxFile.open();
        return auxFile;
    }

    /**
     * Constructs a new instance for the given file and database type ID.
     *
     * @param fileInfo the file information, must not be <code>null</code>
     * @param file     the file, must not be <code>null</code>
     */
    public AuxFile(AuxFileInfo fileInfo, File file) {
        _fileInfo = fileInfo;
        _file = file;
    }

    /**
     * Gets information about this database file.
     *
     * @return the information, never <code>null</code>
     */
    public AuxFileInfo getFileInfo() {
        return _fileInfo;
    }

    /**
     * Gets the file.
     *
     * @return the file, never <code>null</code>
     */
    public File getFile() {
        return _file;
    }


    /**
     * Gets the underlying input stream used to readRecord data from the database file. The method returns
     * <code>null</code>, if this file has not been opened so far.
     *
     * @return the input stream or <code>null</code>
     */
    public ImageInputStream getInputStream() {
        return _inputStream;
    }

    /**
     * Returns the computed file size.
     *
     * @return the computed file size, zero if this file has not been opened so far
     */
    public long getComputedFileSize() {
        return _computedFileSize;
    }

    /**
     * Checks if the file is open or not.
     */
    public boolean isOpen() {
        return _inputStream != null;
    }

    /**
     * Opens a database file.
     *
     * @throws IOException           if the file does not exist or an I/O error occurs
     * @throws IllegalStateException if the file is already open
     */
    public void open() throws IOException {
        if (_inputStream != null) {
            throw new IllegalStateException("already open");
        }

        AuxDatabase.getLogger().info("opening auxiliary database file '" + _file.getPath() + "'");

        if (_inputStream == null) {
            _inputStream = new FileImageInputStream(_file);
        }

        final int datasetCount = _fileInfo.getDatasetCount();

        _recordCounts = new int[datasetCount];
        _datasetOffsets = new long[datasetCount];
        long datasetOffset = 0;
        for (int i = 0; i < datasetCount; i++) {
            final AuxDatasetInfo datasetInfo = _fileInfo.getDatasetInfo(i);
            final String varIdForNumRecords = datasetInfo.getVarIdForNumRecords();
            if (varIdForNumRecords != null) {
                _recordCounts[i] = (int) readUInt(varIdForNumRecords);
            } else {
                _recordCounts[i] = 1;
            }
            _datasetOffsets[i] = datasetOffset;
            datasetOffset += _recordCounts[i] * datasetInfo.getRecordSize();
        }
        _computedFileSize = datasetOffset;

        AuxDatabase.getLogger().fine("computed file size is " + _computedFileSize + " bytes");
    }

    /**
     * Closes the database file.
     */
    public void close() {
        if (_inputStream == null) {
            return;
        }
        try {
            _inputStream.close();
        } catch (IOException e) {
            AuxDatabase.getLogger().warning("failed to close auxiliary database file '" + _file.getPath() + "'");
        }
        _inputStream = null;
        _recordCounts = null;
        _datasetOffsets = null;
        _computedFileSize = 0;
        AuxDatabase.getLogger().info("closed auxiliary database file '" + _file.getPath() + "'");
    }

    /**
     * Reads a signed integer (as <code>int</code>) value for the given variable from this database file.
     *
     * @param varId the variable ID
     * @return the value
     * @throws IOException if an I/O error occurs
     */
    public int readInt(String varId) throws IOException {
        final ProductData data = readRecord(varId, 0, 1, ProductData.TYPE_INT32, null);
        return data.getElemInt();
    }

    /**
     * Reads an unsigned integer (as <code>long</code>) value for the given variable from this database file.
     *
     * @param varId the variable ID
     * @return the value
     * @throws IOException if an I/O error occurs
     */
    public long readUInt(String varId) throws IOException {
        final ProductData data = readRecord(varId, 0, 1, ProductData.TYPE_UINT32, null);
        return data.getElemUInt();
    }

    /**
     * Reads a <code>float</code> value for the given variable from this database file.
     *
     * @param varId the variable ID
     * @return the value
     * @throws IOException if an I/O error occurs
     */
    public float readFloat(String varId) throws IOException {
        final ProductData data = readRecord(varId, 0, 1, ProductData.TYPE_FLOAT32, null);
        return data.getElemFloat();
    }

    /**
     * Reads a <code>double</code> value for the given variable from this database file.
     *
     * @param varId the variable ID
     * @return the value
     * @throws IOException if an I/O error occurs
     */
    public double readDouble(String varId) throws IOException {
        final ProductData data = readRecord(varId, 0, 1, ProductData.TYPE_FLOAT64, null);
        return data.getElemDouble();
    }

    /**
     * Reads a <code>int</code> array value for the given variable from this database file.
     *
     * @param varId        the variable ID
     * @param elementCount the number of elements to read, pass -1 if not known
     * @return the value
     * @throws IOException if an I/O error occurs
     */
    public int[] readIntArray(String varId, int elementCount) throws IOException {
        final ProductData data = readRecord(varId, 0, elementCount, ProductData.TYPE_INT32, null);
        return (int[]) data.getElems();
    }

    /**
     * Reads a <code>int</code> array value for the given variable from this database file.
     *
     * @param varId        the variable ID
     * @param elementCount the number of elements to read, pass -1 if not known
     * @return the value
     * @throws IOException if an I/O error occurs
     */
    public int[] readUIntArray(String varId, int elementCount) throws IOException {
        final ProductData data = readRecord(varId, 0, elementCount, ProductData.TYPE_UINT32, null);
        return (int[]) data.getElems();
    }

    /**
     * Reads a <code>float</code> array value for the given variable from this database file.
     *
     * @param varId        the variable ID
     * @param elementCount the number of elements to read, pass -1 if not known
     * @return the value
     * @throws IOException if an I/O error occurs
     */
    public float[] readFloatArray(String varId, int elementCount) throws IOException {
        final ProductData data = readRecord(varId, 0, elementCount, ProductData.TYPE_FLOAT32, null);
        return (float[]) data.getElems();
    }

    /**
     * Reads a <code>double</code> array value for the given variable from this database file.
     *
     * @param varId the variable ID
     * @return the value
     * @throws IOException if an I/O error occurs
     */
    public double[] readDoubleArray(String varId, int elementCount) throws IOException {
        final ProductData data = readRecord(varId, 0, elementCount, ProductData.TYPE_FLOAT64, null);
        return (double[]) data.getElems();
    }

    /**
     * Reads a data record for the given variable from this database file.
     *
     * @param varId   the variable ID
     * @param varType the variable type
     * @return the data, never <code>null</code>
     * @throws IOException if an I/O error occurs
     */
    public ProductData readRecord(String varId, int varType) throws IOException {
        return readRecord(varId, 0, -1, varType, null);
    }

    /**
     * Reads a data record for the given variable from this database file.
     *
     * @param varId          the variable ID, must not be <code>null</code>
     * @param recordIndex    the record index
     * @param elementCount   the expected element count, <code>-1</code> if not used
     * @param memoryDataType the type of the variable
     * @param memoryData     an instance of data to be reused, if <code>null</code> the method creates a new data
     *                       instance
     * @return the data, never <code>null</code>
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if one of the arguments is invalid
     * @throws IllegalStateException    if this object is in an illegal state, e.g. the file is not open
     */
    public ProductData readRecord(String varId,
                                  int recordIndex,
                                  int elementCount,
                                  int memoryDataType,
                                  ProductData memoryData) throws IOException {

        Guardian.assertNotNullOrEmpty("varId", varId);
        if (_inputStream == null) {
            throw new IllegalStateException("no input stream");
        }

        final AuxVariableInfo variableInfo = AuxDatabase.getInstance().getVariableInfo(varId);
        final AuxDatasetInfo datasetInfo = variableInfo.getDatasetInfo();
        if (getFileInfo() != datasetInfo.getFileInfo()) {
            throw new IllegalArgumentException("illegal varId: " + varId);
        }

        // memory data is the one returned by this method,
        // storage data is the one readRecord from file
        final int storageDataType = variableInfo.getDataType();

        final int storageElemsCount = variableInfo.getNumElements();

        final int memoryElemsCount;
        if (storageDataType == ProductData.TYPE_ASCII && memoryDataType != ProductData.TYPE_ASCII) {
            memoryElemsCount = 1; // ASCII --> always converted to scalar
        } else {
            memoryElemsCount = storageElemsCount;
        }
        if (elementCount > 0 && elementCount != memoryElemsCount) {
            throw new IOException("Auxiliary variable '" + variableInfo.getId() + "': " +
                    "Element count mismatch, expected " + elementCount + ", but was " + memoryElemsCount);
        }

        if (memoryData != null) {
            checkMemoryData(memoryData, memoryDataType, memoryElemsCount);
        } else {
            memoryData = allocate(variableInfo, memoryDataType, memoryElemsCount);
        }

        final ProductData storageData;
        if (memoryDataType == storageDataType) {
            storageData = memoryData;
        } else {
            AuxDatabase.getLogger().warning(createDataConversionMessage(variableInfo, storageDataType, memoryDataType));
            storageData = allocate(variableInfo, storageDataType, storageElemsCount);
        }

        checkRecordSize(variableInfo, storageData);
        long offset = getOffset(datasetInfo, variableInfo, recordIndex);
        _inputStream.seek(offset);
        storageData.readFrom(_inputStream);
        convertData(variableInfo, storageData, memoryData);
        return memoryData;
    }

    private void checkMemoryData(ProductData memoryData, final int memoryDataType, int memoryElemsCount) {
        if (memoryDataType == ProductData.TYPE_ASCII && !(memoryData instanceof ProductData.ASCII) ||
                memoryDataType == ProductData.TYPE_UTC && !(memoryData instanceof ProductData.UTC) ||
                (memoryDataType != ProductData.TYPE_ASCII &&
                        memoryDataType != ProductData.TYPE_UTC &&
                        memoryDataType != memoryData.getType())) {
            throw new IllegalArgumentException("illegal memoryData: memoryDataType mismatch");
        }
        if (memoryData.getNumElems() != memoryElemsCount) {
            throw new IllegalArgumentException("illegal memoryData: memoryElemsCount mismatch");
        }
    }

    private void checkRecordSize(final AuxVariableInfo variableInfo, final ProductData storageData) {
        final int variableRecSize = variableInfo.getNumBytes();
        final int storageRecSize = storageData.getElemSize() * storageData.getNumElems();
        if (variableRecSize != storageRecSize) {
            AuxDatabase.getLogger().warning(createRecordSizeMismatchMessage(variableInfo, variableRecSize, storageRecSize));
        }
    }

    // todo - Olaf fragen, wie man private methoden testet
    private long getOffset(final AuxDatasetInfo datasetInfo, final AuxVariableInfo variableInfo, int recordIndex) {
        final long datasetOffset = _datasetOffsets[datasetInfo.getIndex()];
        final int variableOffset = variableInfo.getOffset();
        return datasetOffset + variableOffset + recordIndex * datasetInfo.getRecordSize();
    }

    private int convertData(final AuxVariableInfo variableInfo, final ProductData storageData, ProductData memoryData) throws IOException {
        int elementCount = storageData.getNumElems();
        if (storageData instanceof ProductData.ASCII) {
            final int memoryDataType = memoryData.getType();
            if (memoryData instanceof ProductData.ASCII) {
                // OK, no conversion required
            } else if (ProductData.isUIntType(memoryDataType)) {
                elementCount = convertAsciiToUnsignedInteger(storageData, memoryData);
            } else if (ProductData.isIntType(memoryDataType)) {
                elementCount = convertAsciiToSignedInteger(storageData, memoryData);
            } else if (ProductData.isFloatingPointType(memoryDataType)) {
                elementCount = convertAsciiToFloatingPoint(storageData, memoryData);
            } else {
                throw new IOException("illegal data conversion, aux-variable " + variableInfo.getId());
            }
        }
        double scale = variableInfo.getScale();
        if (storageData == memoryData && scale != 1.0 ||
                storageData != memoryData && storageData.getNumElems() == memoryData.getNumElems()) {
            for (int i = 0; i < storageData.getNumElems(); i++) {
                memoryData.setElemDoubleAt(i, scale * storageData.getElemDoubleAt(i));
            }
        }
        return elementCount;
    }

    private int convertAsciiToUnsignedInteger(final ProductData storageData, ProductData memoryData) throws IOException {
        final String ascii = storageData.getElemString().trim();
        final int sign = getSignum(ascii);
        if (sign == -1) {
            throw new IOException("cannot convert ASCII '" + ascii + "' to unsigned integer number");
        }
        final String asciiNumber = getAsciiNumber(ascii);
        try {
            memoryData.setElemUInt(sign * Long.parseLong(asciiNumber));
            return 1;
        } catch (NumberFormatException e) {
            throw new IOException("cannot convert ASCII '" + ascii + "' to unsigned integer number");
        }
    }

    private int convertAsciiToSignedInteger(final ProductData storageData, ProductData memoryData) throws IOException {
        final String ascii = storageData.getElemString().trim();
        final int sign = getSignum(ascii);
        final String asciiNumber = getAsciiNumber(ascii);
        try {
            memoryData.setElemInt(sign * Integer.parseInt(asciiNumber));
            return 1;
        } catch (NumberFormatException e) {
            throw new IOException("failed to convert ASCII '" + ascii + "' to signed integer number");
        }
    }

    private int convertAsciiToFloatingPoint(final ProductData storageData, ProductData memoryData) throws IOException {
        final String ascii = storageData.getElemString().trim();
        final int sign = getSignum(ascii);
        final String asciiNumber = getAsciiNumber(ascii);
        try {
            memoryData.setElemDouble(sign * Double.parseDouble(asciiNumber));
            return 1;
        } catch (NumberFormatException e) {
            throw new IOException("failed to convert ASCII '" + ascii + "' to floating point number");
        }
    }

    private int getSignum(final String ascii) {
        return ascii.startsWith("-") ? -1 : 1;
    }

    private String getAsciiNumber(final String ascii) {
        String asciiNumber;
        if (ascii.startsWith("-") || ascii.startsWith("+") || ascii.startsWith("0")) {
            int pos = 1;
            while (ascii.charAt(pos) == '0' && pos < ascii.length() - 1) {
                pos++;
            }
            asciiNumber = ascii.substring(pos);
        } else {
            asciiNumber = ascii;
        }
        return asciiNumber;
    }

    private ProductData allocate(final AuxVariableInfo variableInfo,
                                 final int dataType,
                                 final int numElems) throws IOException {
        final int byteCount = ProductData.getElemSize(dataType) * numElems;
        final int mbyteCount = byteCount / (1024 * 1024);
        if (mbyteCount > 10) {
            AuxDatabase.getLogger().info(createHugeBufferMessage(variableInfo, mbyteCount));
        }
        try {
            return ProductData.createInstance(dataType, numElems);
        } catch (OutOfMemoryError e) {
            AuxDatabase.getLogger().log(Level.SEVERE, createOutOfMemoryMessage(variableInfo, mbyteCount), e);
            throw new IOException("Out of memory, failed allocate data buffer.");
        }
    }

    private String createOutOfMemoryMessage(final AuxVariableInfo variableInfo, final int mbyteCount) {
        return createMessagePrefix(variableInfo) +
                "out of memory, failed to allocate data buffer (" + mbyteCount + " M)";
    }

    private String createHugeBufferMessage(final AuxVariableInfo variableInfo, final int mbyteCount) {
        return createMessagePrefix(variableInfo) +
                "about to allocate \"huge\" data buffer (" + mbyteCount + " M)";
    }

    private String createDataConversionMessage(final AuxVariableInfo variableInfo, final int storageDataType,
                                               int memoryDataType) {
        return createMessagePrefix(variableInfo) +
                "data conversion required from storage data type (" + ProductData.getTypeString(storageDataType) + ") " +
                "to memory data type (" + ProductData.getTypeString(memoryDataType) + ")";
    }

    private String createRecordSizeMismatchMessage(final AuxVariableInfo variableInfo, final int variableRecSize,
                                                   final int storageRecSize) {
        return createMessagePrefix(variableInfo) +
                "variable record size (" + variableRecSize + ") " +
                "does not match storage record size (" + storageRecSize + ")";
    }

    private String createMessagePrefix(final AuxVariableInfo variableInfo) {
        return "file '" + _file.getName() + "': " +
                "variable '" + variableInfo.getId() + "': ";
    }
}
