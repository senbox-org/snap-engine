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
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author muhammad.bc.
 */
public class SmileCorrectionAlgorithmTest {
    @Test
    public void testSpectralDerivative() throws Exception {
        SmileCorrectionAuxdata correctionAuxdata = Mockito.mock(SmileCorrectionAuxdata.class);
        when(correctionAuxdata.getRefCentralWaveLenghts()).thenReturn(new double[]{1, 2,3});
        SmileCorrectionAlgorithm correctionAlgorithm = new SmileCorrectionAlgorithm(correctionAuxdata);

        final double spectralDerivative = correctionAlgorithm.spectralDerivative(new double[]{5, 10});

        assertEquals(5, spectralDerivative, 1e-8);
    }
}