/*
 *
 * Copyright (C) 2020 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.snap.core.dataio.geocoding.util;

import org.esa.snap.core.util.math.DistanceMeasure;
import org.esa.snap.core.util.math.RsMathUtils;
import org.esa.snap.core.util.math.SphericalDistance;

public class InverseDistanceWeightingInterpolator extends DistanceWeightingInterpolator {

    private static final double MIN_DISTANCE = 0.001 / RsMathUtils.MEAN_EARTH_RADIUS;   // which is one millimeter tb 2020-01-10

    @Override
    DistanceMeasure getDistanceMeasure(double lon, double lat) {
        return new SphericalDistance(lon, lat);
    }

    @Override
    double getMinDistance() {
        return MIN_DISTANCE;
    }
}
