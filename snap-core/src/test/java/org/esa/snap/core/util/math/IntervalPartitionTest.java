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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test methods for class {@link IntervalPartition}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class IntervalPartitionTest {

    @Test
    public void testConstructor() {
        try {
            new IntervalPartition((double[]) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new IntervalPartition(new double[0]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            new IntervalPartition(new double[2]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        final IntervalPartition decreasing = new IntervalPartition(1.0, 0.0);

        assertTrue(decreasing.getMonotonicity() < 0);
        assertEquals(2, decreasing.getCardinal());

        assertEquals(1.0, decreasing.get(0), 0.0);
        assertEquals(0.0, decreasing.get(1), 0.0);

        assertEquals(0.0, decreasing.getMin(), 0.0);
        assertEquals(1.0, decreasing.getMax(), 1.0);

        assertEquals(1.0, decreasing.getMesh(), 1.0);

        final IntervalPartition increasing = new IntervalPartition(0.0, 1.0);

        assertTrue(increasing.getMonotonicity() > 0);
        assertEquals(2, increasing.getCardinal());

        assertEquals(0.0, increasing.get(0), 0.0);
        assertEquals(1.0, increasing.get(1), 0.0);

        assertEquals(0.0, decreasing.getMin(), 0.0);
        assertEquals(1.0, decreasing.getMax(), 1.0);

        assertEquals(1.0, decreasing.getMesh(), 1.0);
    }

    @Test
    public void testCreateArray() {
        try {
            IntervalPartition.createArray((double[]) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            IntervalPartition.createArray(new double[0]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            IntervalPartition.createArray(new double[2]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        IntervalPartition[] partitions;

        partitions = IntervalPartition.createArray(new double[]{1.0, 0.0});

        assertEquals(1, partitions.length);

        assertEquals(2, partitions[0].getCardinal());

        assertEquals(1.0, partitions[0].get(0), 0.0);
        assertEquals(0.0, partitions[0].get(1), 0.0);

        try {
            IntervalPartition.createArray((double[][]) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            IntervalPartition.createArray(new double[0][]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            IntervalPartition.createArray(new double[1][0]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            IntervalPartition.createArray(new double[1][2]);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        partitions = IntervalPartition.createArray(new double[]{0.0, 1.0}, new double[]{1.0, 0.0});

        assertEquals(2, partitions.length);

        assertEquals(2, partitions[0].getCardinal());
        assertEquals(2, partitions[1].getCardinal());

        assertEquals(0.0, partitions[0].get(0), 0.0);
        assertEquals(1.0, partitions[0].get(1), 0.0);
        assertEquals(1.0, partitions[1].get(0), 0.0);
        assertEquals(0.0, partitions[1].get(1), 0.0);

        partitions = IntervalPartition.createArray(new double[]{0.0, 1.0}, new double[]{2.0, 3.0});

        assertEquals(2, partitions.length);

        assertEquals(2, partitions[0].getCardinal());
        assertEquals(2, partitions[1].getCardinal());

        assertEquals(0.0, partitions[0].get(0), 0.0);
        assertEquals(1.0, partitions[0].get(1), 0.0);
        assertEquals(2.0, partitions[1].get(0), 0.0);
        assertEquals(3.0, partitions[1].get(1), 0.0);
    }

    @Test
    public void testEnsureStrictMonotonicity() {

        assertTrue(IntervalPartition.ensureStrictMonotonicity(new Array.Double(0.1, 0.4, 0.8)) > 0);
        assertTrue(IntervalPartition.ensureStrictMonotonicity(new Array.Double(0.3, 0.2, 0.1)) < 0);

        try {
            IntervalPartition.ensureStrictMonotonicity(new Array.Double(0.1, 0.1, 0.8));
            fail("Should have thrown exception. Sequence is not monotonic increasing");
        } catch (IllegalArgumentException ignore) {
        }

        try {
            IntervalPartition.ensureStrictMonotonicity(new Array.Double(0.3, 0.5, 0.1));
            fail("Should have thrown exception. Sequence is not monotonic decreasing");
        } catch (IllegalArgumentException ignore) {
        }
    }
}
