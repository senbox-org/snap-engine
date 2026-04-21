/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.stac.internal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestJsonUtils {

    // getInt tests

    @Test
    public void testGetIntFromLong() {
        assertEquals(42, JsonUtils.getInt(42L));
    }

    @Test
    public void testGetIntFromInteger() {
        assertEquals(42, JsonUtils.getInt(42));
    }

    @Test
    public void testGetIntFromDouble() {
        assertEquals(42, JsonUtils.getInt(42.9));
    }

    @Test
    public void testGetIntFromFloat() {
        assertEquals(42, JsonUtils.getInt(42.7f));
    }

    @Test
    public void testGetIntFromString() {
        assertEquals(42, JsonUtils.getInt("42"));
    }

    @Test
    public void testGetIntZero() {
        assertEquals(0, JsonUtils.getInt(0L));
    }

    @Test
    public void testGetIntNegative() {
        assertEquals(-5, JsonUtils.getInt(-5L));
    }

    // getDouble tests

    @Test
    public void testGetDoubleFromDouble() {
        assertEquals(3.14, JsonUtils.getDouble(3.14), 1e-10);
    }

    @Test
    public void testGetDoubleFromFloat() {
        assertEquals(3.14f, JsonUtils.getDouble(3.14f), 1e-5);
    }

    @Test
    public void testGetDoubleFromLong() {
        assertEquals(42.0, JsonUtils.getDouble(42L), 1e-10);
    }

    @Test
    public void testGetDoubleFromInteger() {
        assertEquals(42.0, JsonUtils.getDouble(42), 1e-10);
    }

    @Test
    public void testGetDoubleFromString() {
        assertEquals(3.14, JsonUtils.getDouble("3.14"), 1e-10);
    }

    @Test
    public void testGetDoubleFromNull() {
        assertEquals(Double.NEGATIVE_INFINITY, JsonUtils.getDouble(null), 0);
    }

    @Test
    public void testGetDoubleZero() {
        assertEquals(0.0, JsonUtils.getDouble(0.0), 1e-10);
    }

    @Test
    public void testGetDoubleNegative() {
        assertEquals(-9999.0, JsonUtils.getDouble(-9999.0), 1e-10);
    }
}
