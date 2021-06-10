/*
 * Copyright (c) 2021. Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.znap.snap;

import java.util.HashMap;
import java.util.Map;

public final class CFConstantsAndUtils {

    // CF sample coding attributes
    public static final String FLAG_VALUES = "flag_values";
    public static final String FLAG_MASKS = "flag_masks";
    public static final String FLAG_MEANINGS = "flag_meanings";

    private static final Map<String, String> unitMap = new HashMap<>();

    static {
        unitMap.put("deg", "degree");
    }

    public static String tryFindUnitString(String unit) {
        if (unitMap.containsKey(unit)) {
            return unitMap.get(unit);
        }
        return unit;
    }
}
