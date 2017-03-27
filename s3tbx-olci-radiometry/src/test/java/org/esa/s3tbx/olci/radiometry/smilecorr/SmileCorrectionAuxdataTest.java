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
        assertEquals(1513.6257, auxDataInFlatTable[0][8], 1e-8);

        assertEquals(6.0, auxDataInFlatTable[5][0], 1e-8);
        assertEquals(1796.8542, auxDataInFlatTable[5][8], 1e-8);
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

        float[] expectedSolarIrradiance = {
                1513.6257f,
                1708.0474f,
                1889.9923f,
                1936.2612f,
                1919.6490f,
                1796.8542f,
                1649.1400f,
                1530.1553f,
                1494.7185f,
                1468.8616f, 
                1403.1105f,
                1266.3196f,
                1247.4586f,
                1238.9945f,
                1229.7690f,
                1173.4987f,
                959.71075f,
                930.86300f,
                895.76700f,
                826.40735f,
                699.70306f,
                0f
        };
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
        float[] expectedSolarIrradiance = {
                1714.767334f,
                1878.892944f,
                1928.337158f,
                1928.936279f,
                1803.076294f,
                1650.773804f,
                1531.626465f,
                1472.168091f,
                1407.942627f,
                1266.042847f,
                1254.581177f,
                1177.259522f,
                958.3851929f,
                929.8380127f,
                895.4595947f,
                0f};
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