/*
 * Copyright (C) 2018 Skywatch. https://www.skywatch.com
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
package org.esa.snap.landcover.dataio.AAFC;

import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.engine_utilities.util.Settings;
import org.esa.snap.landcover.dataio.AbstractLandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.LandCoverModel;

import java.io.IOException;
import java.nio.file.Path;

public class AESBEOS2017CropModelDescriptor extends AbstractLandCoverModelDescriptor {

    public static final String NAME = "AAFC Canada 2017 Crop";

    private static final Path INSTALL_DIR = Settings.instance().getAuxDataFolder().toAbsolutePath().resolve(
            "LandCover").resolve("AAFC").resolve("AESB_EOS_Crop_2017");
    private static final Path[] fileList = new Path[]{
            INSTALL_DIR.resolve("aci_2017_ab.zip"),
            INSTALL_DIR.resolve("aci_2017_bc.zip"),
            INSTALL_DIR.resolve("aci_2017_mb.zip"),
            INSTALL_DIR.resolve("aci_2017_nb.zip"),
            INSTALL_DIR.resolve("aci_2017_nl.zip"),
            INSTALL_DIR.resolve("aci_2017_ns.zip"),
            INSTALL_DIR.resolve("aci_2017_on.zip"),
            INSTALL_DIR.resolve("aci_2017_pe.zip"),
            INSTALL_DIR.resolve("aci_2017_qc.zip"),
            INSTALL_DIR.resolve("aci_2017_sk.zip")
    };

    public AESBEOS2017CropModelDescriptor() {
        remotePath = "http://step.esa.int/auxdata/landcover/AAFC/AESB_EOS_Crop_2017/";
        name = NAME;
        NO_DATA_VALUE = -9999d;
        installDir = INSTALL_DIR;
        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(this.getClass());
        colourIndexFile = moduleBasePath.resolve("org/esa/snap/landcover/auxdata/aafc/aafc_crop_index.col");
    }

    @Override
    public synchronized boolean installFiles() {
        installMetadata();

        if (installDir != null) {
            isInstalled = true;
        }
        return isInstalled;
    }

    @Override
    public LandCoverModel createLandCoverModel(final Resampling resampling) throws IOException {
        return new FileLandCoverProvincialModel(this, fileList, resampling);
    }

    @Override
    public String getGrouping() {
        return "AAFC Crop Index";
    }
}
