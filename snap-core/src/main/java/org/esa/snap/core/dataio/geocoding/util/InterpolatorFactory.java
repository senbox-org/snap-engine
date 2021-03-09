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
 *
 *
 */

package org.esa.snap.core.dataio.geocoding.util;

import java.util.Properties;

import static org.esa.snap.core.dataio.geocoding.util.XYInterpolator.SYSPROP_GEOCODING_INTERPOLATOR;

/**
 * @author Marco Peters, Sabine Embacher, Tom Block
 */
public class InterpolatorFactory {

    public static XYInterpolator create(Properties properties) {
        final String interpolatorTypeName = properties.getProperty(SYSPROP_GEOCODING_INTERPOLATOR, XYInterpolator.Type.EUCLIDIAN.name());
        return XYInterpolator.Type.valueOf(interpolatorTypeName).get();
    }
}
