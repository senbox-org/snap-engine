/*
 * $Id: AuxDatabaseTest.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class AuxDatabaseTest {

    @Test
    public void testDatabaseInfos() {
        AuxFileInfo fi;

        assertEquals(30, AuxDatabase.getInstance().getFileInfoCount());

        /////////////////////////////////////////////////////////////////////
        // '0', index 0, l0_rr

        fi = AuxDatabase.getInstance().getFileInfo(0);
        assertNotNull(fi);
        assertSame(fi, AuxDatabase.getInstance().getFileInfo('0'));
        assertEquals('0', fi.getTypeId());
        assertEquals(false, fi.isEditable());
        assertEquals(true, fi.isImport());
        assertEquals("l0_rr", fi.getDirName());
        assertNotNull(fi.getDescription());
        assertEquals(3, fi.getDatasetCount());

        /////////////////////////////////////////////////////////////////////
        // 'D', index 7, procl1par - this isFlagSet was added in order to isFlagSet
        // the different table IDs

        fi = AuxDatabase.getInstance().getFileInfo(7);
        assertNotNull(fi);
        assertSame(fi, AuxDatabase.getInstance().getFileInfo('D'));
        assertEquals('D', fi.getTypeId());
        assertEquals(true, fi.isEditable());
        assertEquals(true, fi.isImport());
        assertEquals("procl1par", fi.getDirName());
        assertNotNull(fi.getDescription());
        assertEquals(15, fi.getDatasetCount());

        /////////////////////////////////////////////////////////////////////
        // 'T', index 23, case1

        fi = AuxDatabase.getInstance().getFileInfo(23);
        assertNotNull(fi);
        assertSame(fi, AuxDatabase.getInstance().getFileInfo('T'));
        assertEquals('T', fi.getTypeId());
        assertEquals(true, fi.isEditable());
        assertEquals(true, fi.isImport());
        assertEquals("case1", fi.getDirName());
        assertNotNull(fi.getDescription());
        assertEquals(9, fi.getDatasetCount());

        /////////////////////////////////////////////////////////////////////
        // 'U', index 24, case2

        fi = AuxDatabase.getInstance().getFileInfo(24);
        assertNotNull(fi);
        assertSame(fi, AuxDatabase.getInstance().getFileInfo('U'));
        assertEquals('U', fi.getTypeId());
        assertEquals(true, fi.isEditable());
        assertEquals(true, fi.isImport());
        assertEquals("case2", fi.getDirName());
        assertNotNull(fi.getDescription());
        assertEquals(8, fi.getDatasetCount());

        /////////////////////////////////////////////////////////////////////
        // 'Z', index 29, windv

        fi = AuxDatabase.getInstance().getFileInfo(29);
        assertNotNull(fi);
        assertSame(fi, AuxDatabase.getInstance().getFileInfo('Z'));
        assertEquals('Z', fi.getTypeId());
        assertEquals(false, fi.isEditable());
        assertEquals(true, fi.isImport());
        assertEquals("windv", fi.getDirName());
        assertNotNull(fi.getDescription());
        assertEquals(1, fi.getDatasetCount());
    }

    @Test
    public void testDatasetInfos() {
        AuxFileInfo fi;

        assertEquals(30, AuxDatabase.getInstance().getFileInfoCount());

        /////////////////////////////////////////////////////////////////////
        // '0', index 0, l0_rr

        fi = AuxDatabase.getInstance().getFileInfo('0');
        assertNotNull(fi);
        assertEquals(3, fi.getDatasetCount());

        for (int i = 0; i < fi.getDatasetCount(); i++) {
            assertSame(fi, fi.getDatasetInfo(i).getFileInfo());
        }

        assertEquals('0', fi.getDatasetInfo(0).getId());
        assertEquals('1', fi.getDatasetInfo(1).getId());
        assertEquals('2', fi.getDatasetInfo(2).getId());

        assertEquals(0, fi.getDatasetInfo(0).getType());
        assertEquals(1, fi.getDatasetInfo(1).getType());
        assertEquals(3, fi.getDatasetInfo(2).getType());

        assertEquals(1247, fi.getDatasetInfo(0).getRecordSize());
        assertEquals(1956, fi.getDatasetInfo(1).getRecordSize());
        assertEquals(2174, fi.getDatasetInfo(2).getRecordSize());

        assertEquals(null, fi.getDatasetInfo(0).getVarIdForNumRecords());
        assertEquals(null, fi.getDatasetInfo(1).getVarIdForNumRecords());
        assertEquals("010U", fi.getDatasetInfo(2).getVarIdForNumRecords());

        assertEquals("MPH", fi.getDatasetInfo(0).getName());
        assertEquals("SPH", fi.getDatasetInfo(1).getName());
        assertEquals("MERIS_SOURCE_PACKETS", fi.getDatasetInfo(2).getName());

        /////////////////////////////////////////////////////////////////////
        // 'D', index 7, procl1par - this isFlagSet was added in order to isFlagSet
        // the different table IDs

        fi = AuxDatabase.getInstance().getFileInfo('D');
        assertNotNull(fi);
        assertEquals(15, fi.getDatasetCount());

        for (int i = 0; i < fi.getDatasetCount(); i++) {
            assertSame(fi, fi.getDatasetInfo(i).getFileInfo());
        }

        assertEquals('0', fi.getDatasetInfo(0).getId());
        assertEquals('1', fi.getDatasetInfo(1).getId());
        assertEquals('2', fi.getDatasetInfo(2).getId());
        assertEquals('4', fi.getDatasetInfo(3).getId());
        assertEquals('6', fi.getDatasetInfo(4).getId());
        assertEquals('7', fi.getDatasetInfo(5).getId());
        assertEquals('8', fi.getDatasetInfo(6).getId());
        assertEquals('9', fi.getDatasetInfo(7).getId());
        assertEquals('A', fi.getDatasetInfo(8).getId());
        assertEquals('B', fi.getDatasetInfo(9).getId());
        assertEquals('C', fi.getDatasetInfo(10).getId());
        assertEquals('D', fi.getDatasetInfo(11).getId());
        assertEquals('E', fi.getDatasetInfo(12).getId());
        assertEquals('I', fi.getDatasetInfo(13).getId());
        assertEquals('J', fi.getDatasetInfo(14).getId());

        /////////////////////////////////////////////////////////////////////
        // 'T', index 23, case1

        fi = AuxDatabase.getInstance().getFileInfo('T');
        assertNotNull(fi);
        assertEquals(9, fi.getDatasetCount());

        for (int i = 0; i < fi.getDatasetCount(); i++) {
            assertSame(fi, fi.getDatasetInfo(i).getFileInfo());
        }

        assertEquals('0', fi.getDatasetInfo(0).getId());
        assertEquals('1', fi.getDatasetInfo(1).getId());
        assertEquals('2', fi.getDatasetInfo(2).getId());
        assertEquals('3', fi.getDatasetInfo(3).getId());
        assertEquals('4', fi.getDatasetInfo(4).getId());
        assertEquals('5', fi.getDatasetInfo(5).getId());
        assertEquals('6', fi.getDatasetInfo(6).getId());
        assertEquals('7', fi.getDatasetInfo(7).getId());
        assertEquals('8', fi.getDatasetInfo(8).getId());

        assertEquals(0, fi.getDatasetInfo(0).getType());
        assertEquals(1, fi.getDatasetInfo(1).getType());
        assertEquals(2, fi.getDatasetInfo(2).getType());
        assertEquals(2, fi.getDatasetInfo(3).getType());
        assertEquals(2, fi.getDatasetInfo(4).getType());
        assertEquals(2, fi.getDatasetInfo(5).getType());
        assertEquals(4, fi.getDatasetInfo(6).getType());
        assertEquals(4, fi.getDatasetInfo(7).getType());
        assertEquals(4, fi.getDatasetInfo(8).getType());

        assertEquals(1247, fi.getDatasetInfo(0).getRecordSize());
        assertEquals(2058, fi.getDatasetInfo(1).getRecordSize());
        assertEquals(12977, fi.getDatasetInfo(2).getRecordSize());
        assertEquals(304, fi.getDatasetInfo(3).getRecordSize());
        assertEquals(51336, fi.getDatasetInfo(4).getRecordSize());
        assertEquals(76, fi.getDatasetInfo(5).getRecordSize());
        assertEquals(131040, fi.getDatasetInfo(6).getRecordSize());
        assertEquals(66500, fi.getDatasetInfo(7).getRecordSize());
        assertEquals(4194304, fi.getDatasetInfo(8).getRecordSize());

        assertEquals(null, fi.getDatasetInfo(0).getVarIdForNumRecords());
        assertEquals(null, fi.getDatasetInfo(1).getVarIdForNumRecords());
        assertEquals(null, fi.getDatasetInfo(2).getVarIdForNumRecords());
        assertEquals(null, fi.getDatasetInfo(3).getVarIdForNumRecords());
        assertEquals(null, fi.getDatasetInfo(4).getVarIdForNumRecords());
        assertEquals(null, fi.getDatasetInfo(5).getVarIdForNumRecords());
        assertEquals("T112", fi.getDatasetInfo(6).getVarIdForNumRecords());
        assertEquals("T11A", fi.getDatasetInfo(7).getVarIdForNumRecords());
        assertEquals("T11I", fi.getDatasetInfo(8).getVarIdForNumRecords());

        assertEquals("MPH", fi.getDatasetInfo(0).getName());
        assertEquals("SPH", fi.getDatasetInfo(1).getName());
        assertEquals("GADS General", fi.getDatasetInfo(2).getName());
        assertEquals("GADS Geometrical Factor", fi.getDatasetInfo(3).getName());
        assertEquals("GADS Thresholds", fi.getDatasetInfo(4).getName());
        assertEquals("GADS Log10 Polynomial Coeff.", fi.getDatasetInfo(5).getName());
        assertEquals("ADS F1/Q Factor", fi.getDatasetInfo(6).getName());
        assertEquals("ADS Glint Reflectance", fi.getDatasetInfo(7).getName());
        assertEquals("ADS 510nm mean water leaving reflectance", fi.getDatasetInfo(8).getName());

        /////////////////////////////////////////////////////////////////////
        // 'U', index 24, case2

        fi = AuxDatabase.getInstance().getFileInfo('U');
        assertNotNull(fi);
        assertEquals(8, fi.getDatasetCount());

        for (int i = 0; i < fi.getDatasetCount(); i++) {
            assertSame(fi, fi.getDatasetInfo(i).getFileInfo());
        }

        assertEquals('0', fi.getDatasetInfo(0).getId());
        assertEquals('1', fi.getDatasetInfo(1).getId());
        assertEquals('2', fi.getDatasetInfo(2).getId());
        assertEquals('3', fi.getDatasetInfo(3).getId());
        assertEquals('4', fi.getDatasetInfo(4).getId());
        assertEquals('5', fi.getDatasetInfo(5).getId());
        assertEquals('6', fi.getDatasetInfo(6).getId());
        assertEquals('7', fi.getDatasetInfo(7).getId());

        assertEquals(0, fi.getDatasetInfo(0).getType());
        assertEquals(1, fi.getDatasetInfo(1).getType());
        assertEquals(2, fi.getDatasetInfo(2).getType());
        assertEquals(2, fi.getDatasetInfo(3).getType());
        assertEquals(2, fi.getDatasetInfo(4).getType());
        assertEquals(2, fi.getDatasetInfo(5).getType());
        assertEquals(4, fi.getDatasetInfo(6).getType());
        assertEquals(2, fi.getDatasetInfo(7).getType());

        assertEquals(1247, fi.getDatasetInfo(0).getRecordSize());
        assertEquals(1778, fi.getDatasetInfo(1).getRecordSize());
        assertEquals(1087, fi.getDatasetInfo(2).getRecordSize());
        assertEquals(56, fi.getDatasetInfo(3).getRecordSize());
        assertEquals(197600, fi.getDatasetInfo(4).getRecordSize());
        assertEquals(8200, fi.getDatasetInfo(5).getRecordSize());
        assertEquals(194560, fi.getDatasetInfo(6).getRecordSize());
        assertEquals(262144, fi.getDatasetInfo(7).getRecordSize());

        assertEquals(null, fi.getDatasetInfo(0).getVarIdForNumRecords());
        assertEquals(null, fi.getDatasetInfo(1).getVarIdForNumRecords());
        assertEquals(null, fi.getDatasetInfo(2).getVarIdForNumRecords());
        assertEquals(null, fi.getDatasetInfo(3).getVarIdForNumRecords());
        assertEquals(null, fi.getDatasetInfo(4).getVarIdForNumRecords());
        assertEquals(null, fi.getDatasetInfo(5).getVarIdForNumRecords());
        assertEquals("U112", fi.getDatasetInfo(6).getVarIdForNumRecords());
        assertEquals(null, fi.getDatasetInfo(7).getVarIdForNumRecords());

        assertEquals("MPH", fi.getDatasetInfo(0).getName());
        assertEquals("SPH", fi.getDatasetInfo(1).getName());
        assertEquals("GADS General", fi.getDatasetInfo(2).getName());
        assertEquals("GADS Case 2 YS Det. Coeff.", fi.getDatasetInfo(3).getName());
        assertEquals("GADS Anom. Scatt. Detection", fi.getDatasetInfo(4).getName());
        assertEquals("GADS IOP v/ Geochem. Var.", fi.getDatasetInfo(5).getName());
        assertEquals("ADS  Reflectance v/ IOP", fi.getDatasetInfo(6).getName());
        assertEquals("GADS Neural Network", fi.getDatasetInfo(7).getName());

        /////////////////////////////////////////////////////////////////////
        // 'Z', index 29, windv

        fi = AuxDatabase.getInstance().getFileInfo('Z');
        assertNotNull(fi);
        assertEquals(1, fi.getDatasetCount());

        for (int i = 0; i < fi.getDatasetCount(); i++) {
            assertSame(fi, fi.getDatasetInfo(i).getFileInfo());
        }

        assertEquals('0', fi.getDatasetInfo(0).getId());

        assertEquals(2, fi.getDatasetInfo(0).getType());

        assertEquals(130440, fi.getDatasetInfo(0).getRecordSize());

        assertEquals(null, fi.getDatasetInfo(0).getVarIdForNumRecords());

        assertEquals("winds GADS", fi.getDatasetInfo(0).getName());
    }

    @Test
    public void testSomeVariableInfosForT() {
        AuxFileInfo fi;
        AuxVariableInfo vi;

        fi = AuxDatabase.getInstance().getFileInfo('T');
        assertNotNull(fi);

        //    T200       0 1.000000e+00 10      4      0      0       4  1   1  1 -
        //    wind speed tabulated values for GADS - Geometrical factor R
        //    m.s-1
        vi = fi.getVariableInfo("T200");
        assertNotNull(vi);
        assertSame(fi.getDatasetInfo('2'), vi.getDatasetInfo());
        assertSame(fi, vi.getFileInfo());
        assertEquals("T200", vi.getId());
        assertEquals(0, vi.getOffset());
        assertEquals(1.0, vi.getScale(), 1e-10);
        assertEquals(4, vi.getNumBytes());
        assertEquals(ProductData.TYPE_UINT8, vi.getDataType());
        assertEquals(4, vi.getDim1());
        assertEquals(0, vi.getDim2());
        assertEquals(0, vi.getDim3());
        assertEquals(1, vi.getEditFlag());
        assertEquals(1, vi.getEditType());
        assertEquals(1, vi.getDisplayType());
        assertEquals(null, vi.getRange());
        assertNotNull(vi.getComment());
        assertEquals("m.s-1", vi.getUnit());

        //    T201       4 1.000000e+00  4      9      0      0      36  1   2  1 -
        //    l tabulated values for ADS f1/Q
        //    nm
        vi = fi.getVariableInfo("T201");
        assertNotNull(vi);
        assertSame(fi.getDatasetInfo('2'), vi.getDatasetInfo());
        assertSame(fi, vi.getFileInfo());
        assertEquals("T201", vi.getId());
        assertEquals(4, vi.getOffset());
        assertEquals(1.0, vi.getScale(), 1e-10);
        assertEquals(ProductData.TYPE_FLOAT32, vi.getDataType());
        assertEquals(9, vi.getDim1());
        assertEquals(0, vi.getDim2());
        assertEquals(0, vi.getDim3());
        assertEquals(36, vi.getNumBytes());
        assertEquals(1, vi.getEditFlag());
        assertEquals(1, vi.getEditType());
        assertEquals(1, vi.getDisplayType());
        assertEquals(null, vi.getRange());
        assertNotNull(vi.getComment());
        assertEquals("nm", vi.getUnit());

        //    T202      40 1.000000e-06 30      6      0      0      24  1   1  1 -
        //    qs tabulated values for ADS f1/Q
        //    deg
        vi = fi.getVariableInfo("T202");
        assertNotNull(vi);
        assertSame(fi.getDatasetInfo('2'), vi.getDatasetInfo());
        assertSame(fi, vi.getFileInfo());
        assertEquals("T202", vi.getId());
        assertEquals(40, vi.getOffset());
        assertEquals(1e-6, vi.getScale(), 1e-10);
        assertEquals(ProductData.TYPE_UINT32, vi.getDataType());
        assertEquals(6, vi.getDim1());
        assertEquals(0, vi.getDim2());
        assertEquals(0, vi.getDim3());
        assertEquals(24, vi.getNumBytes());
        assertEquals(1, vi.getEditFlag());
        assertEquals(1, vi.getEditType());
        assertEquals(1, vi.getDisplayType());
        assertEquals(null, vi.getRange());
        assertNotNull(vi.getComment());
        assertEquals("deg", vi.getUnit());

        //    T601   32760 1.000000e+00  4      9    182      5   32760  1   2  1 -
        //    fl/Q factor( l , qv x Df , Chl ) for tA 2 & wind 1
        //    dl
        vi = fi.getVariableInfo("T601");
        assertNotNull(vi);
        assertSame(fi.getDatasetInfo('6'), vi.getDatasetInfo());
        assertSame(fi, vi.getFileInfo());
        assertEquals("T601", vi.getId());
        assertEquals(32760, vi.getOffset());
        assertEquals(1.0, vi.getScale(), 1e-10);
        assertEquals(ProductData.TYPE_FLOAT32, vi.getDataType());
        assertEquals(9, vi.getDim1());
        assertEquals(182, vi.getDim2());
        assertEquals(5, vi.getDim3());
        assertEquals(32760, vi.getNumBytes());
        assertEquals(1, vi.getEditFlag());
        assertEquals(1, vi.getEditType());
        assertEquals(1, vi.getDisplayType());
        assertEquals(null, vi.getRange());
        assertNotNull(vi.getComment());
        assertEquals("dl", vi.getUnit());
    }

    @Test
    public void testSomeVariableInfosForW() {
        AuxFileInfo fi;
        AuxVariableInfo vi;

        fi = AuxDatabase.getInstance().getFileInfo('W');
        assertNotNull(fi);

        //    W000       0 1.000000e+00  0     62      0      0      62  0   0  1 -
        //    Product file name - V. 20
        //    -
        vi = fi.getVariableInfo("W000");
        assertNotNull(vi);
        assertSame(fi.getDatasetInfo('0'), vi.getDatasetInfo());
        assertSame(fi, vi.getFileInfo());
        assertEquals("W000", vi.getId());
        assertEquals(0, vi.getOffset());
        assertEquals(1.0, vi.getScale(), 1e-10);
        assertEquals(ProductData.TYPE_ASCII, vi.getDataType());
        assertEquals(62, vi.getDim1());
        assertEquals(0, vi.getDim2());
        assertEquals(0, vi.getDim3());
        assertEquals(62, vi.getNumBytes());
        assertEquals(0, vi.getEditFlag());
        assertEquals(0, vi.getEditType());
        assertEquals(1, vi.getDisplayType());
        assertEquals(null, vi.getRange());
        assertNotNull(vi.getComment());
        assertEquals(null, vi.getUnit());

        //    W106     305 1.000000e+00  0     11      0      0      11  0   0  1 -
        //    Number of Data Set Records
        //    -
        vi = fi.getVariableInfo("W106");
        assertNotNull(vi);
        assertSame(fi.getDatasetInfo('1'), vi.getDatasetInfo());
        assertSame(fi, vi.getFileInfo());
        assertEquals("W106", vi.getId());
        assertEquals(305, vi.getOffset());
        assertEquals(1.0, vi.getScale(), 1e-10);
        assertEquals(ProductData.TYPE_ASCII, vi.getDataType());
        assertEquals(11, vi.getDim1());
        assertEquals(0, vi.getDim2());
        assertEquals(0, vi.getDim3());
        assertEquals(11, vi.getNumBytes());
        assertEquals(0, vi.getEditFlag());
        assertEquals(0, vi.getEditType());
        assertEquals(0, vi.getDisplayType());
        assertEquals(null, vi.getRange());
        assertNotNull(vi.getComment());
        assertEquals(null, vi.getUnit());

        //    W205      27 1.000000e+00  4      3      0      0      12  1   2  1 -
        //    rho_i parameters for blue, red and near infrared channels for TOAVI computation
        //    dl
        vi = fi.getVariableInfo("W205");
        assertNotNull(vi);
        assertEquals("W205", vi.getId());
        assertSame(fi.getDatasetInfo('2'), vi.getDatasetInfo());
        assertSame(fi, vi.getFileInfo());
        assertEquals(27, vi.getOffset());
        assertEquals(1.0, vi.getScale(), 1e-10);
        assertEquals(ProductData.TYPE_FLOAT32, vi.getDataType());
        assertEquals(3, vi.getDim1());
        assertEquals(0, vi.getDim2());
        assertEquals(0, vi.getDim3());
        assertEquals(12, vi.getNumBytes());
        assertEquals(1, vi.getEditFlag());
        assertEquals(1, vi.getEditType());
        assertEquals(1, vi.getDisplayType());
        assertEquals(null, vi.getRange());
        assertNotNull(vi.getComment());
        assertEquals("dl", vi.getUnit());

        //    W207      51 1.000000e+00  4      3     12      0     144  1   2  1 -
        //    toavi polynomial coefficients for blue, red and near infrared channels for TOAVI computation
        //    dl
        vi = fi.getVariableInfo("W207");
        assertNotNull(vi);
        assertEquals("W207", vi.getId());
        assertSame(fi.getDatasetInfo('2'), vi.getDatasetInfo());
        assertSame(fi, vi.getFileInfo());
        assertEquals(51, vi.getOffset());
        assertEquals(1.0, vi.getScale(), 1e-10);
        assertEquals(ProductData.TYPE_FLOAT32, vi.getDataType());
        assertEquals(3, vi.getDim1());
        assertEquals(12, vi.getDim2());
        assertEquals(0, vi.getDim3());
        assertEquals(144, vi.getNumBytes());
//        assertEquals(144, vi.getDatasetInfo().getRecordSize());
        assertEquals(1, vi.getEditFlag());
        assertEquals(1, vi.getEditType());
        assertEquals(1, vi.getDisplayType());
        assertEquals(null, vi.getRange());
        assertNotNull(vi.getComment());
        assertEquals("dl", vi.getUnit());
    }
}

