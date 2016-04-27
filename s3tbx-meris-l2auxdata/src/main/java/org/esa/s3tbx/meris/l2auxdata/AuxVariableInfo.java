/*
 * $Id: AuxVariableInfo.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;


public class AuxVariableInfo {

    private final AuxDatasetInfo _datasetInfo;

    private String _id;
    private int _offset;
    private double _scale;
    private int _dataType;
    private int _dim1;
    private int _dim2;
    private int _dim3;
    private int _elementSize; // size in bytes (e.g. if var. is located in MPH/SPH size = 11 characters)
    private int _editFlag;
    private int _editType;
    private int _displayType;
    private String _range;
    private String _comment;
    private String _unit;

    public AuxVariableInfo(AuxDatasetInfo datasetInfo) {
        _datasetInfo = datasetInfo;
    }

    public AuxFileInfo getFileInfo() {
        return _datasetInfo.getFileInfo();
    }

    public AuxDatasetInfo getDatasetInfo() {
        return _datasetInfo;
    }

    public String getComment() {
        return _comment;
    }

    void setComment(String comment) {
        _comment = comment;
    }

    public int getDim1() {
        return _dim1;
    }

    void setDim1(int dim1) {
        _dim1 = dim1;
    }

    public int getDim2() {
        return _dim2;
    }

    void setDim2(int dim2) {
        _dim2 = dim2;
    }

    public int getDim3() {
        return _dim3;
    }

    void setDim3(int dim3) {
        _dim3 = dim3;
    }

    public int getNumElements() {
        int numElements = _dim1;
        if (_dim2 > 0) {
            numElements *= _dim2;
        }
        if (_dim3 > 0) {
            numElements *= _dim3;
        }
        return numElements;
    }

    public int getDisplayType() {
        return _displayType;
    }

    void setDisplayType(int displayType) {
        _displayType = displayType;
    }

    public int getEditFlag() {
        return _editFlag;
    }

    void setEditFlag(int editFlag) {
        _editFlag = editFlag;
    }

    public int getEditType() {
        return _editType;
    }

    void setEditType(int editType) {
        _editType = editType;
    }

    public String getId() {
        return _id;
    }

    void setId(String id) {
        _id = id;
    }

    public int getOffset() {
        return _offset;
    }

    void setOffset(int offset) {
        _offset = offset;
    }

    public String getRange() {
        return _range;
    }

    void setRange(String range) {
        _range = range;
    }

    public double getScale() {
        return _scale;
    }

    void setScale(double scale) {
        _scale = scale;
    }

    public int getNumBytes() {
        return _elementSize;
    }

    public void setElementSize(int elementSize) {
        _elementSize = elementSize;
    }

    public int getDataType() {
        return _dataType;
    }

    void setDataType(int dataType) {
        _dataType = dataType;
    }

    public String getUnit() {
        return _unit;
    }

    void setUnit(String unit) {
        _unit = unit;
    }
}
