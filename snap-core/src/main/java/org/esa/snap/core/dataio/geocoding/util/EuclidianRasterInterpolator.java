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

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.util.math.EuclideanDistance;

class EuclidianRasterInterpolator implements XYInterpolator {

    private static final double MIN_DISTANCE = 1e-12; // in squared degrees

    @Override
    public PixelPos interpolate(GeoPos geoPos, PixelPos pixelPos, InterpolationContext context) {
        final double delta_lon_1 = context.lons[1] - context.lons[0];
        final double delta_lon_2 = context.lons[3] - context.lons[2];

        final double delta_lat_1 = context.lats[0] - context.lats[1];
        final double delta_lat_2 = context.lats[1] - context.lats[3];


        return null;
    }
}
