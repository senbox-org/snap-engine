/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.landcover.dataio.ESRILULC;

import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.engine_utilities.util.Settings;
import org.esa.snap.landcover.dataio.AbstractLandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.LandCoverModel;
import org.esa.snap.landcover.dataio.StacLandCoverModel;

import java.io.File;
import java.nio.file.Path;

public class ESRILULC2020ModelDescriptor extends AbstractLandCoverModelDescriptor {

    public static final String NAME = "ESRILULC2020";

    private static final File INSTALL_DIR = new File(Settings.instance().getAuxDataFolder().getAbsolutePath(),
            "LandCover" + File.separator + NAME);

    public ESRILULC2020ModelDescriptor() {
        name = NAME;
        NO_DATA_VALUE = 230;
        installDir = INSTALL_DIR;
        remotePath = "https://planetarycomputer.microsoft.com/api/stac/v1";

        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(this.getClass());
        colourIndexFile = moduleBasePath.resolve("org/esa/snap/landcover/auxdata/esri2020/esri2020_index.col");
    }

    @Override
    public LandCoverModel createLandCoverModel(final Resampling resampling) {
        return new StacLandCoverModel(this, resampling);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }
}
