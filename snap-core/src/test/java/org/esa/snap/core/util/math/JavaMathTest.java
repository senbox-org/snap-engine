/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.util.math;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A set of tests which are used to demonstrate
 * behaviour of Java mathematics.
 */
public class JavaMathTest {

    @Test
    public void testUnsignedIntegerArithmetic() {

        assertEquals(0xff, 0xff);
        assertEquals(0xff, 0x00ff);
        assertEquals(0xff, 0x0000ff);
        assertEquals(0xff, 0x000000ff);
        assertEquals(0xff, 255);

        assertEquals(-1, (int) (byte) 0xff);

        byte b;
        b = -128;
        assertNotEquals(b << 8, (b & 0xff) << 8);
        b = 127;
        assertEquals(b << 8, (b & 0xff) << 8);

        int i;
        i = 240;
        assertEquals((byte) -16, (byte) i);
        i = -16;
        assertEquals((byte) -16, (byte) i);
    }

    /**
     * If this test fails with some JDK > 5.0, just comment it out.
     * BEAM does not really rely on the success of this test,
     * it's just for demonstration purpose.
     */
    @Test
    public void testStrangeFloatingPointValues() {
        float f;

        // Float.NaN is really not a number:
        f = (float) Math.sqrt(-1);
        assertTrue(Float.isNaN(f));
        assertFalse((f == f));
        assertTrue(f != f);
        assertTrue(f != Float.NaN);

        // Float.POSITIVE_INFINITY is a real number:
        f = +1f / 0f;
        assertTrue(Float.isInfinite(f));
        assertEquals(Float.POSITIVE_INFINITY, f, 0.0);
        assertTrue(f > 0f);
        assertEquals(f, f, 0.0);
        assertFalse(f != f);

        // Float.NEGATIVE_INFINITY is a real number:
        f = -1f / 0f;
        assertTrue(Float.isInfinite(f));
        assertEquals(Float.NEGATIVE_INFINITY, f, 0.0);
        assertTrue(f < 0f);
        assertEquals(f, f, 0.0);
        assertFalse(f != f);

        // test that greater- and less-than comparisions with NaN always fail
        assertFalse(Float.NaN <= Float.POSITIVE_INFINITY);
        assertFalse(Float.NaN < Float.POSITIVE_INFINITY);
        assertFalse(Float.NaN >= Float.NEGATIVE_INFINITY);
        assertFalse(Float.NaN > Float.NEGATIVE_INFINITY);
    }

    @Test
    public void testNaN() {
        double x = Double.NaN;
        assertFalse((x == Double.NaN));
        assertTrue(x != Double.NaN);
        x = 6;
        assertTrue(x != Double.NaN);
        assertNotEquals(Double.NaN, x, 0.0);
        assertTrue(Float.isNaN((float) Double.NaN));
        assertTrue(Double.isNaN(Float.NaN));
        assertTrue(Double.isNaN(Math.floor(Double.NaN)));
        assertEquals(0, (int) Float.NaN);
        assertEquals(0, (int) Math.floor(Float.NaN));
        assertEquals(0, (int) Math.floor(Float.NaN + 0.5f));
        assertEquals(Integer.MIN_VALUE, (int) Float.NEGATIVE_INFINITY);
        assertEquals(Integer.MAX_VALUE, (int) Float.POSITIVE_INFINITY);
        assertEquals(0, (int) Double.NaN);
        assertEquals(0, (int) Math.floor(Double.NaN));
        assertEquals(0, (int) Math.floor(Double.NaN + 0.5f));
        assertEquals(Integer.MIN_VALUE, (int) Double.NEGATIVE_INFINITY);
        assertEquals(Integer.MAX_VALUE, (int) Double.POSITIVE_INFINITY);
        assertEquals(Double.NaN, Math.max(Double.NaN, 5.4), 1e-8);
        assertFalse(Double.NaN < 5.4);
        assertFalse(Double.NaN > 5.4);
        assertNotEquals(Double.NaN, 5.4, 0.0);
        assertFalse(Double.NaN < Double.NEGATIVE_INFINITY);
        assertFalse(Double.NaN > Double.NEGATIVE_INFINITY);

        assertTrue(Double.isNaN(0.0 * Double.NaN));

    }

    /**
     * If this test fails with some JDK > 5.0, just comment it out.
     * BEAM does not really rely on the success of this test,
     * it's just for demonstration purpose.
     */
    @Test
    public void testDoubleUnequalToFloat() {
        assertTrue(0.1f != 0.1d);
        assertEquals(1f, 1d, 0.0);
        assertEquals((0.1f * 10f), (0.1d * 10d), 0.0);

        final double d1 = 7.1;
        final float f1 = (float) d1;
        assertNotEquals(d1, f1, 0.0);
    }

    /**
     * If this test fails with some JDK > 5.0, just comment it out.
     * BEAM does not really rely on the success of this test,
     * it's just for demonstration purpose.
     */
    @Test
    public void testFloatEqualToDouble() {
        final float f1 = 7.1f;
        final double d1 = f1;

        assertEquals(f1, d1, 0.0);
    }

    /**
     * If this test fails with some JDK > 5.0, just comment it out.
     * BEAM does not really rely on the success of this test,
     * it's just for demonstration purpose.
     */
    @Test
    public void testComputingWithInfinity() {

        assertEquals(Double.NEGATIVE_INFINITY, Math.log(0.0), 0.0);
        assertTrue(Double.isNaN(Math.log(Double.NEGATIVE_INFINITY)));
        assertEquals(Double.POSITIVE_INFINITY, Math.log(Double.POSITIVE_INFINITY), 0.0);

        assertEquals(1.0, Math.exp(0.0), 0.0);
        assertEquals(0.0, Math.exp(Double.NEGATIVE_INFINITY), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, Math.exp(Double.POSITIVE_INFINITY), 0.0);


        assertEquals(Double.NEGATIVE_INFINITY, -Double.POSITIVE_INFINITY, 0.0);
        assertEquals(Double.POSITIVE_INFINITY, -Double.NEGATIVE_INFINITY, 0.0);

        assertEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY + 1.0, 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY + 1.0, 0.0);
        assertEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY - 1.0, 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY - 1.0, 0.0);
        assertEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY * 2.0, 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY * 2.0, 0.0);
        assertEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY / 2.0, 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY / 2.0, 0.0);

        assertTrue(Double.isNaN(Double.NEGATIVE_INFINITY + Double.POSITIVE_INFINITY));
        assertTrue(Double.isNaN(Double.POSITIVE_INFINITY + Double.NEGATIVE_INFINITY));
        assertEquals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY - Double.POSITIVE_INFINITY, 0.0);
        assertEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY - Double.NEGATIVE_INFINITY, 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY * Double.POSITIVE_INFINITY, 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY * Double.NEGATIVE_INFINITY, 0.0);
        assertTrue(Double.isNaN(Double.NEGATIVE_INFINITY / Double.POSITIVE_INFINITY));
        assertTrue(Double.isNaN(Double.POSITIVE_INFINITY / Double.NEGATIVE_INFINITY));
    }
}
