/*
 * $Id: AuxFileInfo.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;


public class AuxFileInfo {

    private char _typeId; // '0'...'9', 'A'...'Z'
    private String _dirName;
    private int _datasetCount;
    private AuxDatasetInfo[] _datasetInfos; // a database file comprises multiple datasets
    private AuxVariableInfo[] _variableInfos; // some datasets contain data for variables

    private boolean _editable;  // not used
    private boolean _import; // not used
    private String _description;  // not used

    public AuxFileInfo() {
    }

    public char getTypeId() {
        return _typeId;
    }

    void setTypeId(char typeId) {
        _typeId = typeId;
    }

    public String getDirName() {
        return _dirName;
    }

    void setDirName(String dirName) {
        _dirName = dirName;
    }

    public String getDescription() {
        return _description;
    }

    void setDescription(String description) {
        _description = description;
    }

    public boolean isImport() {
        return _import;
    }

    void setImport(boolean anImport) {
        _import = anImport;
    }

    public boolean isEditable() {
        return _editable;
    }

    void setEditable(boolean editable) {
        _editable = editable;
    }

    public int getDatasetCount() {
        return _datasetCount;
    }

    public AuxDatasetInfo getDatasetInfo(final int index) {
        return _datasetInfos[index];
    }

    public AuxDatasetInfo getDatasetInfo(final char id) {
        final int index = getDatasetIndex(id);
        if (index == -1) {
            throw new IllegalArgumentException("database " + getDirName() + ": invalid dataset index: '" + id + "'");
        }
        return getDatasetInfo(index);
    }

    public int getDatasetIndex(final char id) {
        for (int i = 0; i < _datasetInfos.length; i++) {
            final AuxDatasetInfo datasetInfo = _datasetInfos[i];
            if (datasetInfo.getId() == id) {
                return i;
            }
        }
        return -1;
    }

    public int getVariableCount() {
        return _variableInfos.length;
    }

    public AuxVariableInfo getVariableInfo(final int index) {
        return _variableInfos[index];
    }

    public int getVariableIndex(final String id) {
        for (int i = 0; i < getVariableCount(); i++) {
            final AuxVariableInfo variableInfo = _variableInfos[i];
            if (variableInfo.getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public AuxVariableInfo getVariableInfo(final String id) {
        final int index = getVariableIndex(id);
        if (index == -1) {
            throw new IllegalArgumentException("database " + getDirName() + ": invalid id: " + id);
        }
        return getVariableInfo(index);
    }


    void setVariableInfos(AuxVariableInfo[] variableInfos) {
        _variableInfos = variableInfos;
    }

    void setDatasetCount(int datasetCount) {
        _datasetCount = datasetCount;
        _datasetInfos = new AuxDatasetInfo[_datasetCount];
        for (int i = 0; i < _datasetInfos.length; i++) {
            _datasetInfos[i] = new AuxDatasetInfo(this, i);
        }
    }
}
