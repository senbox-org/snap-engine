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

import org.esa.snap.core.util.math.EuclideanDistance;

public class InterpolationContext {

    public double[] lons;
    public double[] lats;
    public int[] x;
    public int[] y;
    public InterpolationContext() {
        lons = new double[4];
        lats = new double[4];
        x = new int[4];
        y = new int[4];
    }

    public static InterpolationContext extract(int x, int y, double[] longitudes, double[] latitudes, int rasterWidth, int rasterHeight) {
        // search for closest surrounding (3x3) quadrant center location
        final double[] center_lons = new double[4];
        final double[] center_lats = new double[4];

        // the reference geo-location
        final int lineOffset = y * rasterWidth;
        final int lineOffset_minus = (y - 1) * rasterWidth;
        final int centerIndex = lineOffset + x;
        final double ref_lon = longitudes[centerIndex];
        final double ref_lat = latitudes[centerIndex];

        if ((y - 1) >= 0 && (x - 1) >= 0) {
            center_lons[0] = 0.25 * (longitudes[lineOffset_minus + x - 1] + longitudes[lineOffset_minus + x] + longitudes[lineOffset + x - 1] + longitudes[lineOffset + x]);
            center_lats[0] = 0.25 * (latitudes[lineOffset_minus + x - 1] + latitudes[lineOffset_minus + x] + latitudes[lineOffset + x - 1] + latitudes[lineOffset + x]);
        } else {
            center_lons[0] = Double.NaN;
            center_lats[0] = Double.NaN;
        }
        if ((y - 1) >= 0 && (x + 1) < rasterWidth) {
            center_lons[1] = 0.25 * (longitudes[lineOffset_minus + x] + longitudes[lineOffset_minus + x + 1] + longitudes[lineOffset + x] + longitudes[lineOffset + x + 1]);
            center_lats[1] = 0.25 * (latitudes[lineOffset_minus + x] + latitudes[lineOffset_minus + x + 1] + latitudes[lineOffset + x] + latitudes[lineOffset + x + 1]);
        } else {
            center_lons[1] = Double.NaN;
            center_lats[1] = Double.NaN;
        }
        if ((y + 1) < rasterHeight && (x + 1) < rasterWidth) {
            center_lons[2] = 0.25 * (longitudes[lineOffset + x] + longitudes[lineOffset + x + 1] + longitudes[(y + 1) * rasterWidth + x + 1] + longitudes[(y + 1) * rasterWidth + x]);
            center_lats[2] = 0.25 * (latitudes[lineOffset + x] + latitudes[lineOffset + x + 1] + latitudes[(y + 1) * rasterWidth + x + 1] + latitudes[(y + 1) * rasterWidth + x]);
        } else {
            center_lons[2] = Double.NaN;
            center_lats[2] = Double.NaN;
        }
        if ((y + 1) < rasterHeight && (x - 1) >= 0) {
            center_lons[3] = 0.25 * (longitudes[lineOffset + x - 1] + longitudes[lineOffset + x] + longitudes[(y + 1) * rasterWidth + x] + longitudes[(y + 1) * rasterWidth + x - 1]);
            center_lats[3] = 0.25 * (latitudes[lineOffset + x - 1] + latitudes[lineOffset + x] + latitudes[(y + 1) * rasterWidth + x] + latitudes[(y + 1) * rasterWidth + x - 1]);
        } else {
            center_lons[3] = Double.NaN;
            center_lats[3] = Double.NaN;
        }

        int quadrant = -1;
        // final SphericalDistance distCalc = new SphericalDistance(ref_lon, ref_lat);
        final EuclideanDistance distCalc = new EuclideanDistance(ref_lon, ref_lat);
        double min_dist = Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            if (!Double.isNaN(center_lons[i])) {
                final double distance = distCalc.distance(center_lons[i], center_lats[i]);
                if (distance < min_dist) {
                    min_dist = distance;
                    quadrant = i;
                }
            }
        }

        int x_start;
        int y_start;
        if (quadrant == 0) {
            x_start = x - 1;
            y_start = y - 1;
        } else if (quadrant == 1) {
            x_start = x;
            y_start = y - 1;
        } else if (quadrant == 2) {
            x_start = x;
            y_start = y;
        } else {
            x_start = x - 1;
            y_start = y;
        }

        // extract lon/lat/x/y for interpolation quadrant
        final InterpolationContext context = new InterpolationContext();
        for (int m = 0; m < 2; m++) {
            for (int k = 0; k < 2; k++) {
                final int writeIndex = 2 * m + k;
                final int readIndex = (y_start + m) * rasterWidth + x_start + k;

                context.lons[writeIndex] = longitudes[readIndex];
                context.lats[writeIndex] = latitudes[readIndex];
                context.x[writeIndex] = x_start + k;
                context.y[writeIndex] = y_start + m;
            }
        }

        return context;
    }
}
