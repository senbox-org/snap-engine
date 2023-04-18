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

public class PixelForward implements ForwardCoding {

    public static final String KEY = "FWD_PIXEL";

    private int sceneWidth;
    private int sceneHeight;
    private double[] longitudes;
    private double[] latitudes;

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        geoPos.setInvalid();
        if (!pixelPos.isValid()) {
            return geoPos;
        }

        final double x = pixelPos.getX();
        final double y = pixelPos.getY();

        if (x < 0 || x > sceneWidth || y < 0 || y > sceneHeight) {
            return geoPos;
        }

        int xf = (int) Math.floor(x);
        int yf = (int) Math.floor(y);
        if (xf == sceneWidth) {
            xf--;
        }
        if (yf == sceneHeight) {
            yf--;
        }
        final int index = yf * sceneWidth + xf;
        geoPos.setLocation(latitudes[index], longitudes[index]);
        return geoPos;
    }

    @Override
    public void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations) {
        sceneWidth = geoRaster.getSceneWidth();
        sceneHeight = geoRaster.getSceneHeight();
        longitudes = geoRaster.getLongitudes();
        latitudes = geoRaster.getLatitudes();
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
        final PixelForward clone = new PixelForward();

        clone.longitudes = longitudes;
        clone.latitudes = latitudes;
        clone.sceneWidth = sceneWidth;
        clone.sceneHeight = sceneHeight;

        return clone;
    }

    public static class Plugin implements ForwardPlugin {

        @Override
        public ForwardCoding create() {
            return new PixelForward();
        }
    }
}
