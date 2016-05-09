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

import org.esa.snap.core.gpf.OperatorException;

/**
 * @author muhammad.bc.
 */
public class SmileUtils {

    public static float[] multiple2ArrayFloat(float[] array1, float[] array2) {
        if (array1 == null || array2 == null) {
            //todo mba/*** ask MarcoP if null is ok.
            throw new OperatorException("d");
        }
        if (array1.length != array2.length) {
            throw new OperatorException("The arrays are null.");
        }
        final float[] cal = new float[array1.length];
        for (int i = 0; i < array1.length; i++) {
            cal[i] = array1[i] * array2[i];
        }
        return cal;
    }

    public static float[] multiple3ArrayFloat(float[] array1, float[] array2, float[] array3) {
        return array1;
    }
}
