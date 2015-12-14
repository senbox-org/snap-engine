/*
 * $Id: AuxDatabase.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;

import org.esa.snap.core.datamodel.ProductData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * The <code>AuxDatabase</code> class provides access to the description of MERIS L1b and L2 auxiliary database files
 * and variables.
 * <p/>
 * It is implemented as a singleton, since all information provided by this class is globally accessible.
 */
@SuppressWarnings("JavaDoc")
public class AuxDatabase {


    private static AuxDatabase _instance = new AuxDatabase();
    private AuxFileInfo[] _fileInfos;
    private HashMap _varIdToVarInfoMap;

    /////////////////////////////////////////////////////////////////////////
    // Public methods

    /**
     * Gets the one and only instance of this class.
     *
     * @return the instance
     */
    public static AuxDatabase getInstance() {
        return _instance;
    }

    /**
     * Gets the file information for the given database type.
     *
     * @param type the database type, e.g. '1', '2', 'P', 'A', ...
     * @return the file information
     */
    public AuxFileInfo getFileInfo(char type) {
        for (AuxFileInfo fileInfo : _fileInfos) {
            if (fileInfo.getTypeId() == type) {
                return fileInfo;
            }
        }
        throw new IllegalArgumentException("illegal type: '" + type + "'");
    }

    /**
     * Gets the variable information for the given variable ID.
     *
     * @param varId the variable ID, e.g. "P10U"
     * @return
     */
    public AuxVariableInfo getVariableInfo(String varId) {
        return (AuxVariableInfo) _varIdToVarInfoMap.get(varId);
    }

    /**
     * Gets the logger used by the auxiliray database.
     *
     * @return the logger
     */
    public static Logger getLogger() {
        return Logger.getLogger("org.esa.beam.dataproc.meris.sdr.auxdata");
    }

    /////////////////////////////////////////////////////////////////////////
    // Private methods

    private AuxDatabase() {
        getLogger().info("loading auxiliary resources...");
        try {
            initFileInfos();
            initVariableInfos();
        } catch (IOException e) {
            throw new IllegalStateException("auxiliary resource I/O error: " + e.getMessage());
        }
        getLogger().info("auxiliary resources loaded");
    }


