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
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.TiePointGrid;

public class TiePointBilinearForward extends TiePointForward {

    public static final String KEY = "FWD_TIE_POINT_BILINEAR";

    private TiePointGrid lonGrid;
    private TiePointGrid latGrid;
    private int sceneWidth;
    private int sceneHeight;

    TiePointBilinearForward() {
        lonGrid = null;
        latGrid = null;
    }

    @Override
    public void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations) {
        int discontinuity = containsAntiMeridian ? TiePointGrid.DISCONT_AT_180 : TiePointGrid.DISCONT_NONE;

        lonGrid = new TiePointGrid("lon", geoRaster.getRasterWidth(), geoRaster.getRasterHeight(),
                                   geoRaster.getOffsetX(), geoRaster.getOffsetY(),
                                   geoRaster.getSubsamplingX(), geoRaster.getSubsamplingY(),
                                   RasterUtils.toFloat(geoRaster.getLongitudes()), discontinuity);

        latGrid = new TiePointGrid("lat", geoRaster.getRasterWidth(), geoRaster.getRasterHeight(),
                                   geoRaster.getOffsetX(), geoRaster.getOffsetY(),
                                   geoRaster.getSubsamplingX(), geoRaster.getSubsamplingY(),
                                   RasterUtils.toFloat(geoRaster.getLatitudes()));

        checkGrids(lonGrid, latGrid);

        this.sceneWidth = geoRaster.getSceneWidth();
        this.sceneHeight = geoRaster.getSceneHeight();
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        if (pixelPos.x < 0 || pixelPos.x > sceneWidth
            || pixelPos.y < 0 || pixelPos.y > sceneHeight) {
            geoPos.setInvalid();
        } else {
            geoPos.lat = latGrid.getPixelDouble(pixelPos.x, pixelPos.y);
            geoPos.lon = lonGrid.getPixelDouble(pixelPos.x, pixelPos.y);
        }
        return geoPos;
    }

    @Override
    public void dispose() {
        lonGrid = null;
        latGrid = null;
    }

    @Override
    public ForwardCoding clone() {
        final TiePointBilinearForward clone = new TiePointBilinearForward();

        clone.lonGrid = lonGrid.cloneTiePointGrid();
        clone.latGrid = latGrid.cloneTiePointGrid();
        clone.sceneWidth = sceneWidth;
        clone.sceneHeight = sceneHeight;

        return clone;
    }

    public static class Plugin implements ForwardPlugin{
        @Override
        public ForwardCoding create() {
            return new TiePointBilinearForward();
        }
    }
}
