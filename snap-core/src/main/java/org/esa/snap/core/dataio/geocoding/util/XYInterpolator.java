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

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;

public interface XYInterpolator {

    String SYSPROP_GEOCODING_INTERPOLATOR = "snap.core.geocoding.interpolator";

    PixelPos interpolate(GeoPos geoPos, PixelPos pixelPos, InterpolationContext context);

    enum Type {
        EUCLIDIAN {
            @Override
            public XYInterpolator get() {
                return new EuclidianDistanceWeightingInterpolator();
            }
        },
        GEODETIC {
            @Override
            public XYInterpolator get() {
                return new GeodeticDistanceWeightingInterpolator();
            }
        };

        public abstract XYInterpolator get();
    }
}