    private void initFileInfos() throws IOException {
        final String resourceName = "database.db";
        final InputStream istream = getResourceAsStream(resourceName);
        if (istream == null) {
            throw new IllegalArgumentException("resource I/O error: resource not found: " + resourceName);
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(istream))) {
            _fileInfos = loadFileInfos(r);
        }
    }

    private void initVariableInfos() throws IOException {
        _varIdToVarInfoMap = new HashMap();
        for (final AuxFileInfo fileInfo : _fileInfos) {
            final String resourceName = "index_" + (fileInfo.getTypeId() + ".txa").toLowerCase();
            final InputStream istream = getResourceAsStream(resourceName);
            if (istream != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(istream))) {
                    final AuxVariableInfo[] variableInfos = loadVariableInfos(fileInfo, r);
                    fileInfo.setVariableInfos(variableInfos);
                    for (int j = 0; j < variableInfos.length; j++) {
                        final AuxVariableInfo variableInfo = fileInfo.getVariableInfo(j);
                        _varIdToVarInfoMap.put(variableInfo.getId(), variableInfo);
                    }
                }
            } else {
                getLogger().warning("missing resource (might be OK): " + resourceName);
            }
        }
    }

    private static AuxFileInfo[] loadFileInfos(BufferedReader r) throws IOException {
        final List fileInfoList = new ArrayList();
        AuxFileInfo fileInfo = null;
        StringTokenizer st;
        int dataIndex = 0;
        while (true) {
            String line = r.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.startsWith("#")) {
                continue;
            }
            if (dataIndex == 0) {
                fileInfo = new AuxFileInfo();
                fileInfoList.add(fileInfo);
                fileInfo.setTypeId(line.charAt(0));
            } else if (dataIndex == 1) {
                st = new StringTokenizer(line);
                fileInfo.setEditable(Integer.parseInt(st.nextToken()) != 0);
                fileInfo.setImport(Integer.parseInt(st.nextToken()) != 0);
                fileInfo.setDirName(st.nextToken());
            } else if (dataIndex == 2) {
                fileInfo.setDescription(line);
            } else if (dataIndex == 3) {
                fileInfo.setDatasetCount(Integer.parseInt(line));
            } else if (dataIndex == 4) {
                st = new StringTokenizer(line);
                for (int i = 0; i < fileInfo.getDatasetCount(); i++) {
                    final int index = Integer.parseInt(st.nextToken());
                    final char id = (char) (index < 10 ? ('0' + index) : ('A' + index - 10));
                    fileInfo.getDatasetInfo(i).setId(id);
                }
            } else if (dataIndex == 5) {
                st = new StringTokenizer(line);
                for (int i = 0; i < fileInfo.getDatasetCount(); i++) {
                    fileInfo.getDatasetInfo(i).setType(Integer.parseInt(st.nextToken()));
                }
            } else if (dataIndex == 6) {
                st = new StringTokenizer(line);
                for (int i = 0; i < fileInfo.getDatasetCount(); i++) {
                    fileInfo.getDatasetInfo(i).setRecordSize(Integer.parseInt(st.nextToken()));
                }
            } else if (dataIndex == 7) {
                st = new StringTokenizer(line);
                for (int i = 0; i < fileInfo.getDatasetCount(); i++) {
                    final String varIdForNumRecs = st.nextToken();
                    if (varIdForNumRecs.length() > 1) {
                        fileInfo.getDatasetInfo(i).setVarIdForNumRecords(varIdForNumRecs);
                    }
                }
            } else if (dataIndex >= 8 && dataIndex < 8 + fileInfo.getDatasetCount()) {
                int i = dataIndex - 8;
                fileInfo.getDatasetInfo(i).setName(line);
            } else {
                dataIndex = -1;
            }
            dataIndex++;
        }

        AuxFileInfo[] databaseInfos = new AuxFileInfo[fileInfoList.size()];
        fileInfoList.toArray(databaseInfos);
        return databaseInfos;
    }


    private static AuxVariableInfo[] loadVariableInfos(AuxFileInfo fileInfo, BufferedReader r) throws IOException {
        final List variableInfoList = new ArrayList();
        AuxVariableInfo variableInfo = null;
        StringTokenizer st;
        int dataIndex = 0;
        while (true) {
            String line = r.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.startsWith("#")) {
                continue;
            }

            if (dataIndex == 0) {

                st = new StringTokenizer(line);
                final String varId = st.nextToken();
                final char datasetId = varId.charAt(1);
                final AuxDatasetInfo datasetInfo = fileInfo.getDatasetInfo(datasetId);
                variableInfo = new AuxVariableInfo(datasetInfo);
                variableInfo.setId(varId);
                variableInfo.setOffset(Integer.parseInt(st.nextToken()));
                variableInfo.setScale(Double.parseDouble(st.nextToken()));
                variableInfo.setDataType(getProductDataType(Integer.parseInt(st.nextToken())));
                variableInfo.setDim1(Integer.parseInt(st.nextToken()));
                variableInfo.setDim2(Integer.parseInt(st.nextToken()));
                variableInfo.setDim3(Integer.parseInt(st.nextToken()));
                variableInfo.setElementSize(Integer.parseInt(st.nextToken()));
                variableInfo.setEditFlag(Integer.parseInt(st.nextToken()));
                variableInfo.setEditType(Integer.parseInt(st.nextToken()));
                variableInfo.setDisplayType(Integer.parseInt(st.nextToken()));
                if (st.hasMoreTokens()) {
                    final String range = st.nextToken();
                    variableInfo.setRange(range.equals("-") ? null : range);
                }
                variableInfoList.add(variableInfo);
            } else if (dataIndex == 1) {
                variableInfo.setComment(line.equals("-") ? null : line);
            } else if (dataIndex == 2) {
                variableInfo.setUnit(line.equals("-") ? null : line);
            } else /*if (dataIndex == 3)*/ {
                dataIndex = -1; // Empty line
            }

            dataIndex++;
        }

        final AuxVariableInfo[] variableDescriptors = new AuxVariableInfo[variableInfoList.size()];
        variableInfoList.toArray(variableDescriptors);
        return variableDescriptors;
    }

    private static InputStream getResourceAsStream(final String resourceName) {
        return AuxDatabase.class.getResourceAsStream(resourceName);
    }

    private static int getProductDataType(final int varDataType) {

        /* type index possible values */
        final int STRING = 0;   // Java: String
        final int SI_CHAR = 1;  // Java: byte
        final int SI_SHORT = 2;  // Java: short
        final int SI_LONG = 3;  // Java: int
        final int F_FLOAT = 4; // Java: float
        final int F_DOUBLE = 5; // Java: double
        final int UN_CHAR = 10;  // Java: unsigned byte --> short
        final int UN_SHORT = 20; // Java: unsigned short --> int
        final int UN_LONG = 30;  // Java: unsigned int --> long
        final int MJD = 90; // Java: Date

        if (varDataType == UN_CHAR) {
            return ProductData.TYPE_UINT8;
        } else if (varDataType == SI_CHAR) {
            return ProductData.TYPE_INT8;
        } else if (varDataType == UN_SHORT) {
            return ProductData.TYPE_UINT16;
        } else if (varDataType == SI_SHORT) {
            return ProductData.TYPE_INT16;
        } else if (varDataType == UN_LONG) {
            return ProductData.TYPE_UINT32;
        } else if (varDataType == SI_LONG) {
            return ProductData.TYPE_INT32;
        } else if (varDataType == F_FLOAT) {
            return ProductData.TYPE_FLOAT32;
        } else if (varDataType == F_DOUBLE) {
            return ProductData.TYPE_FLOAT64;
        } else if (varDataType == MJD) {
            return ProductData.TYPE_UTC;
        } else if (varDataType == STRING) {
            return ProductData.TYPE_ASCII;
        }
        throw new IllegalArgumentException("illegal varDataType: " + varDataType);
    }

      // currently not used
