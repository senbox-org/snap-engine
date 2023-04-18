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

public class EuclidianDistanceWeightingInterpolatorTest {

    @Test
    public void testInterpolate() {
        final InterpolationContext context = new InterpolationContext();
        context.lons = new double[]{16.011154, 16.017494, 16.00977, 16.016115};
        context.lats = new double[]{64.3739, 64.37316, 64.371346, 64.3706};
        context.x = new int[]{273, 274, 273, 274};
        context.y = new int[]{435, 435, 436, 436};

        final EuclidianDistanceWeightingInterpolator interpolator = new EuclidianDistanceWeightingInterpolator();

        GeoPos geoPos = new GeoPos(64.372, 16.013);
        PixelPos pixelPos = interpolator.interpolate(geoPos, null, context);
        //           273.45764449834144
        assertEquals(273.36037412619277, pixelPos.x, 1e-8);
        //           435.55644428657826
        assertEquals(435.48486251879467, pixelPos.y, 1e-8);

        geoPos = new GeoPos(64.371, 16.015);
        pixelPos = interpolator.interpolate(geoPos, null, context);
        //           273.7319667307041
        assertEquals(273.9100685669, pixelPos.x, 1e-8);
        //           435.7402634275665
        assertEquals(435.8473267485266, pixelPos.y, 1e-8);

        geoPos = new GeoPos(64.3739, 16.011154);
        pixelPos = interpolator.interpolate(geoPos, null, context);
        assertEquals(273.0, pixelPos.x, 1e-8);
        assertEquals(435.0, pixelPos.y, 1e-8);

        geoPos = new GeoPos(64.37316, 16.017494);
        pixelPos = interpolator.interpolate(geoPos, null, context);
        assertEquals(274.0, pixelPos.x, 1e-8);
        assertEquals(435.0, pixelPos.y, 1e-8);

        geoPos = new GeoPos(64.371346, 16.00977);
        pixelPos = interpolator.interpolate(geoPos, null, context);
        assertEquals(273.0, pixelPos.x, 1e-8);
        assertEquals(436.0, pixelPos.y, 1e-8);

        geoPos = new GeoPos(64.3706, 16.016115);
        pixelPos = interpolator.interpolate(geoPos, null, context);
        assertEquals(274.0, pixelPos.x, 1e-8);
        assertEquals(436.0, pixelPos.y, 1e-8);
    }

    @Test
    public void testInterpolate_duplicateLocations() {
        final InterpolationContext context = new InterpolationContext();
        context.lons = new double[]{27.77865, 27.77865, 27.77332, 27.77332};
        context.lats = new double[]{73.653238, 73.653238, 73.650687, 73.650687};
        context.x = new int[]{192, 193, 192, 193};
        context.y = new int[]{430, 430, 431, 431};

        final EuclidianDistanceWeightingInterpolator interpolator = new EuclidianDistanceWeightingInterpolator();

        GeoPos geoPos = new GeoPos(73.652, 27.774);
        PixelPos pixelPos = interpolator.interpolate(geoPos, null, context);
        assertEquals(192.5, pixelPos.x, 1e-8);
        //           430.57586403503655
        assertEquals(430.9137238175166, pixelPos.y, 1e-8);

        geoPos = new GeoPos(73.651, 27.775);
        pixelPos = interpolator.interpolate(geoPos, null, context);
        assertEquals(192.5, pixelPos.x, 1e-8);
        //           430.8128151936594
        assertEquals(430.8625806548459, pixelPos.y, 1e-8);

        geoPos = new GeoPos(73.653, 27.776);
        pixelPos = interpolator.interpolate(geoPos, null, context);
        assertEquals(192.5, pixelPos.x, 1e-8);
        //           430.243454102032
        assertEquals(430.36096878399906, pixelPos.y, 1e-8);
    }
}
