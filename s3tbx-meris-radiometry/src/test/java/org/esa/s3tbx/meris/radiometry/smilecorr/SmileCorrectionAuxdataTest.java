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


package org.esa.s3tbx.meris.radiometry.smilecorr;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class SmileCorrectionAuxdataTest {

    private static SmileCorrectionAuxdata rrData;
    private static SmileCorrectionAuxdata frData;
    private static Path auxdataDir;

    @BeforeClass
    public static void beforeClass() throws Exception {
        auxdataDir = SmileCorrectionAuxdata.installAuxdata();
        rrData = SmileCorrectionAuxdata.loadRRAuxdata(auxdataDir);
        frData = SmileCorrectionAuxdata.loadFRAuxdata(auxdataDir);
    }

    @Test
    public void testThatDataIsReloadedWithoutUsingStaticArrayInstances() {
        SmileCorrectionAuxdata rrData = null;
        try {
            rrData = SmileCorrectionAuxdata.loadRRAuxdata(auxdataDir);
        } catch (IOException e) {
            fail("IOException not expected: " + e.getMessage());
        }
        assertNotSame(rrData.getRadCorrFlagsLand(), SmileCorrectionAuxdataTest.rrData.getRadCorrFlagsLand());
        assertNotSame(rrData.getRadCorrFlagsWater(), SmileCorrectionAuxdataTest.rrData.getRadCorrFlagsWater());
        assertNotSame(rrData.getLowerBandIndexesLand(), SmileCorrectionAuxdataTest.rrData.getLowerBandIndexesLand());
        assertNotSame(rrData.getLowerBandIndexesWater(), SmileCorrectionAuxdataTest.rrData.getLowerBandIndexesWater());
        assertNotSame(rrData.getUpperBandIndexesLand(), SmileCorrectionAuxdataTest.rrData.getUpperBandIndexesLand());
        assertNotSame(rrData.getUpperBandIndexesWater(), SmileCorrectionAuxdataTest.rrData.getUpperBandIndexesWater());
        assertNotSame(rrData.getTheoreticalWavelengths(), SmileCorrectionAuxdataTest.rrData.getTheoreticalWavelengths());
        assertNotSame(rrData.getTheoreticalSunSpectralFluxes(), SmileCorrectionAuxdataTest.rrData.getTheoreticalSunSpectralFluxes());
        assertNotSame(rrData.getDetectorWavelengths(), SmileCorrectionAuxdataTest.rrData.getDetectorWavelengths());
        assertNotSame(rrData.getDetectorSunSpectralFluxes(), SmileCorrectionAuxdataTest.rrData.getDetectorSunSpectralFluxes());
    }

    @Test
    public void testCurrentRrData() {
        //       	band    switch_land	lower_land	upper_land	switch_water	lower_water	upper_water	lam_theo	E0_theo
        //         	13	1	13	14	1	13	14	865	958.763
        assertEquals(true, rrData.getRadCorrFlagsLand()[12]);
        assertEquals(13 - 1, rrData.getLowerBandIndexesLand()[12]);
        assertEquals(14 - 1, rrData.getUpperBandIndexesLand()[12]);
        assertEquals(true, rrData.getRadCorrFlagsWater()[12]);
        assertEquals(13 - 1, rrData.getLowerBandIndexesWater()[12]);
        assertEquals(14 - 1, rrData.getUpperBandIndexesWater()[12]);
        assertEquals(865, rrData.getTheoreticalWavelengths()[12], 1.0e-6);
        assertEquals(958.763, rrData.getTheoreticalSunSpectralFluxes()[12], 1.0e-6);

        assertEquals(490.0209579, rrData.getDetectorWavelengths()[20][2], 1.0e-6);
        assertEquals(1929.29938966317, rrData.getDetectorSunSpectralFluxes()[20][2], 1.0e-10);
    }

    @Test
    public void testCurrentFrData() {
        //        band	switch_land	lower_land	upper_land	switch_water	lower_water	upper_water	lam_theo	E0_theo
        //          3	1	2	4	1	2	4	490	1929.26
        assertEquals(true, frData.getRadCorrFlagsLand()[2]);
        assertEquals(2 - 1, frData.getLowerBandIndexesLand()[2]);
        assertEquals(4 - 1, frData.getUpperBandIndexesLand()[2]);
        assertEquals(true, frData.getRadCorrFlagsWater()[2]);
        assertEquals(2 - 1, frData.getLowerBandIndexesWater()[2]);
        assertEquals(4 - 1, frData.getUpperBandIndexesWater()[2]);
        assertEquals(490.0, frData.getTheoreticalWavelengths()[2], 1.0e-6);
        assertEquals(1929.26, frData.getTheoreticalSunSpectralFluxes()[2], 1.0e-6);

        assertEquals(490.1104498, frData.getDetectorWavelengths()[20][2], 1.0e-6);
        assertEquals(1930.1682671039, frData.getDetectorSunSpectralFluxes()[20][2], 1.0e-10);
    }
}