//    private static class Dumper {
//
//        private PrintWriter _w;
//
//        public Dumper(String filepath) {
//            try {
//                _w = new PrintWriter(new FileWriter(filepath));
//            } catch (IOException e) {
//                e.printStackTrace();  //To change body of catch statement use Options | File Templates.
//                _w = new PrintWriter(System.out);
//            }
//        }
//
//        public void close() {
//            _w.close();
//        }
//
//        public void dumpVariableInfoHeader() {
//            _w.print("Database ID|");
//            _w.print("Database Name|");
//            _w.print("Descriptive Name|");
//            _w.print("Variable ID|");
//            _w.print("Data Type|");
//            _w.print("Dim 1|");
//            _w.print("Dim 2|");
//            _w.print("Dim 3|");
//            _w.print("Scale|");
//            _w.print("Unit|");
//            _w.print("Range|");
//            _w.println();
//        }
//
//        public void dumpVariableInfos(final AuxFileInfo databaseInfo, final AuxVariableInfo[] variableInfos) {
//            for (int j = 0; j < variableInfos.length; j++) {
//                AuxVariableInfo variableInfo = variableInfos[j];
//                _w.print(databaseInfo.getTypeId() + "|");
//                _w.print(databaseInfo.getDirName() + "|");
//                _w.print(variableInfo.getComment() + "|");
//                _w.print(variableInfo.getId() + "|");
//                _w.print(ProductData.getTypeString(variableInfo.getDataType()) + "|");
//                _w.print(variableInfo.getDim1() + "|");
//                _w.print(variableInfo.getDim2() + "|");
//                _w.print(variableInfo.getDim3() + "|");
//                _w.print(variableInfo.getScale() + "|");
//                _w.print(variableInfo.getUnit() + "|");
//                _w.print(variableInfo.getRange() + "|");
//                _w.println();
//            }
//        }
//    }
}

