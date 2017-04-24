/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.dataio.landsat.geotiff;

import org.junit.Test;

import static org.junit.Assert.*;

public class LandsatTypeInfoTest {

    @Test
    public void testIsLandsat() throws Exception {
        assertTrue(LandsatTypeInfo.isLandsat("LE07_L1TP_016039_20040918_20160211_01_T1_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsat("LM11870291976166ESA00_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsat("LT40140341983030XXX13_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsat("LT41730601990155XXX01.tar.bz"));
        assertTrue(LandsatTypeInfo.isLandsat("LT51231232013068GSI01_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsat("LT51940252011320KIS00.tar.gz"));
        assertTrue(LandsatTypeInfo.isLandsat("LE71890342011277ASN00_MTL.TXT"));
        assertTrue(LandsatTypeInfo.isLandsat("LE71890342011277ASN00.tar.gz"));
        assertTrue(LandsatTypeInfo.isLandsat("LO82160332013191LGN00.tar.gz"));
        assertTrue(LandsatTypeInfo.isLandsat("LT82270322013068LGN01_MTL.TXT"));
    }

    @Test
    public void testIsLandsatCollectionFilename() throws Exception {
        assertTrue(LandsatTypeInfo.isLandsatCollection("LE07_L1TP_016039_20040918_20160211_01_T1_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsatCollection("LT04_L1GS_017036_19821115_20160315_01_T2_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsatCollection("LE07_L1GT_037035_20160314_20160314_01_RT_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsatCollection("LT05_L1TP_202026_20031017_20161203_01_T1_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsatCollection("LC08_L1TP_194028_20160622_20170323_01_T1_MTL.txt"));

        assertFalse(LandsatTypeInfo.isLandsatCollection("LT82270322013068LGN01_MTL.txt"));
    }

    @Test
    public void testIsLandsatMSSFilename() throws Exception {
        assertTrue(LandsatTypeInfo.isMss("LM11870291976166ESA00_MTL.txt"));
        assertTrue(LandsatTypeInfo.isMss("LM32170241982254XXX01_MTL.txt"));
        assertTrue(LandsatTypeInfo.isMss("LM42310081982267ESA00_MTL.txt"));
        assertTrue(LandsatTypeInfo.isMss("LM52010241984295AAA03_MTL.txt"));

        assertFalse(LandsatTypeInfo.isMss("LT40140341983030XXX13_MTL.txt"));
    }

    @Test
    public void testIsLandsat4Filename() throws Exception {
        assertTrue(LandsatTypeInfo.isLandsat4("LT40140341983030XXX13_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsat4("LT40140341982315PAC00_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsat4("LT41160361989137XXX02_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsat4("LT41730601990155XXX01.tar.bz")); // 'tar.bz' expected as extension
        assertTrue(LandsatTypeInfo.isLandsat4("LT41930241992221XXX02.tar.gz")); // 'tar.gz' expected as extension

        assertFalse(LandsatTypeInfo.isLandsat4("LT40140341982315PAC00_B1.TIF"));
        assertFalse(LandsatTypeInfo.isLandsat4("LT51920342011274MPS00.tgz"));
    }

    @Test
    public void testIsLandsat5Filename() throws Exception {
        assertTrue(LandsatTypeInfo.isLandsat5("LT51231232013068GSI01_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsat5("LT51231232013068GSI01_MTL.TXT"));
        assertTrue(LandsatTypeInfo.isLandsat5("LT51920342011274MPS00.tar.gz"));
        assertTrue(LandsatTypeInfo.isLandsat5("LT51920342011274MPS00.tgz")); // 'tar.gz' expected as extension
        assertTrue(LandsatTypeInfo.isLandsat5("LT51700572011312MLK00.tar.bz")); // 'tar.bz' expected as extension
        assertTrue(LandsatTypeInfo.isLandsat5("LT51940252011320KIS00.tar.gz")); // 'tar.gz' expected as extension
        assertTrue(LandsatTypeInfo.isLandsat5("LT51970241984299FUI00.ZIP")); // 'zip' expected as extension

        assertFalse(LandsatTypeInfo.isLandsat5("LT51231232013068GSI01_B3.txt")); // is a band name, not the metadata file
        assertFalse(LandsatTypeInfo.isLandsat5("L5196030_03020031023_MTL.txt"));  // Sensor type missing
        assertFalse(LandsatTypeInfo.isLandsat5("LT71920342011274MPS00.tar.gz")); // '5' expected after 'LT'
    }

    @Test
    public void testIsLandsat7Filename() throws Exception {
        assertTrue(LandsatTypeInfo.isLandsat7("LE71890342011277ASN00_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsat7("LE71890342011277ASN00_MTL.TXT"));
        assertTrue(LandsatTypeInfo.isLandsat7("LE71890342011277ASN00.tar.gz"));
        assertTrue(LandsatTypeInfo.isLandsat7("LE71890342011277ASN00.tgz")); // 'tar.gz' expected as extension
        assertTrue(LandsatTypeInfo.isLandsat7("LE71710602000329EDC00.tar.bz")); // 'tar.bz' expected as extension
        assertTrue(LandsatTypeInfo.isLandsat7("LE71920252000332NSG00.ZIP")); // 'zip' expected as extension
        assertTrue(LandsatTypeInfo.isLandsat7("LE71940272000330EDC00.tar.gz"));

        assertFalse(LandsatTypeInfo.isLandsat7("LE71890342011277ASN00_B3.txt")); // is a band name, not the metadata file
        assertFalse(LandsatTypeInfo.isLandsat7("L71890342011277ASN00.txt"));  // Sensor type missing
        assertFalse(LandsatTypeInfo.isLandsat7("LE81890342011277ASN00_MTL.txt")); // '7' expected after 'LT'
    }

    @Test
    public void testIsLandsat8Filename() throws Exception {
        assertTrue(LandsatTypeInfo.isLandsat8("LT82270322013068LGN01_MTL.txt"));
        assertTrue(LandsatTypeInfo.isLandsat8("LT82270322013068LGN01_MTL.TXT"));
        assertTrue(LandsatTypeInfo.isLandsat8("LC82160332013191LGN00.tar.gz"));
        assertTrue(LandsatTypeInfo.isLandsat8("LO82160332013191LGN00.tar.gz"));
        assertTrue(LandsatTypeInfo.isLandsat8("LT82160332013191LGN00.tar.gz"));
        assertTrue(LandsatTypeInfo.isLandsat8("LT82160332013191LGN00.tgz"));
        assertTrue(LandsatTypeInfo.isLandsat8("LC81970232013266LGN00.tar.bz")); // 'tar.bz' expected as extension

        assertFalse(LandsatTypeInfo.isLandsat8("L8196030_03020031023_MTL.txt"));  // Sensor type missing
        assertFalse(LandsatTypeInfo.isLandsat8("LT52160332013191LGN00.tar.gz")); // '8' expected after 'LT'
    }

    @Test
    public void testIsLandsat5LegacyFilename() throws Exception {
        assertTrue(LandsatTypeInfo.isLandsat5Legacy("LT51960300302003GSI01_MTL.txt")); //according to specification
        assertTrue(LandsatTypeInfo.isLandsat5Legacy("LT51960300302003GSI01_MTL.TXT"));

        assertTrue(LandsatTypeInfo.isLandsat5Legacy("L5196030_03020031023_MTL.txt")); //according to real-world data
        assertTrue(LandsatTypeInfo.isLandsat5Legacy("L5196030_03020031023_MTL.TXT"));

        assertTrue(LandsatTypeInfo.isLandsat5Legacy("LT51960302003296MTI01.tar.gz"));

        assertFalse(LandsatTypeInfo.isLandsat5Legacy("L51950302003257MTI01.tar.gz"));  // Sensor type missing
        assertFalse(LandsatTypeInfo.isLandsat5Legacy("LT72160332013191LGN00.tar.gz")); // '5' expected after 'LT'
        assertFalse(LandsatTypeInfo.isLandsat5Legacy("LT82160332013191LGN00.tgz")); // 'tar.gz' or 'txt' expected as extension
        assertFalse(LandsatTypeInfo.isLandsat5Legacy("LT82160332013191LGN00.dat")); // 'tar.gz' or 'txt' expected as extension
    }

    @Test
    public void testIsLandsat7LegacyFilename() throws Exception {
        assertTrue(LandsatTypeInfo.isLandsat7Legacy("LE71960300302003GSI01_MTL.txt")); //according to specification
        assertTrue(LandsatTypeInfo.isLandsat7Legacy("LE71960300302003GSI01_MTL.TXT"));

        assertTrue(LandsatTypeInfo.isLandsat7Legacy("L71196030_03020031023_MTL.txt")); //according to real-world data
        assertTrue(LandsatTypeInfo.isLandsat7Legacy("L71196030_03020031023_MTL.TXT"));

        assertTrue(LandsatTypeInfo.isLandsat7Legacy("LE71960302003296ASN01.tar.gz"));
        assertTrue(LandsatTypeInfo.isLandsat7Legacy("LE72160332013191LGN00.tgz"));

        assertFalse(LandsatTypeInfo.isLandsat7Legacy("L71950302003257MTI01.tar.gz"));  // Sensor type missing
        assertFalse(LandsatTypeInfo.isLandsat7Legacy("LE52160332013191LGN00.tar.gz")); // '7' expected after 'LT'
        assertFalse(LandsatTypeInfo.isLandsat7Legacy("LE72160332013191LGN00.dat")); // 'tar.gz' or 'txt' expected as extension
    }

}
