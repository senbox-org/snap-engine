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

package org.esa.snap.core.dataio.geocoding.util;

import org.esa.snap.runtime.Config;

import java.util.prefs.Preferences;

public class InterpolatorFactory {

    public final static String SYSPROP_INTERPOLATOR_GEODETIC = "snap.core.geocoding.interpolator.geodetic";

    public static DistanceWeightingInterpolator get() {
        final Preferences preferences = Config.instance("snap").preferences();
        final boolean isGeodetic = preferences.getBoolean(SYSPROP_INTERPOLATOR_GEODETIC, false);
        if (isGeodetic) {
            return new InverseDistanceWeightingInterpolator();
        }
        return new EuclidianRasterInterpolator();
    }
}
