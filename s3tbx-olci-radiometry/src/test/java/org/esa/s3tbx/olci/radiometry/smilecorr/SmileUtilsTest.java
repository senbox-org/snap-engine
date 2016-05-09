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
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author muhammad.bc.
 */
public class SmileUtilsTest {
    @Test
    public void testMultipleOf2ArraysNull() {

    }

    @Test
    public void testMultiple2ArrayNull() throws Exception {
        try {
            final float[] multiple2ArrayFloat = SmileUtils.multiple2ArrayFloat(null, null);
            fail("The arrays are null.");
        } catch (OperatorException e) {
        }

        final float[] arrayFloat = SmileUtils.multiple2ArrayFloat(new float[]{1, 2, 3}, new float[]{1, 2, 3});
        assertArrayEquals(new float[]{1, 4, 9}, arrayFloat, 1);
    }
}