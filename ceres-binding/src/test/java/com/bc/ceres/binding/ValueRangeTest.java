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

package com.bc.ceres.binding;

import org.junit.Test;

import static org.junit.Assert.*;

public class ValueRangeTest {

    @Test
    public void testParseFailures() {
        try {
            ValueRange.parseValueRange(null);
            fail();
        } catch (NullPointerException e) {
        } catch (IllegalArgumentException e) {
            fail();
        }
        try {
            ValueRange.parseValueRange("");
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            ValueRange.parseValueRange("10,20");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testParse() {
        ValueRange valueRange = ValueRange.parseValueRange("[10,20)");
        assertEquals(10.0, valueRange.getMin(), 1e-10);
        assertTrue(valueRange.hasMin());
        assertTrue(valueRange.isMinIncluded());
        assertEquals(20.0, valueRange.getMax(), 1e-10);
        assertTrue(valueRange.hasMax());
        assertFalse(valueRange.isMaxIncluded());

        valueRange = ValueRange.parseValueRange("(-10,20]");
        assertEquals(-10.0, valueRange.getMin(), 1e-10);
        assertFalse(valueRange.isMinIncluded());
        assertEquals(20.0, valueRange.getMax(), 1e-10);
        assertTrue(valueRange.isMaxIncluded());
        assertTrue(valueRange.hasMin());
        assertTrue(valueRange.hasMax());

        valueRange = ValueRange.parseValueRange("(*, 20]");
        assertEquals(Double.NEGATIVE_INFINITY, valueRange.getMin(), 1e-10);
        assertFalse(valueRange.hasMin());
        assertFalse(valueRange.isMinIncluded());
        assertEquals(20.0, valueRange.getMax(), 1e-10);
        assertTrue(valueRange.hasMax());
        assertTrue(valueRange.isMaxIncluded());

        valueRange = ValueRange.parseValueRange("[-10,*]");
        assertEquals(-10.0, valueRange.getMin(), 1e-10);
        assertTrue(valueRange.isMinIncluded());
        assertEquals(Double.POSITIVE_INFINITY, valueRange.getMax(), 1e-10);
        assertFalse(valueRange.hasMax());
        assertTrue(valueRange.isMaxIncluded());
        assertTrue(valueRange.hasMin());
    }

    @Test
    public void testContains() {
        ValueRange valueRange = new ValueRange(-1.5, 3.2, true, false);

        assertFalse(valueRange.contains(-1.6));
        assertTrue(valueRange.contains(-1.5));
        assertTrue(valueRange.contains(-1.4));

        assertTrue(valueRange.contains(3.1));
        assertFalse(valueRange.contains(3.2));
        assertFalse(valueRange.contains(3.3));

        valueRange = new ValueRange(-1.5, 3.2, false, true);

        assertFalse(valueRange.contains(-1.6));
        assertFalse(valueRange.contains(-1.5));
        assertTrue(valueRange.contains(-1.4));

        assertTrue(valueRange.contains(3.1));
        assertTrue(valueRange.contains(3.2));
        assertFalse(valueRange.contains(3.3));

        valueRange = new ValueRange(Double.NEGATIVE_INFINITY, 3.2, false, true);

        assertTrue(valueRange.contains(-1.6));
        assertTrue(valueRange.contains(-1.5));
        assertTrue(valueRange.contains(-1.4));

        assertTrue(valueRange.contains(3.1));
        assertTrue(valueRange.contains(3.2));
        assertFalse(valueRange.contains(3.3));

        valueRange = new ValueRange(-1.5, Double.POSITIVE_INFINITY, false, true);

        assertFalse(valueRange.contains(-1.6));
        assertFalse(valueRange.contains(-1.5));
        assertTrue(valueRange.contains(-1.4));

        assertTrue(valueRange.contains(3.1));
        assertTrue(valueRange.contains(3.2));
        assertTrue(valueRange.contains(3.3));
    }
}
