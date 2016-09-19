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

package org.esa.s3tbx.olci.radiometry.rayleigh;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrectionOpIITest {
    RayleighCorrectionOp rayleighCorrectionOpII = new RayleighCorrectionOp();

    @Test
    public void testGetBandIndex() throws Exception {
        assertEquals(8, rayleighCorrectionOpII.getSourceBandIndex("band_08"));
        assertEquals(-1, rayleighCorrectionOpII.getSourceBandIndex("band"));
        assertEquals(9, rayleighCorrectionOpII.getSourceBandIndex("09band"));
        assertEquals(5, rayleighCorrectionOpII.getSourceBandIndex("Bla05band"));
    }
}