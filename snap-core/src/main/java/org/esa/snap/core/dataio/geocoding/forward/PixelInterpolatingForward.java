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

package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.ForwardCoding;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.util.math.MathUtils;

public class PixelInterpolatingForward implements ForwardCoding {

    public static final String KEY = "FWD_PIXEL_INTERPOLATING";

    private int sceneWidth;
    private int sceneHeight;
    private double[] longitudes;
    private double[] latitudes;
    private LonInterpolator lonInterpolator;

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        geoPos.setInvalid();
        if (!pixelPos.isValid()) {
            return geoPos;
        }

        double x = pixelPos.getX();
        double y = pixelPos.getY();
        if (x < 0 || x > sceneWidth || y < 0 || y > sceneHeight) {
            return geoPos;
        }

        int x0 = (int) Math.floor(x);
        if (x0 == sceneWidth) {
            x0 -= 1;
            x -= 1;
        }
        int y0 = (int) Math.floor(y);
        if (y == sceneHeight) {
            y0 -= 1;
            y -= 1;
        }

        double delta = x - x0;
        if (x0 > 0 && delta < 0.5 || x0 == sceneWidth - 1) {
            x0 -= 1;
        }

        delta = y - y0;
        if (y0 > 0 && delta < 0.5 || y0 == sceneHeight - 1) {
            y0 -= 1;
        }

        final double wx = x - (x0 + 0.5);
        final double wy = y - (y0 + 0.5);

        InterpolationContext context = getInterpolationContext(longitudes, x0, y0);
        final double lon = lonInterpolator.interpolate(wx, wy, context);

        context = getInterpolationContext(latitudes, x0, y0);
        final double lat = MathUtils.interpolate2D(wx, wy, context.d00, context.d10, context.d01, context.d11);

        geoPos.lon = lon;
        geoPos.lat = lat;

        return geoPos;
    }

    @Override
    public void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations) {
        sceneWidth = geoRaster.getSceneWidth();
        sceneHeight = geoRaster.getSceneHeight();
        longitudes = geoRaster.getLongitudes();
        latitudes = geoRaster.getLatitudes();

        if (containsAntiMeridian) {
            lonInterpolator = new AntiMeridianLonInterpolator();
        } else {
            lonInterpolator = new StandardLonInterpolator();
        }
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public void dispose() {
        longitudes = null;
        latitudes = null;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ForwardCoding clone() {
        final PixelInterpolatingForward clone = new PixelInterpolatingForward();

        clone.longitudes = longitudes;
        clone.latitudes = latitudes;
        clone.sceneWidth = sceneWidth;
        clone.sceneHeight = sceneHeight;

        clone.lonInterpolator = lonInterpolator;

        return clone;
    }

    private InterpolationContext getInterpolationContext(double[] data, int x0, int y0) {
        final InterpolationContext context = new InterpolationContext();

        context.d00 = data[(y0 * sceneWidth) + x0];
        context.d10 = data[(y0 * sceneWidth) + x0 + 1];
        context.d01 = data[((y0 + 1) * sceneWidth) + x0];
        context.d11 = data[((y0 + 1) * sceneWidth) + x0 + 1];

        return context;
    }

    static private class InterpolationContext {
        double d00;
        double d10;
        double d01;
        double d11;
    }

    interface LonInterpolator {
        double interpolate(double wx, double wy, InterpolationContext context);
    }

    static class StandardLonInterpolator implements LonInterpolator {

        @Override
        public double interpolate(double wx, double wy, InterpolationContext context) {
            return MathUtils.interpolate2D(wx, wy, context.d00, context.d10, context.d01, context.d11);
        }
    }

    static class AntiMeridianLonInterpolator implements LonInterpolator {

        private static final double OFFSET = 360.0;
        private static final double THRESH = 180.0;

        @Override
        public double interpolate(double wx, double wy, InterpolationContext context) {
            boolean containsAntiMeridian = false;

            if (Math.abs(context.d10 - context.d00) > THRESH) {
                containsAntiMeridian = true;
            } else if (Math.abs(context.d11 - context.d01) > THRESH) {
                containsAntiMeridian = true;
            } else if (Math.abs(context.d11 - context.d10) > THRESH) {
                containsAntiMeridian = true;
            } else if (Math.abs(context.d01 - context.d00) > THRESH) {
                containsAntiMeridian = true;
            }

            if (containsAntiMeridian) {
                if (context.d00 < 0.0) {
                    context.d00 += OFFSET;
                }
                if (context.d10 < 0.0) {
                    context.d10 += OFFSET;
                }
                if (context.d01 < 0.0) {
                    context.d01 += OFFSET;
                }
                if (context.d11 < 0.0) {
                    context.d11 += OFFSET;
                }
            }

            double lon = MathUtils.interpolate2D(wx, wy, context.d00, context.d10, context.d01, context.d11);
            if (lon > THRESH) {
                lon -= OFFSET;
            }
            return lon;
        }
    }

    public static class Plugin implements ForwardPlugin {
        @Override
        public ForwardCoding create() {
            return new PixelInterpolatingForward();
        }
    }
}
