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
package org.esa.snap.landcover.dataio.globcover;

import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.engine_utilities.util.Settings;
import org.esa.snap.landcover.dataio.AbstractLandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.FileLandCoverModel;
import org.esa.snap.landcover.dataio.LandCoverModel;

import java.io.IOException;
import java.nio.file.Path;

public class GlobCoverModelDescriptor extends AbstractLandCoverModelDescriptor {

    public static final String NAME = "GlobCover";

    private static final Path INSTALL_DIR = Settings.instance().getAuxDataFolder().toAbsolutePath().resolve(
            "LandCover").resolve(NAME);
    private static final Path[] fileList = new Path[]{
            INSTALL_DIR.resolve("globecover_1.tif.zip"),          //todo replace with coordinates
            INSTALL_DIR.resolve("globecover_2.tif.zip"),
            INSTALL_DIR.resolve("globecover_3.tif.zip"),
            INSTALL_DIR.resolve("globecover_4.tif.zip"),
            INSTALL_DIR.resolve("globecover_5.tif.zip"),
            INSTALL_DIR.resolve("globecover_6.tif.zip"),
            INSTALL_DIR.resolve("globecover_7.tif.zip"),
            INSTALL_DIR.resolve("globecover_8.tif.zip"),
            INSTALL_DIR.resolve("globecover_9.tif.zip"),
            INSTALL_DIR.resolve("globecover_10.tif.zip"),
            INSTALL_DIR.resolve("globecover_11.tif.zip"),
            INSTALL_DIR.resolve("globecover_12.tif.zip"),
            INSTALL_DIR.resolve("globecover_13.tif.zip"),
            INSTALL_DIR.resolve("globecover_14.tif.zip"),
            INSTALL_DIR.resolve("globecover_15.tif.zip"),
            INSTALL_DIR.resolve("globecover_16.tif.zip"),
            INSTALL_DIR.resolve("globecover_17.tif.zip"),
            INSTALL_DIR.resolve("globecover_18.tif.zip"),
            INSTALL_DIR.resolve("globecover_19.tif.zip"),
            INSTALL_DIR.resolve("globecover_20.tif.zip"),
            INSTALL_DIR.resolve("globecover_21.tif.zip"),
            INSTALL_DIR.resolve("globecover_22.tif.zip"),
            INSTALL_DIR.resolve("globecover_23.tif.zip"),
            INSTALL_DIR.resolve("globecover_24.tif.zip"),
            INSTALL_DIR.resolve("globecover_25.tif.zip"),
            INSTALL_DIR.resolve("globecover_26.tif.zip"),
            INSTALL_DIR.resolve("globecover_27.tif.zip"),
            INSTALL_DIR.resolve("globecover_28.tif.zip"),
            INSTALL_DIR.resolve("globecover_29.tif.zip"),
            INSTALL_DIR.resolve("globecover_30.tif.zip"),
            INSTALL_DIR.resolve("globecover_31.tif.zip"),
            INSTALL_DIR.resolve("globecover_32.tif.zip"),
            INSTALL_DIR.resolve("globecover_33.tif.zip"),
            INSTALL_DIR.resolve("globecover_34.tif.zip"),
            INSTALL_DIR.resolve("globecover_35.tif.zip"),
            INSTALL_DIR.resolve("globecover_36.tif.zip")
    };

    public GlobCoverModelDescriptor() {
        remotePath = "http://step.esa.int/auxdata/landcover/globcover/";
        name = NAME;
        NO_DATA_VALUE = 230;
        installDir = INSTALL_DIR;

        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(this.getClass());
        colourIndexFile = moduleBasePath.resolve("org/esa/snap/landcover/auxdata/globcover/globcover_index.col");
    }

    @Override
    public LandCoverModel createLandCoverModel(final Resampling resampling) throws IOException {
        return new FileLandCoverModel(this, fileList, resampling);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }
}
