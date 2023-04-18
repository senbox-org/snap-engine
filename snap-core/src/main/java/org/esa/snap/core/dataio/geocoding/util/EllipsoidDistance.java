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
import org.opengis.referencing.datum.Ellipsoid;

public class EllipsoidDistance implements DistanceMeasure {

    private final double lon_rad;
    private final double lat_rad;
    private final double a;
    private final double f;

    public EllipsoidDistance(double lon, double lat, Ellipsoid ellipsoid) {
        this.lon_rad = Math.toRadians(lon);
        this.lat_rad = Math.toRadians(lat);
        this.a = ellipsoid.getSemiMajorAxis();
        this.f = 1.0 / ellipsoid.getInverseFlattening();
    }

    /**
     * Calculates the distance from the location passed in to the location supplied in the constructor.
     * @param lon longitude in decimal degrees
     * @param lat latitude in decimal degrees
     * @return the distance on the surface of the ellipsoid in meters
     */
    @Override
    public double distance(double lon, double lat) {
        final double lon_rad = Math.toRadians(lon);
        final double lat_rad = Math.toRadians(lat);

        final double F = 0.5 * (this.lat_rad + lat_rad);
        final double G = 0.5 * (this.lat_rad - lat_rad);
        final double l = 0.5 * (this.lon_rad - lon_rad);

        final double sin_f = Math.sin(F);
        final double cos_f = Math.cos(F);
        final double sin_g = Math.sin(G);
        final double cos_g = Math.cos(G);
        final double sin_l = Math.sin(l);
        final double cos_l = Math.cos(l);

        final double sin_f_sq = sin_f * sin_f;
        final double cos_f_sq = cos_f * cos_f;
        final double sin_q_sq = sin_g * sin_g;
        final double cos_g_sq = cos_g * cos_g;
        final double sin_l_sq = sin_l * sin_l;
        final double cos_l_sq = cos_l * cos_l;

        final double S = sin_q_sq * cos_l_sq + cos_f_sq * sin_l_sq;
        final double C = cos_g_sq * cos_l_sq + sin_f_sq * sin_l_sq;

        final double omega = Math.atan(Math.sqrt(S / C));
        if (omega == 0.0) {
            return 0.0;
        }

        final double D = 2.0 * omega * a;
        final double three_R = 3.0 * Math.sqrt(S * C) / omega;

        final double H_1 = (three_R - 1.0) / (2.0 * C);
        final double H_2 = (three_R + 1.0) / (2.0 * S);

        return D * (1.0 + f * (H_1 * sin_f_sq * cos_g_sq - H_2 * cos_f_sq * sin_q_sq));
    }
}
