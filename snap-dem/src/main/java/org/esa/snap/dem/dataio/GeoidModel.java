/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.dem.dataio;

/**
 * A geoid model: the geoid undulation N, in metres, that converts an orthometric height H to an
 * ellipsoidal (WGS84) height h = H + N.
 */
public interface GeoidModel {

    /**
     * @param lat       latitude in degrees
     * @param lon       longitude in degrees, -180..360
     * @param workspace a 4x4 scratch array, so callers can avoid an allocation per sample
     * @return the geoid undulation in metres
     */
    float getEGM(double lat, double lon, double[][] workspace);

    /** As {@link #getEGM(double, double, double[][])}, allocating the workspace. */
    default float getEGM(double lat, double lon) {
        return getEGM(lat, lon, new double[4][4]);
    }
}
