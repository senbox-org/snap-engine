/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package gov.nasa.gsfc.seadas.dataio;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.util.math.MathUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * Created by dshea on 11/25/14.
 */
public class BowtiePixelScanGeoCoding implements GeoCoding {
    private static final float EPS = 0.04F; // used by quad-tree search
    private static final boolean TRACE = false;
    private static final double D2R = Math.PI / 180.0;

    private int width;
    private int height;
    private float[] lats;
    private float[] lons;

    private Boolean crossingMeridianAt180;


    private static class Result {

        public static final float INVALID = Float.MAX_VALUE;

        private int x;
        private int y;
        private double delta;

        private Result() {
            delta = INVALID;
        }

        public final boolean update(final int x, final int y, final double delta) {
            final boolean b = delta < this.delta;
            if (b) {
                this.x = x;
                this.y = y;
                this.delta = delta;
            }
            return b;
        }

        @Override
        public String toString() {
            return "Result[" + x + ", " + y + ", " + delta + "]";
        }
    }

    public BowtiePixelScanGeoCoding(float[] lats, float[] lons, int width, int height) {
        this.lats = lats;
        this.lons = lons;
        this.width = width;
        this.height = height;
    }

    /**
     * Checks whether or not the longitudes of this geo-coding cross the +/- 180 degree meridian.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean isCrossingMeridianAt180() {

        if (crossingMeridianAt180 == null) {
            crossingMeridianAt180 = false;

            GeoPos geoPos1 = new GeoPos();
            GeoPos geoPos2 = new GeoPos();
            for(int y=0; y<height; y++) {
                geoPos1.setInvalid();
                geoPos2.setInvalid();
                for(int x=0; x<width/2; x++) {
                    getGeoPosInternal(x, y, geoPos1);
                    if(geoPos1.isValid())
                        break;
                }
                if(!geoPos1.isValid())
                    continue;
                for(int x=width-1; x>width/2; x--) {
                    getGeoPosInternal(x, y, geoPos2);
                    if(geoPos2.isValid())
                        break;
                }
                if(!geoPos2.isValid())
                    continue;

                if(geoPos1.lon > geoPos2.lon) {
                    crossingMeridianAt180 = true;
                    break;
                }
            }
        }
        return crossingMeridianAt180;
    }

    @Override
    public boolean canGetPixelPos() {
        return true;
    }

    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    private void getGeoPosInternal(int pixelX, int pixelY, GeoPos geoPos) {
        if(pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
            int i = pixelY*width + pixelX;
            if(lats[i] >= -90 && lats[i] <= 90 && lons[i] >= -180 && lons[i] <= 180) {
                geoPos.setLocation(lats[i], lons[i]);
                return;
            }
        }
        geoPos.setInvalid();
    }

    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        if (pixelPos.isValid()) {
            int x0 = (int) Math.floor(pixelPos.getX());
            int y0 = (int) Math.floor(pixelPos.getY());
            if (x0 >= 0 && x0 < width && y0 >= 0 && y0 < height) {
                if (x0 > 0 && pixelPos.x - x0 < 0.5f || x0 == width - 1) {
                    x0 -= 1;
                }
                if (y0 > 0 && pixelPos.y - y0 < 0.5f || y0 == height - 1) {
                    y0 -= 1;
                }
                final double wx = pixelPos.x - (x0 + 0.5f);
                final double wy = pixelPos.y - (y0 + 0.5f);

                GeoPos d00 = new GeoPos();
                GeoPos d10 = new GeoPos();
                GeoPos d01 = new GeoPos();
                GeoPos d11 = new GeoPos();

                getGeoPosInternal(x0,   y0,   d00);
                getGeoPosInternal(x0+1, y0,   d10);
                getGeoPosInternal(x0,   y0+1, d01);
                getGeoPosInternal(x0 + 1, y0 + 1, d11);

                if(d00.isValid() && d10.isValid() && d01.isValid() && d11.isValid()) {
                    double lat = MathUtils.interpolate2D(wx, wy, d00.lat, d10.lat, d01.lat, d11.lat);
                    double lon = GeoCodingFactory.interpolateLon(wx, wy, d00.lon, d10.lon, d01.lon, d11.lon);
                    geoPos.setLocation(lat, lon);
                    return geoPos;
                }
            }
        }
        geoPos.setInvalid();
        return geoPos;
    }

    private boolean quadTreeRecursion(final int depth,
                                      final double lat, final double lon,
                                      final int i, final int j,
                                      final int w, final int h,
                                      final Result result) {
        int w2 = w >> 1;
        int h2 = h >> 1;
        final int i2 = i + w2;
        final int j2 = j + h2;
        final int w2r = w - w2;
        final int h2r = h - h2;

        if (w2 < 2) {
            w2 = 2;
        }

        if (h2 < 2) {
            h2 = 2;
        }

        final boolean b1 = quadTreeSearch(depth + 1, lat, lon, i, j, w2, h2, result);
        final boolean b2 = quadTreeSearch(depth + 1, lat, lon, i, j2, w2, h2r, result);
        final boolean b3 = quadTreeSearch(depth + 1, lat, lon, i2, j, w2r, h2, result);
        final boolean b4 = quadTreeSearch(depth + 1, lat, lon, i2, j2, w2r, h2r, result);

        return b1 || b2 || b3 || b4;
    }

    private static double min(final double a, final double b) {
        return (a <= b) ? a : b;
    }

    private static double max(final double a, final double b) {
        return (a >= b) ? a : b;
    }

    private static double sq(final double dx, final double dy) {
        return dx * dx + dy * dy;
    }

    static double getNegativeLonMax(double lon0, double lon1, double lon2, double lon3) {
        double lonMax;
        lonMax = -180.0f;
        if (lon0 < 0.0f) {
            lonMax = lon0;
        }
        if (lon1 < 0.0f) {
            lonMax = max(lon1, lonMax);
        }
        if (lon2 < 0.0f) {
            lonMax = max(lon2, lonMax);
        }
        if (lon3 < 0.0f) {
            lonMax = max(lon3, lonMax);
        }
        return lonMax;
    }

    static double getPositiveLonMin(double lon0, double lon1, double lon2, double lon3) {
        double lonMin;
        lonMin = 180.0f;
        if (lon0 >= 0.0f) {
            lonMin = lon0;
        }
        if (lon1 >= 0.0f) {
            lonMin = min(lon1, lonMin);
        }
        if (lon2 >= 0.0f) {
            lonMin = min(lon2, lonMin);
        }
        if (lon3 >= 0.0f) {
            lonMin = min(lon3, lonMin);
        }
        return lonMin;
    }

    static boolean isCrossingMeridianInsideQuad(boolean crossingMeridianInsideProduct, double lon0, double lon1,
                                                double lon2, double lon3) {
        if (!crossingMeridianInsideProduct) {
            return false;
        }
        double lonMin = min(lon0, min(lon1, min(lon2, lon3)));
        double lonMax = max(lon0, max(lon1, max(lon2, lon3)));

        return Math.abs(lonMax - lonMin) > 180.0;
    }

    private boolean quadTreeSearch(final int depth,
                                   final double lat,
                                   final double lon,
                                   final int x, final int y,
                                   final int w, final int h,
                                   final Result result) {
        if (w < 2 || h < 2) {
            return false;
        }

        final int x1 = x;
        final int x2 = x1 + w - 1;

        final int y1 = y;
        final int y2 = y1 + h - 1;

        GeoPos geoPos = new GeoPos();
        getGeoPosInternal(x1, y1, geoPos);
        final double lat0 = geoPos.lat;
        double lon0 = geoPos.lon;
        getGeoPosInternal(x1, y2, geoPos);
        final double lat1 = geoPos.lat;
        double lon1 = geoPos.lon;
        getGeoPosInternal(x2, y1, geoPos);
        final double lat2 = geoPos.lat;
        double lon2 = geoPos.lon;
        getGeoPosInternal(x2, y2, geoPos);
        final double lat3 = geoPos.lat;
        double lon3 = geoPos.lon;

        final float epsL = EPS;
        final double latMin = min(lat0, min(lat1, min(lat2, lat3))) - epsL;
        final double latMax = max(lat0, max(lat1, max(lat2, lat3))) + epsL;
        double lonMin;
        double lonMax;
        if (isCrossingMeridianInsideQuad(isCrossingMeridianAt180(), lon0, lon1, lon2, lon3)) {
            final double signumLon = Math.signum(lon);
            if (signumLon > 0f) {
                // position is in a region with positive longitudes, so cut negative longitudes from quad area
                lonMax = 180.0f;
                lonMin = getPositiveLonMin(lon0, lon1, lon2, lon3);
            } else {
                // position is in a region with negative longitudes, so cut positive longitudes from quad area
                lonMin = -180.0f;
                lonMax = getNegativeLonMax(lon0, lon1, lon2, lon3);
            }
        } else {
            lonMin = min(lon0, min(lon1, min(lon2, lon3))) - epsL;
            lonMax = max(lon0, max(lon1, max(lon2, lon3))) + epsL;
        }

        boolean pixelFound = false;
        final boolean definitelyOutside = lat < latMin || lat > latMax || lon < lonMin || lon > lonMax;
        if (!definitelyOutside) {
            if (w == 2 && h == 2) {
                final double f = Math.cos(lat * D2R);
                if (result.update(x1, y1, sq(lat - lat0, f * (lon - lon0)))) {
                    pixelFound = true;
                }
                if (result.update(x1, y2, sq(lat - lat1, f * (lon - lon1)))) {
                    pixelFound = true;
                }
                if (result.update(x2, y1, sq(lat - lat2, f * (lon - lon2)))) {
                    pixelFound = true;
                }
                if (result.update(x2, y2, sq(lat - lat3, f * (lon - lon3)))) {
                    pixelFound = true;
                }
            } else if (w >= 2 && h >= 2) {
                pixelFound = quadTreeRecursion(depth, lat, lon, x1, y1, w, h, result);
            }
        }

        if (TRACE) {
            for (int i = 0; i < depth; i++) {
                System.out.print("  ");
            }
            System.out.println(
                    depth + ": (" + x + "," + y + ") (" + w + "," + h + ") " + definitelyOutside + "  " + pixelFound);
        }
        return pixelFound;
    }


    /**
     * Returns the pixel co-ordinates as x/y for a given geographical position given as lat/lon.
     * This algorithm
     *
     * @param geoPos   the geographical position as lat/lon.
     * @param pixelPos the return value
     */
    public void getPixelPosUsingQuadTreeSearch(final GeoPos geoPos, PixelPos pixelPos) {

        final Result result = new Result();
        boolean pixelFound = quadTreeSearch(0,
                                            geoPos.lat, geoPos.lon,
                                            0, 0,
                                            width,
                                            height,
                                            result);

        if (pixelFound) {
            pixelPos.setLocation(result.x + 0.5f, result.y + 0.5f);
        } else {
            pixelPos.setInvalid();
        }
    }

    @Override
    public PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        pixelPos.setInvalid();
        if (geoPos.isValid()) {
            getPixelPosUsingQuadTreeSearch(geoPos, pixelPos);
        }
        return pixelPos;
    }


    @Override
    public Datum getDatum() {
        return null;
    }

    @Override
    public void dispose() {

    }

    @Override
    public CoordinateReferenceSystem getImageCRS() {
        return null;
    }

    @Override
    public CoordinateReferenceSystem getMapCRS() {
        return null;
    }

    @Override
    public CoordinateReferenceSystem getGeoCRS() {
        return null;
    }

    @Override
    public MathTransform getImageToMapTransform() {
        return null;
    }
}
