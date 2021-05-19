/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio.srtm3_geotiff;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.ElevationFile;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.engine_utilities.util.Settings;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds information about a dem file.
 */
public final class SRTM3GeoTiffFile extends ElevationFile {

    private final SRTM3GeoTiffElevationModel demModel;

    private static final String remoteHTTP1 = "https://download.esa.int/step/auxdata/dem/SRTM90/tiff/";
    
    private static String remoteHTTP = Settings.instance().get("DEM.srtm3GeoTiffDEM_HTTP", remoteHTTP1);

    private static final Logger logger = Logger.getLogger(SRTM3GeoTiffFile.class.getName());
    static {
        // if old property files still contain old bucket
        if(remoteHTTP.startsWith("http://srtm.csi.cgiar.org") || remoteHTTP.startsWith("http://cgiar-csi-srtm")) {
            remoteHTTP = remoteHTTP1;
        }
    }

    public SRTM3GeoTiffFile(final SRTM3GeoTiffElevationModel model, final File localFile, final ProductReader reader) {
        super(localFile, reader);
        this.demModel = model;
    }

    protected ElevationTile createTile(final Product product) throws IOException {
        final SRTM3GeoTiffElevationTile tile = new SRTM3GeoTiffElevationTile(demModel, product);
        demModel.updateCache(tile);
        return tile;
    }

    protected Boolean getRemoteFile() {
        try {
            boolean found = getRemoteHttpFile(remoteHTTP);
            if (!found) {
                logger.log(Level.WARNING, localZipFile + " not found at  " + remoteHTTP + "trying " + remoteHTTP1);
                found = getRemoteHttpFile(remoteHTTP1);
            }
            return found;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to download " + localZipFile + " from " + remoteHTTP + " or " + remoteHTTP1, e);
            try {
                return getRemoteHttpFile(remoteHTTP1);
            } catch (Exception e2) {
                logger.log(Level.SEVERE, "Unable to download " + localZipFile + " from " + remoteHTTP1, e2);
                return false;
            }
        }
    }
}
