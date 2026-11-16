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
package org.esa.snap.dem.dataio.aster;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.ElevationFile;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.core.util.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Holds information about a dem file.
 */
public final class AsterFile extends ElevationFile {

    private final AsterElevationModel demModel;

    private static final String[] ASTERVERSIONS = new String[] {"ASTGTMV003","ASTGTM2"};

    public AsterFile(final AsterElevationModel model, final Path localFile, final ProductReader reader) {
        super(localFile, reader);
        this.demModel = model;
    }

    protected Boolean getRemoteFile() {
        remoteFileExists = false;
        return false;
    }

    protected ElevationTile createTile(final Product product) throws IOException {
        final AsterElevationTile tile = new AsterElevationTile(demModel, product);
        demModel.updateCache(tile);
        return tile;
    }

    protected boolean findLocalFile() {
        if (Files.exists(localFile) && Files.isRegularFile(localFile) && IOHelper.size(localFile) > 0) {
            return true;
        } else {
            final String name = FileUtils.getFilenameWithoutExtension(localFile.getFileName());
            for(String asterVersion : ASTERVERSIONS) {
                // check for versions 3 or 2
                final String versionedName = name.replace("ASTGTM", asterVersion);
                final Path versionedFile = localFile.getParent().resolve(versionedName + ".zip");
                if (Files.exists(versionedFile)) {
                    localFile = localFile.getParent().resolve(versionedName + "_dem.tif");
                    localZipFile = versionedFile;
                    return true;
                } else {
                    // check if unzipped
                    final Path unzipFile = localFile.getParent().resolve(name + "_dem.tif");
                    if (Files.exists(unzipFile)) {
                        localFile = unzipFile;
                        return true;
                    } else {
                        final Path v2UnzipFile = localFile.getParent().resolve(versionedName + "_dem.tif");
                        if (Files.exists(v2UnzipFile)) {
                            localFile = v2UnzipFile;
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected InputStream getZipInputStream(Path dataFile) throws IOException {
        if (!Files.exists(dataFile)) {
            for(String asterVersion : ASTERVERSIONS) {
                final String v2Name = dataFile.getFileName().toString().replace("ASTGTM", asterVersion);
                dataFile = dataFile.getParent().resolve(v2Name);
                if (Files.exists(dataFile)) {
                    break;
                }
            }
        }
        return super.getZipInputStream(dataFile);
    }
}
