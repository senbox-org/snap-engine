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

import org.esa.s3tbx.olci.radiometry.rayleigh.SpikeInterpolation;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author muhammad.bc.
 */
public class SpikeInterpolationTest {
    double[] useAr = {-2, 1.3, 1.7, 2, 2.9, 3};

    @Test
    public void testClosestToLowerBound() throws Exception {
        Assert.assertEquals(1.3, SpikeInterpolation.getLowerBound(useAr, 1.4), 1e-2);
        Assert.assertEquals(1.3, SpikeInterpolation.getLowerBound(useAr, 1.5), 1e-2);
        Assert.assertEquals(1.7, SpikeInterpolation.getLowerBound(useAr, 1.9), 1e-2);
        Assert.assertEquals(2.0, SpikeInterpolation.getLowerBound(useAr, 2.1), 1e-2);
        Assert.assertEquals(2.9, SpikeInterpolation.getLowerBound(useAr, 3), 1e-2);
        Assert.assertEquals(3, SpikeInterpolation.getLowerBound(useAr, 4), 1e-2);
        Assert.assertEquals(3, SpikeInterpolation.getLowerBound(useAr, 30), 1e-2);
    }

    @Test
    public void testClosestToLowerBoundNotInArray() throws Exception {
        try {
            Assert.assertEquals(-2, SpikeInterpolation.getLowerBound(useAr, -3), 1e-2);
            Assert.assertEquals(-2, SpikeInterpolation.getLowerBound(useAr, -3), 1e-2);
            fail();
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testClosestToUpperBound() throws Exception {
        Assert.assertEquals(1.3, SpikeInterpolation.getUpperValue(useAr, 1.3), 1e-2);
        Assert.assertEquals(2, SpikeInterpolation.getUpperValue(useAr, 1.9), 1e-2);
        Assert.assertEquals(2.9, SpikeInterpolation.getUpperValue(useAr, 2.7), 1e-2);
        Assert.assertEquals(2.9, SpikeInterpolation.getUpperValue(useAr, 2.9), 1e-2);
    }

    @Test
    public void testClosestToUpperBoundNotInArray() throws Exception {
        try {
            Assert.assertEquals(-2, SpikeInterpolation.getUpperValue(useAr, -10), 1e-2);
            Assert.assertEquals(-2, SpikeInterpolation.getUpperValue(useAr, 100), 1e-2);
            fail("Can fine the closest max value of 100.0");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testGetValueIndexInArray() throws Exception {
        assertEquals(1, SpikeInterpolation.arrayIndex(useAr, 1.3));
        assertEquals(3, SpikeInterpolation.arrayIndex(useAr, 2));
    }

    @Test
    public void testInterpolatedBtwArrayRange() throws Exception {
        //https://en.wikipedia.org/wiki/Bilinear_interpolation

        double upperBound = SpikeInterpolation.interBetween(91, 210, 15, 14, 14.5);
        assertEquals(150.5, upperBound, 1e-2);
        double lowerBound = SpikeInterpolation.interBetween(162, 95, 15, 14, 14.5);
        assertEquals(128.5, lowerBound, 1e-2);
        //Interpolate between Y coordinate
        double v = SpikeInterpolation.interBetween(lowerBound, upperBound, 20, 21, 20.2);
        assertEquals(146.1, v, 1e-2);
    }

}
