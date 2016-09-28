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

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author muhammad.bc.
 */
public class SmileCorrectionAuxdataTest {


    @Test
    public void testAuxDataIsInstall() throws Exception {
        Path auxDataPath =  SmileCorrectionAuxdata.installAuxdata();
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

        int[] expectedBands = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 0};
        assertArrayEquals(expectedBands, smileCorrectionAuxdata.getBandIndices());

        int[] expectedWaterLower = {1, 1, 2, 3, 4, 5, 6, 7, 8, 0, 11, 11, 0, 0, 0, 16, 16, 17, 0, 0, 18, 0};
        assertArrayEquals(expectedWaterLower, smileCorrectionAuxdata.getWaterLowerBands());

        int[] expectedWaterUpper = {2, 3, 4, 5, 6, 7, 8, 9, 9, 0, 12, 12, 0, 0, 0, 17, 18, 18, 0, 0, 21, 0};
        assertArrayEquals(expectedWaterUpper, smileCorrectionAuxdata.getWaterUpperBands());

        int[] expectedLandLowerBands = {1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 0, 0, 0, 16, 16, 17, 0, 0, 18, 0};
        assertArrayEquals(expectedLandLowerBands, smileCorrectionAuxdata.getLandLowerBands());

        float[] expectedCentralWvl = {400f, 412.5f, 442.5f, 490f, 510f, 560f, 620f, 665f, 673.75f, 681.25f, 708.75f, 753.75f, 761.25f, 764.37f, 767.5f, 778.75f, 865f, 885f, 900f, 940f, 1020f, 0f};
        assertArrayEquals(expectedCentralWvl, smileCorrectionAuxdata.getRefCentralWaveLengths(), 1e-6f);

        float[] expectedSolarIrradiance = {1441.8f, 1685.2f, 1864.1f, 1923.7f, 1943.5f, 1804.4f, 1653.4f, 1532.3f, 1497.9f, 1472.4f, 1408.4f, 1265.9f, 1252.1f, 1248.5f, 1222.1f, 1184.5f, 958.2f, 929.5f, 895.7f, 824.7f, 694.0f, 0.0f};
        assertArrayEquals(expectedSolarIrradiance, smileCorrectionAuxdata.getSolarIrradiances(), 1e-6f);

    }
}