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

import org.esa.s3tbx.olci.radiometry.Sensor;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author muhammad.bc.
 */
public class SmileCorrectionAuxdataTest {


    @Test
    public void testAuxDataIsInstall() throws Exception {
        Path auxDataPath = SmileCorrectionAuxdata.installAuxdata();
        List<Path> collect = Files.list(auxDataPath).collect(Collectors.toList());

        assertTrue(auxDataPath.isAbsolute());
        assertTrue(collect.stream().anyMatch(path -> path.getFileName().toString().equals("band_info_olci.txt")));
        assertTrue(collect.stream().anyMatch(path -> path.getFileName().toString().equals("band_value.txt")));
    }

    @Test
    public void testAuxDataAreLoadedInFlatTable() throws Exception {
        List<String[]> loadAuxdata = SmileCorrectionAuxdata.loadAuxdata(Sensor.OLCI.getBandInfoFileName());
        double[][] auxDataInFlatTable = SmileCorrectionAuxdata.auxDataInFlatTable(loadAuxdata, 9);
        assertEquals(22, auxDataInFlatTable.length);
        assertEquals(9, auxDataInFlatTable[0].length);

        assertEquals(1, auxDataInFlatTable[0][0], 1e-8);
        assertEquals(1441.8, auxDataInFlatTable[0][8], 1e-8);

        assertEquals(6.0, auxDataInFlatTable[5][0], 1e-8);
        assertEquals(1804.4, auxDataInFlatTable[5][8], 1e-8);
    }

    @Test
    public void testAuxDataValueForOlci() {
        SmileCorrectionAuxdata smileCorrectionAuxdata = new SmileCorrectionAuxdata(Sensor.OLCI);

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

    @Test
    public void testAuxDataValueForMeris() {
        SmileCorrectionAuxdata smileCorrectionAuxdata = new SmileCorrectionAuxdata(Sensor.MERIS);

        int[] expectedBands = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0};
        assertArrayEquals(expectedBands, smileCorrectionAuxdata.getBandIndices());

        int[] expectedWaterLower = {1, 1, 2, 3, 4, 5, 6, 7, 8, 10, 10, 10, 13, 13, 13, 0};
        assertArrayEquals(expectedWaterLower, smileCorrectionAuxdata.getWaterLowerBands());

        int[] expectedWaterUpper = {2, 3, 4, 5, 6, 7, 9, 9, 9, 12, 12, 12, 14, 14, 14, 0};
        assertArrayEquals(expectedWaterUpper, smileCorrectionAuxdata.getWaterUpperBands());

        int[] expectedLandLowerBands = {1, 1, 2, 3, 4, 5, 6, 7, 9, 10, 10, 10, 13, 13, 13, 0};
        assertArrayEquals(expectedLandLowerBands, smileCorrectionAuxdata.getLandLowerBands());

        int[] expectedLandUpperBands = {2, 3, 4, 5, 6, 7, 9, 8, 10, 12, 12, 12, 14, 14, 14, 0};
        assertArrayEquals(expectedLandUpperBands, smileCorrectionAuxdata.getLandUpperBands());


        float[] expectedCentralWvl = {412.5f, 442.5f, 490f, 510f, 560f, 620f, 665f, 681.25f, 708.75f, 753.75f, 761.875f, 778.75f, 865f, 885f, 900f, 0f};
        assertArrayEquals(expectedCentralWvl, smileCorrectionAuxdata.getRefCentralWaveLengths(), 1e-6f);
        float[] expectedSolarIrradiance = {1713.69f, 1877.57f, 1929.26f, 1926.89f, 1800.46f, 1649.70f, 1530.93f, 1470.23f, 1405.47f, 1266.20f, 1249.80f, 1175.74f, 958.763f, 929.786f, 895.460f, 0f};
        assertArrayEquals(expectedSolarIrradiance, smileCorrectionAuxdata.getSolarIrradiances(), 1e-6f);

    }

    @Ignore
    @Test
    public void testReadSolarFluxMER_F() throws Exception {
        SmileCorrectionAuxdata sCorrectAux = new SmileCorrectionAuxdata(Sensor.MERIS);
        sCorrectAux.loadFluxWaven("MER_F");
        double[][] detSunSpectralFlux = sCorrectAux.getDetectorSunSpectralFluxes();
        assertNotNull(detSunSpectralFlux);
        assertEquals(15, detSunSpectralFlux.length);

        assertEquals(1715.95068068023, detSunSpectralFlux[0][0], 1e-8);
        assertEquals(1715.94499537724, detSunSpectralFlux[0][1], 1e-8);
        assertEquals(1715.87048338401, detSunSpectralFlux[0][14], 1e-8);
    }

    @Ignore
    @Test
    public void testReadSolarFluxMER_R() throws Exception {
        SmileCorrectionAuxdata sCorrectAux = new SmileCorrectionAuxdata(Sensor.MERIS);
        sCorrectAux.loadFluxWaven("MER_R");
        double[][] detSunSpectralFlux = sCorrectAux.getDetectorSunSpectralFluxes();
        assertNotNull(detSunSpectralFlux);
        assertEquals(15, detSunSpectralFlux.length);

        assertEquals(1715.92504199224, detSunSpectralFlux[0][0], 1e-8);
        assertEquals(1715.90214552674, detSunSpectralFlux[0][1], 1e-8);
        assertEquals(1715.59427589335, detSunSpectralFlux[0][14], 1e-8);
    }
}