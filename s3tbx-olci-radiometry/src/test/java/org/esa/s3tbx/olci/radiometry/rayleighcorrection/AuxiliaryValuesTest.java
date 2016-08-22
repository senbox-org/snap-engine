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

package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import org.junit.Test;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author muhammad.bc.
 */
public class AuxiliaryValuesTest {
    @Test
    public void getCosSZA() throws Exception {
        AuxiliaryValues auxiliaryValues = new AuxiliaryValues();

        double[] sunZenithAngles = {1, 2, 3};
        auxiliaryValues.setSunZenithAngles(sunZenithAngles);
        auxiliaryValues.setCosSZARads();
        assertArrayEquals(sunZenithAngles, auxiliaryValues.getSunZenithAngles(), 1e-8);
        assertArrayEquals(new double[]{0.9998476951563913, 0.9993908270190958, 0.9986295347545738}, auxiliaryValues.getCosSZARads(), 1e-8);
    }

    @Test
    public void getSZARad() throws Exception {
        AuxiliaryValues auxiliaryValues = new AuxiliaryValues();

        double[] sunZenithAngles = {1, 2, 3};
        auxiliaryValues.setSunZenithAngles(sunZenithAngles);
        assertArrayEquals(sunZenithAngles, auxiliaryValues.getSunZenithAngles(), 1e-8);
        assertArrayEquals(new double[]{0.017453292519943295, 0.03490658504, 0.05235987756}, auxiliaryValues.getSunZenithAnglesRad(), 1e-8);
    }


    @Test
    public void testCreateLineSpaceOfArrayElements() throws Exception {
        AuxiliaryValues auxiliaryValues = new AuxiliaryValues();
        double[] lineSpace = auxiliaryValues.getLineSpace(0, 10, 5);
        assertNotNull(lineSpace);
        assertEquals(5, lineSpace.length);
        assertArrayEquals(new double[]{0.0, 2.5, 5.0, 7.5, 10.0}, lineSpace, 1e-8);

        lineSpace = auxiliaryValues.getLineSpace(0, 1, 5);
        assertEquals(5, lineSpace.length);
        assertArrayEquals(new double[]{0.0, 0.25, 0.5, 0.75, 1.0}, lineSpace, 1e-8);

        lineSpace = auxiliaryValues.getLineSpace(0, 0, 5);
        assertEquals(5, lineSpace.length);
        assertArrayEquals(new double[]{0.0, 0.0, 0.0, 0.0, 0.0}, lineSpace, 1e-8);

        try {
            lineSpace = auxiliaryValues.getLineSpace(0, 10, -5);
            fail("Array cant have negative index");
        } catch (NegativeArraySizeException ex) {

        }
    }
}