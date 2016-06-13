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

package org.esa.s3tbx.meris.radiometry.smilecorr;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author muhammad.bc.
 */
public class SmileCorrectionAlgorithmTest {

    @Test
    public void testCalculateSmile() throws Exception {
        SmileCorrectionAuxdata auxdata = SmileCorrectionAuxdata.loadAuxdata("MER_F");
        SmileCorrectionAlgorithm algorithm = new SmileCorrectionAlgorithm(auxdata);
        double correct = algorithm.correct(0, 1, new double[]{412.691, 412.891}, true);
        assertEquals(412.691988282132,correct,1e-8);
    }
}