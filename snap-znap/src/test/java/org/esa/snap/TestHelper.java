/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap;

import java.lang.reflect.Field;

public class TestHelper {

    public static Object getPrivateFieldObject(Object fieldOwner, String fieldName) throws IllegalAccessException {
        Field field = findFieldRecursively(fieldOwner, fieldName);
        field.setAccessible(true);
        return field.get(fieldOwner);
    }

    public static void setPrivateFieldObject(Object fieldOwner, String fieldName, Object fieldValueToBeSet) throws IllegalAccessException {
        Field field = findFieldRecursively(fieldOwner, fieldName);
        field.setAccessible(true);
        field.set(fieldOwner, fieldValueToBeSet);
    }


    private static Field findFieldRecursively(Object fieldOwner, String fieldName) {
        Class<?> aClass = fieldOwner.getClass();
        Field field = null;
        try {
            field = aClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignore) {
        }
        while (field == null) {
            aClass = aClass.getSuperclass();
            try {
                field = aClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignore) {
            }
        }
        return field;
    }
}
