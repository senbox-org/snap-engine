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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonUtils {

    public static int getInt(final Object o) {
        if (o instanceof Long) {
            return (int) (long) o;
        } else if (o instanceof Integer) {
            return (Integer) o;
        } else if (o instanceof Double) {
            return ((Double) o).intValue();
        } else if (o instanceof Float) {
            return ((Float) o).intValue();
        }
        return Integer.parseInt((String) o);
    }

    public static double getDouble(final Object o) {
        if (o instanceof Double) {
            return (Double) o;
        } else if (o instanceof Float) {
            return (Float) o;
        } else if (o instanceof Long || o instanceof Integer) {
            return getInt(o);
        }
        if (o == null) {
            return Double.NEGATIVE_INFINITY;
        }
        return Double.parseDouble((String) o);
    }

    public static String prettyPrint(final Object json) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }
}
