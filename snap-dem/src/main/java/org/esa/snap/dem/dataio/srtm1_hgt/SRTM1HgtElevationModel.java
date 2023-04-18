/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio.srtm1_hgt;

import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.BaseElevationModel;
import org.esa.snap.core.dataop.dem.ElevationFile;
import org.esa.snap.core.dataop.resamp.Resampling;

import java.io.File;
import java.io.IOException;

public class SRTM1HgtElevationModel extends BaseElevationModel {

    private static final ProductReaderPlugIn productReaderPlugIn = getReaderPlugIn(SRTMHGTReaderPlugIn.FORMAT_NAMES[0]);

    public SRTM1HgtElevationModel(final SRTM1HgtElevationModelDescriptor descriptor, final Resampling resamplingMethod) throws IOException {
        super(descriptor, resamplingMethod);
    }

    @Override
    public double getIndexX(final GeoPos geoPos) {
        return ((geoPos.lon + 180.0) * DEGREE_RES_BY_NUM_PIXELS_PER_TILE_X_inv);
    }

    @Override
    public double getIndexY(final GeoPos geoPos) {
        return ((60.0 - geoPos.lat) * DEGREE_RES_BY_NUM_PIXELS_PER_TILE_Y_inv);
    }

    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos) {
        final double pixelLat = (RASTER_HEIGHT - pixelPos.y) * DEGREE_RES_BY_NUM_PIXELS_PER_TILE_Y - 60.0;
        final double pixelLon = pixelPos.x * DEGREE_RES_BY_NUM_PIXELS_PER_TILE_X - 180.0;
        return new GeoPos(pixelLat, pixelLon);
    }

    @Override
    protected void createElevationFile(final ElevationFile[][] elevationFiles,
                                       final int x, final int y, final File demInstallDir) {
        final int minLon = x * DEGREE_RES - 180;
        final int minLat = y * DEGREE_RES - 60;
        final String fileName = createTileFilename(minLat, minLon);
        final File localFile = new File(demInstallDir, fileName);
        elevationFiles[x][NUM_Y_TILES - 1 - y] = new SRTM1HgtFile(this, localFile, productReaderPlugIn.createReaderInstance());
    }

    private String createTileFilename(int minLat, int minLon) {
        final StringBuilder name = new StringBuilder();
        name.append(minLat < 0 ? "S" : "N");
        String latString = String.valueOf(Math.abs(minLat));
        while (latString.length() < 2) {
            latString = '0' + latString;
        }
        name.append(latString);

        name.append(minLon < 0 ? "W" : "E");
        String lonString = String.valueOf(Math.abs(minLon));
        while (lonString.length() < 3) {
            lonString = '0' + lonString;
        }
        name.append(lonString);
        name.append(".SRTMGL1.hgt.zip");

        return name.toString();
    }

}
