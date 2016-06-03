/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.smilecorr;

import org.esa.snap.core.util.SystemUtils;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author muhammad.bc.
 */
public class SmileCorrectionAuxdataTest {


    @Test
    public void testAuxDataIsInstall() throws Exception {
        SmileCorrectionAuxdata.installAuxdata();
        Path auxDataPath = SystemUtils.getAuxDataPath().resolve("olci/smile-correction");
        List<Path> collect = Files.list(auxDataPath).collect(Collectors.toList());

        assertTrue(auxDataPath.isAbsolute());
        assertTrue(collect.stream().anyMatch(path -> path.getFileName().toString().equals("reflconfig.txt")));
        assertTrue(collect.stream().anyMatch(path -> path.getFileName().toString().equals("band_value.txt")));
    }

    @Test
    public void testAuxDataAreLoadedInFlatTable() throws Exception {
        List<String[]> loadAuxdata = SmileCorrectionAuxdata.loadAuxdata();
        double[][] auxDataInFlatTable = SmileCorrectionAuxdata.auxDataInFlatTable(loadAuxdata);
        assertEquals(22, auxDataInFlatTable.length);
        assertEquals(9, auxDataInFlatTable[0].length);

        assertEquals(1, auxDataInFlatTable[0][0], 1e-8);
        assertEquals(1441.8, auxDataInFlatTable[0][8], 1e-8);

        assertEquals(6.0, auxDataInFlatTable[5][0], 1e-8);
        assertEquals(1804.4, auxDataInFlatTable[5][8], 1e-8);
    }

    @Test
    public void testAuxDataValue() {
        SmileCorrectionAuxdata smileCorrectionAuxdata = new SmileCorrectionAuxdata();

        double[] expectedBands = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 0.0};
        assertArrayEquals(expectedBands, smileCorrectionAuxdata.getBands(), 1e-8);

        double[] expectedWaterLower = {1, 1, 2, 3, 4, 5, 6, 7, 8, 0, 11, 11, 0, 0, 0, 16, 16, 17, 0, 0, 18, 0};
        assertArrayEquals(expectedWaterLower, smileCorrectionAuxdata.getWater_LowerBands(), 1e-8);

        double[] expectedWaterUpper = {2, 3, 4, 5, 6, 7, 8, 9, 9, 0, 12, 12, 0, 0, 0, 17, 18, 18, 0, 0, 21, 0};
        assertArrayEquals(expectedWaterUpper, smileCorrectionAuxdata.getWaterUpperBands(), 1e-8);

        double[] expectedLandLowerBands = {1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 0, 0, 0, 16, 16, 17, 0, 0, 18, 0.0};
        assertArrayEquals(expectedLandLowerBands, smileCorrectionAuxdata.getLandLowerBands(), 1e-8);

        double[] expectedLandUpperBands = {400, 412.5, 442.5, 490, 510, 560, 620, 665, 673.75, 681.25, 708.75, 753.75, 761.25, 764.37, 767.5, 778.75, 865, 885, 900, 940, 1020, 0};
        assertArrayEquals(expectedLandUpperBands, smileCorrectionAuxdata.getRefCentralWaveLenghts(), 1e-8);

        double[] expectedSolarIrradiance = {1441.8, 1685.2, 1864.1, 1923.7, 1943.5, 1804.4, 1653.4, 1532.3, 1497.9, 1472.4, 1408.4, 1265.9, 1252.1, 1248.5, 1222.1, 1184.5, 958.2, 929.5, 895.7, 824.7, 694.0, 0.0};
        assertArrayEquals(expectedSolarIrradiance, smileCorrectionAuxdata.getSolarIrradiances(), 1e-8);

    }
}