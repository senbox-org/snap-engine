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


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author muhammad.bc.
 */
public class SmileUtilsTest {

    @Test
    public void testMultiple2ArrayNullAndNotNull() throws Exception {
        assertArrayEquals(new float[]{1, 4, 9}, SmileCorrectionUtils.multiple2ArrayFloat(new float[]{1, 2, 3}, new float[]{1, 2, 3}), 0);
        assertArrayEquals(new float[]{0, 4, 10}, SmileCorrectionUtils.multiple2ArrayFloat(new float[]{0, 1, 2}, new float[]{3, 4, 5}), 0);
        assertArrayEquals(new float[]{0, 4, 10, 10}, SmileCorrectionUtils.multiple2ArrayFloat(new float[]{0, 1, 2, 1}, new float[]{3, 4, 5, 10}), 0);
    }


    @Test
    public void testMultiple3ArrayNullAndNotNull() throws Exception {
        assertArrayEquals(new float[]{0, 4, 20}, SmileCorrectionUtils.multiple3ArrayFloat(new float[]{0, 1, 2}, new float[]{3, 4, 5}, new float[]{0, 1, 2}), 0);
        assertArrayEquals(new float[]{0, 4, 10, 10}, SmileCorrectionUtils.multiple3ArrayFloat(new float[]{0, 1, 2, 1}, new float[]{3, 4, 5, 10}, new float[]{1, 1, 1, 1}), 0);
        float[] actuals = SmileCorrectionUtils.multiple3ArrayFloat(new float[]{1, 2, 3}, new float[]{4, 5, 6}, new float[]{7, 8, 9});
        assertEquals(3, actuals.length);
        assertArrayEquals(new float[]{28, 80, 162}, actuals, 0);
    }

    @Test
    public void convertDegToRads() throws Exception {
        double[] degToRads = SmileCorrectionUtils.convertDegreesToRadians(new double[]{1.0, 2.0, 3.0});
        assertArrayEquals(new double[]{0.017453292519943295, 0.03490658503988659, 0.05235987755982988}, degToRads, 1e-8);
    }

    @Test
    public void getAirMass() throws Exception {
        double[] airMass = SmileCorrectionUtils.getAirMass(new double[]{1.0, 2.0, 3}, new double[]{1.0, 2.0, 3});
        assertArrayEquals(new double[]{2.0, 1.0, 0.6666}, airMass, 1e-4);
    }

    @Test
    public void getAziDiff() throws Exception {
        double[] aziDiff = SmileCorrectionUtils.getAziDiff(new double[]{2.0, 8.0, 10.0}, new double[]{4.0, 5.0, 6.0});
        assertArrayEquals(new double[]{2.0, 3.0, 2.2831}, aziDiff, 1e-4);
    }
}