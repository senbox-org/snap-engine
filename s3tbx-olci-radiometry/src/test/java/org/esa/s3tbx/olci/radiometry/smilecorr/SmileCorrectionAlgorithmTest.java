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

import org.esa.snap.core.util.math.RsMathUtils;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author muhammad.bc.
 */
public class SmileCorrectionAlgorithmTest {
    @Test
    public void testCalculateSmileCorrection() throws Exception {
        //224
        float lowerRef = RsMathUtils.radianceToReflectance(107.214905f, 41.63521f, 1730.1123f);
        float targetRef = RsMathUtils.radianceToReflectance(98.60547f, 41.63521f, 1913.8246f);
        float upperRef = RsMathUtils.radianceToReflectance(79.279076f, 41.63521f, 1959.5077f);

        float correctOLCI = SmileCorrectionAlgorithm.correctWithReflectance(targetRef, lowerRef, upperRef, 442.93088f, 411.735f, 490.3521f, 442.5f);
        float withConvertionRad2Ref = RsMathUtils.reflectanceToRadiance(correctOLCI, 41.63521f, 1864.1f);

        float withoutConversion = SmileCorrectionAlgorithm.correctionWithRadiance(98.60547f, 107.214905f, 79.279076f, 442.93088f, 411.735f, 490.3521f, 442.5f, 1913.8246f, 1864.1f, 1730.1123f, 1959.5077f);
        assertEquals(withConvertionRad2Ref, withoutConversion, 1e-4);
    }
}