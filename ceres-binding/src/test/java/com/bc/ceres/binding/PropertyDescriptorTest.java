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

import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class PropertyDescriptorTest {
    @Test
    public void testMandatoryNameAndType() {
        PropertyDescriptor descriptor;

        descriptor = new PropertyDescriptor("wasIstDasDenn", String.class);
        assertEquals("wasIstDasDenn", descriptor.getName());
        assertEquals("Was ist das denn", descriptor.getDisplayName());
        assertSame(String.class, descriptor.getType());

        descriptor = new PropertyDescriptor("was_ist_das_denn", Double.TYPE);
        assertEquals("was_ist_das_denn", descriptor.getName());
        assertEquals("Was ist das denn", descriptor.getDisplayName());
        assertSame(Double.TYPE, descriptor.getType());

        descriptor.setDisplayName("Was denn");
        assertEquals("Was denn", descriptor.getDisplayName());

        try {
            descriptor.setDisplayName(null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }
    }
        @Test
    public void testThatPrimitiveTypesAreAlwaysNotNull() {
        assertThatPrimitiveTypesAreAlwaysNotNull(Character.TYPE, Character.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Byte.TYPE, Byte.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Short.TYPE, Short.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Integer.TYPE, Integer.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Long.TYPE, Long.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Float.TYPE, Float.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Double.TYPE, Double.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Void.TYPE, Void.class);
    }

    private static void assertThatPrimitiveTypesAreAlwaysNotNull(Class<?> primitiveType, Class<?> wrapperType) {
        assertTrue(new PropertyDescriptor("vd", primitiveType).isNotNull());
        assertFalse(new PropertyDescriptor("vd", wrapperType).isNotNull());
    }

       @Test
    public void testThatEnumTypesHaveValueSet() {
        PropertyDescriptor descriptor = new PropertyDescriptor("letter", Letter.class);
        assertNotNull(descriptor.getValueSet());
        assertArrayEquals(new Object[]{Letter.A,Letter.B,Letter.C,Letter.D},
                          descriptor.getValueSet().getItems());
    }

    @Test
    public void testCopyConstruction() {
        final PropertyDescriptor descriptor = new PropertyDescriptor("was_ist_das_denn", Double.TYPE);
        descriptor.setAttribute("wo", 12);
        descriptor.setAttribute("wie", 14);
        descriptor.setAttribute("wann", 16);

        final PropertyDescriptor copyConstructed = new PropertyDescriptor(descriptor);
        assertEquals("was_ist_das_denn", copyConstructed.getName());
        assertEquals(Double.TYPE, copyConstructed.getType());
        assertEquals(14, copyConstructed.getAttribute("wie"));
    }

    @Test
    public void testSetGetUnit() {
        final PropertyDescriptor descriptor = new PropertyDescriptor("depth", Double.TYPE);

        assertNull(descriptor.getUnit());
        descriptor.setUnit("m");
        assertEquals("m", descriptor.getUnit());
    }

    @Test
    public void testSetGetDescription() {
        final PropertyDescriptor descriptor = new PropertyDescriptor("depth", Double.TYPE);

        assertNull(descriptor.getDescription());
        descriptor.setDescription("describe");
        assertEquals("describe", descriptor.getDescription());
    }

    @Test
    public void testSetIsNotEmpty() {
        final PropertyDescriptor descriptor = new PropertyDescriptor("depth", Double.TYPE);

        assertFalse(descriptor.isNotEmpty());
        descriptor.setNotEmpty(true);
        assertTrue(descriptor.isNotEmpty());
    }

    @Test
    public void testSetIsTransient() {
        final PropertyDescriptor descriptor = new PropertyDescriptor("depth", Double.TYPE);

        assertFalse(descriptor.isTransient());
        descriptor.setTransient(true);
        assertTrue(descriptor.isTransient());
    }

    @Test
    public void testSetGetFormat() {
        final PropertyDescriptor descriptor = new PropertyDescriptor("depth", Double.TYPE);

        assertNull(descriptor.getFormat());
        descriptor.setFormat("complex");
        assertEquals("complex", descriptor.getFormat());
    }

    @Test
    public void testSetGetValueRange() {
        final PropertyDescriptor descriptor = new PropertyDescriptor("depth", Double.TYPE);

        assertNull(descriptor.getValueRange());

        descriptor.setValueRange(new ValueRange(8, 22));
        ValueRange valueRange = descriptor.getValueRange();
        assertEquals(8, valueRange.getMin(), 1e-8);
        assertEquals(22, valueRange.getMax(), 1e-8);
    }

    @Test
    public void testSetGetPattern() {
        final PropertyDescriptor descriptor = new PropertyDescriptor("depth", Double.TYPE);

        assertNull(descriptor.getPattern());

        Pattern pattern = Pattern.compile("[AB]");
        descriptor.setPattern(pattern);
        assertEquals(pattern, descriptor.getPattern());
    }

        private enum Letter {
        A,B,C,D,
    }
}
