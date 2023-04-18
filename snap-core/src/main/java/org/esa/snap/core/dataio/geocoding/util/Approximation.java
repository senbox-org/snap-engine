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

import org.esa.snap.core.util.math.FXYSum;

public class Approximation {

    private final FXYSum fX;
    private final FXYSum fY;
    private final double centerLat;
    private final double centerLon;
    private final double minSquareDistance;

    public Approximation(FXYSum fX, FXYSum fY, double centerLat, double centerLon, double minSquareDistance) {
        this.fX = fX;
        this.fY = fY;
        this.centerLat = centerLat;
        this.centerLon = centerLon;
        this.minSquareDistance = minSquareDistance;
    }

    public final FXYSum getFX() {
        return fX;
    }

    public final FXYSum getFY() {
        return fY;
    }

    public double getCenterLat() {
        return centerLat;
    }

    public double getCenterLon() {
        return centerLon;
    }

    public double getMinSquareDistance() {
        return minSquareDistance;
    }

    /**
     * Computes the square distance to the given geographical coordinate.
     *
     * @param lat the latitude value
     * @param lon the longitude value
     * @return the square distance
     */
    public final double getSquareDistance(double lat, double lon) {
        final double dx = lon - centerLon;
        final double dy = lat - centerLat;
        return dx * dx + dy * dy;
    }
}
