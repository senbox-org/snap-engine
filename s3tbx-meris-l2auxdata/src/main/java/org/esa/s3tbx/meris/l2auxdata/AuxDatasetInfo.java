/*
 * $Id: AuxDatasetInfo.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;

public class AuxDatasetInfo {

    public static final int MPH = 0;
    public static final int SPH = 1;
    public static final int GADS = 2;
    public static final int MDS = 3;
    public static final int ADS = 4;

    private final AuxFileInfo _fileInfo;
    private final int _index;

    private char _id;
    private int _type;
    private int _recordSize;
    private String _varIdForNumRecords;
    private String _name;


    public AuxDatasetInfo(AuxFileInfo fileInfo, int index) {
        _fileInfo = fileInfo;
        _index = index;
    }

    public AuxFileInfo getFileInfo() {
        return _fileInfo;
    }

    public int getIndex() {
        return _index;
    }

    public char getId() {
        return _id;
    }

    void setId(char id) {
        _id = id;
    }

    public int getType() {
        return _type;
    }

    void setType(int type) {
        _type = type;
    }

    public int getRecordSize() {
        return _recordSize;
    }

    void setRecordSize(int recordSize) {
        _recordSize = recordSize;
    }

    public String getVarIdForNumRecords() {
        return _varIdForNumRecords;
    }

    void setVarIdForNumRecords(String varIdForNumRecords) {
        _varIdForNumRecords = varIdForNumRecords;
    }

    public String getName() {
        return _name;
    }

    void setName(String name) {
        _name = name;
    }
}
