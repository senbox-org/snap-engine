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

class Segment {

    public static final int MIN_DIMENSION = 4;

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

    // @todo 1 tb/tb think about testability here 2021-03-09
    void calculateGeoPoints(GeoPosCalculator calculator) {
        final GeoPos ul = new GeoPos();
        final GeoPos ur = new GeoPos();
        final GeoPos lr = new GeoPos();
        final GeoPos ll = new GeoPos();

        calculator.getGeoPos(x_min, y_min, ul);
        calculator.getGeoPos(x_max, y_min, ur);
        calculator.getGeoPos(x_max, y_max, lr);
        calculator.getGeoPos(x_min, y_max, ll);

        lon_min = Math.min(ul.lon, Math.min(ur.lon, Math.min(lr.lon, ll.lon)));
        lon_max = Math.max(ul.lon, Math.max(ur.lon, Math.max(lr.lon, ll.lon)));

        lat_min = Math.min(ul.lat, Math.min(ur.lat, Math.min(lr.lat, ll.lat)));
        lat_max = Math.max(ul.lat, Math.max(ur.lat, Math.max(lr.lat, ll.lat)));

        // @todo 1 tb/tb check if this constant is OK - we're not comparing neighbouring pixels here 2021-03-11
        if (Math.abs(lon_max - lon_min) > 270) {
            containsAntiMeridian = true;
        }
    }

    void printWktBoundingRect() {
        final StringBuilder builder = new StringBuilder();
        if (containsAntiMeridian) {
            builder.append("MULTIPOLYGON(((");
            builder.append("-180 " + lat_max + ",");
            builder.append(lon_min + " " + lat_max + ",");
            builder.append(lon_min + " " + lat_min + ",");
            builder.append("-180 " + lat_min + ",");
            builder.append("-180 " + lat_max);
            builder.append(")),((");
            builder.append(lon_max + " " + lat_max + ",");
            builder.append("180 " + lat_max + ",");
            builder.append("180 " + lat_min + ",");
            builder.append(lon_max + " " + lat_min + ",");
            builder.append(lon_max + " " + lat_max);
            builder.append(")))");
        } else {
            builder.append("POLYGON((");
            builder.append(lon_min + " " + lat_max + ",");
            builder.append(lon_max + " " + lat_max + ",");
            builder.append(lon_max + " " + lat_min + ",");
            builder.append(lon_min + " " + lat_min + ",");
            builder.append(lon_min + " " + lat_max);
            builder.append("))");
        }

        System.out.println(builder.toString());
    }

    void printBounds(GeoPosCalculator calc) {
        final StringBuilder builder = new StringBuilder();
        final int step = 30;
        final GeoPos geoPos = new GeoPos();

        builder.append("MULTIPOINT(");

        // upper
        for (int x = x_min; x < x_max; x+= step) {
            calc.getGeoPos(x, y_min, geoPos);
            builder.append(geoPos.lon + " " + geoPos.lat + ",");
        }
        // right
        for (int y = y_min; y < y_max; y+= step) {
            calc.getGeoPos(x_max, y, geoPos);
            builder.append(geoPos.lon + " " + geoPos.lat + ",");
        }
        // lower
        for (int x = x_max; x > x_min; x-= step) {
            calc.getGeoPos(x, y_max, geoPos);
            builder.append(geoPos.lon + " " + geoPos.lat + ",");
        }
        // left
        for (int y = y_max; y > y_min; y-= step) {
            calc.getGeoPos(x_min, y, geoPos);
            builder.append(geoPos.lon + " " + geoPos.lat + ",");
        }

        builder.deleteCharAt(builder.length() - 1);
        builder.append(")");

        System.out.println(builder.toString());
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
}
