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

package org.esa.snap.core.dataio.geocoding.inverse;

import org.esa.snap.core.datamodel.GeoPos;

import java.util.ArrayList;

class Segment {

    static final int MIN_DIMENSION = 4;

    final int x_min;
    final int x_max;
    final int y_min;
    final int y_max;

    double lon_min;
    double lon_max;
    double lat_min;
    double lat_max;

    boolean containsAntiMeridian;

    Segment(int x_min, int x_max, int y_min, int y_max) {
        this.x_min = x_min;
        this.x_max = x_max;
        this.y_min = y_min;
        this.y_max = y_max;

        lon_min = Double.NaN;
        lon_max = Double.NaN;
        lat_min = Double.NaN;
        lat_max = Double.NaN;

        containsAntiMeridian = false;
    }

    Segment[] split(boolean acrossTrack) {
        final int width = x_max - x_min + 1;
        final int height = y_max - y_min + 1;

        if (width <= MIN_DIMENSION || height <= MIN_DIMENSION) {
            return new Segment[]{this};
        }

        final Segment[] segments = new Segment[2];
        if (acrossTrack) {
            final int y_split = y_min + height / 2;
            segments[0] = new Segment(x_min, x_max, y_min, y_split);
            segments[1] = new Segment(x_min, x_max, y_split + 1, y_max);
        } else {
            final int x_split = x_min + width / 2;
            segments[0] = new Segment(x_min, x_split, y_min, y_max);
            segments[1] = new Segment(x_split + 1, x_max, y_min, y_max);
        }

        return segments;
    }

    Segment[] split_x(int xPos) {
        if (xPos <= x_min || xPos >= x_max){
            throw new IllegalArgumentException("attempt to split outside segment region");
        }

        if (xPos - x_min < MIN_DIMENSION || x_max - xPos < MIN_DIMENSION) {
            return new Segment[] {this};
        }

        final Segment[] segments = new Segment[2];

        segments[0] = new Segment(x_min, xPos - 1, y_min, y_max);
        segments[1] = new Segment(xPos, x_max, y_min, y_max);

        return segments;
    }

    Segment[] split_y(int yPos) {
        if (yPos <= y_min || yPos >= y_max){
            throw new IllegalArgumentException("attempt to split outside segment region");
        }

        if (yPos - y_min < MIN_DIMENSION || y_max - yPos < MIN_DIMENSION) {
            return new Segment[] {this};
        }

        final Segment[] segments = new Segment[2];

        segments[0] = new Segment(x_min, x_max, y_min, yPos - 1);
        segments[1] = new Segment(x_min, x_max, yPos, y_max);

        return segments;
    }

    void calculateGeoPoints(GeoPosCalculator calculator) {
        final ArrayList<GeoPos> geoPosList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            geoPosList.add(new GeoPos());
        }

        calculator.getGeoPos(x_min, y_min, geoPosList.get(0));
        calculator.getGeoPos(x_max, y_min, geoPosList.get(1));
        calculator.getGeoPos(x_max, y_max, geoPosList.get(2));
        calculator.getGeoPos(x_min, y_max, geoPosList.get(3));

        double minItLon = 180.0;
        double maxItLon = -180.0;
        double minItLat = 90.0;
        double maxItLat = -90.0;

        for (int i = 0; i < 4; i++) {
            final GeoPos geoPos = geoPosList.get(i);
            if (geoPos.lon < minItLon) {
                minItLon = geoPos.lon;
            }
            if (geoPos.lon > maxItLon) {
                maxItLon = geoPos.lon;
            }
            if (geoPos.lat < minItLat) {
                minItLat = geoPos.lat;
            }
            if (geoPos.lat > maxItLat) {
                maxItLat = geoPos.lat;
            }
        }

        lon_min = minItLon;
        lon_max = maxItLon;
        lat_min = minItLat;
        lat_max = maxItLat;

        if (Math.abs(maxItLon - minItLon) > 270) {
            containsAntiMeridian = true;
        }

        if (containsAntiMeridian) {
            // we need to re-calculate the longitude boundaries so that lon_min is the largest negative longitude
            // and lon_max is the smallest positive longitude
            minItLon = -180.0;
            maxItLon = 180.0;
            for (int i = 0; i < 4; i++) {
                final GeoPos geoPos = geoPosList.get(i);

                if (geoPos.lon > 0) {
                    if (geoPos.lon < maxItLon) {
                        maxItLon = geoPos.lon;
                    }
                } else {
                    if (geoPos.lon > minItLon) {
                        minItLon = geoPos.lon;
                    }
                }
            }

            lon_min = minItLon;
            lon_max = maxItLon;
        }
    }

    boolean isInside(double lon, double lat) {
        if (containsAntiMeridian) {
            if (lon >= 0.0) {
                // in this case lon_max is the lower bound of the geo-region as it wraps around anti-meridian
                return lon >= lon_max && lat >= lat_min && lat <= lat_max;
            } else {
                // in this case lon_min is the upper bound of the geo-region as it wraps around anti-meridian
                return lon <= lon_min && lat >= lat_min && lat <= lat_max;
            }
        } else {
            return lon >= lon_min && lon <= lon_max && lat >= lat_min && lat <= lat_max;
        }
    }

    int getWidth() {
        return x_max - x_min + 1;
    }

    int getHeight() {
        return y_max - y_min + 1;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Segment clone() {
        final Segment clone = new Segment(x_min, x_max, y_min, y_max);
        clone.lon_min = lon_min;
        clone.lon_max = lon_max;
        clone.lat_min = lat_min;
        clone.lat_max = lat_max;
        clone.containsAntiMeridian = containsAntiMeridian;
        return clone;
    }
}
