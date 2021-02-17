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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EuclidianRasterInterpolatorTest {

    @Test
    public void testInterpolate() {
        final InterpolationContext context = new InterpolationContext();
        context.lons = new double[]{16.011154, 16.017494, 16.00977, 16.016115};
        context.lats = new double[]{64.3739, 64.37316, 64.371346, 64.3706};
        context.x = new int[]{273, 274, 273, 274};
        context.y = new int[]{435, 435, 436, 436};

        final EuclidianRasterInterpolator interpolator = new EuclidianRasterInterpolator();

        GeoPos geoPos = new GeoPos(64.372, 16.013);
        PixelPos pixelPos = interpolator.interpolate(geoPos, null, context);
        assertEquals(273.45764449834144, pixelPos.x, 1e-8);
        assertEquals(435.55644428657826, pixelPos.y, 1e-8);
    }
}
