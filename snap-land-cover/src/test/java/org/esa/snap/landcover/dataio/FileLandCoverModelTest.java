/*
 * Copyright (C) 2026 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.landcover.dataio;

import org.esa.snap.core.dataop.resamp.Resampling;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FileLandCoverModelTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test

    public void ensureLoadedFailsForMissingAuxdata() throws Exception {
        final File missingFile = new File(temporaryFolder.getRoot(), "missing-land-cover.zip");
        final MissingAuxdataDescriptor descriptor = new MissingAuxdataDescriptor(missingFile);
        final FileLandCoverModel model = (FileLandCoverModel) descriptor.createLandCoverModel(Resampling.NEAREST_NEIGHBOUR);

        try {
            model.ensureLoaded();
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Unable to load land cover model 'MissingAuxdataModel'"));
            assertTrue(e.getMessage().contains("Required auxiliary data could not be read or downloaded."));
        }
    }

    private static class MissingAuxdataDescriptor extends AbstractLandCoverModelDescriptor {
        private final File file;

        private MissingAuxdataDescriptor(final File file) {
            this.file = file;
            name = "MissingAuxdataModel";
            remotePath = null;
        }

        @Override
        public LandCoverModel createLandCoverModel(final Resampling resampling) {
            return new FileLandCoverModel(this, new File[]{file}, resampling);
        }
    }
}
