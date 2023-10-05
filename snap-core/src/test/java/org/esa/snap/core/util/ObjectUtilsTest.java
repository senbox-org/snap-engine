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

package org.esa.snap.core.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class ObjectUtilsTest {

    @Test
    public void testEqualObjects() {
        Object a = "A";
        Object b = "A";
        Object c = "1";
        Object d = "AB";
        Object e = a;
        assertTrue(ObjectUtils.equalObjects(a, b));
        assertFalse(ObjectUtils.equalObjects(a, c));
        assertFalse(ObjectUtils.equalObjects(a, d));
        assertTrue(ObjectUtils.equalObjects(a, e));
        assertTrue(ObjectUtils.equalObjects(null, null));
        assertFalse(ObjectUtils.equalObjects(a, null));
        assertFalse(ObjectUtils.equalObjects(null, a));

        double[] ad1 = new double[]{1.3, 2.3, 4.5};
        double[] ad2 = new double[]{1.3, 2.3, 4.5};
        double[] ad3 = new double[]{1.3, -.3, 4.5};
        assertTrue(ObjectUtils.equalObjects(ad1, ad1));
        assertTrue(ObjectUtils.equalObjects(ad1, ad2));
        assertFalse(ObjectUtils.equalObjects(ad1, ad3));
        assertFalse(ObjectUtils.equalObjects(ad1, b));

        Object[] aad1 = new Object[]{new double[]{1.3, 2.3, 4.5}, new double[]{9.1, 4.3, 4.7}};
        Object[] aad2 = new Object[]{new double[]{1.3, 2.3, 4.5}, new double[]{9.1, 4.3, 4.7}};
        Object[] aad3 = new Object[]{new double[]{1.3, -.3, 4.5}, new double[]{9.1, 4.3, 4.7}};
        assertTrue(ObjectUtils.equalObjects(aad1, aad1));
        assertTrue(ObjectUtils.equalObjects(aad1, aad2));
        assertFalse(ObjectUtils.equalObjects(aad1, aad3));
        assertFalse(ObjectUtils.equalObjects(aad1, b));
    }
}
