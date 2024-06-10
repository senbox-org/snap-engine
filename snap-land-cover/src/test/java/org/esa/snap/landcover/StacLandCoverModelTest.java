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
package org.esa.snap.landcover;

import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.landcover.dataio.ESRILULC.ESRILULC2020ModelDescriptor;
import org.esa.snap.landcover.dataio.LandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.StacLandCoverModel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StacLandCoverModelTest {
    private StacLandCoverModel model;
    private LandCoverModelDescriptor descriptor;
    private Resampling resamplingMethod;

    @Before
    public void setup() {
        descriptor = new ESRILULC2020ModelDescriptor();
        resamplingMethod = Resampling.NEAREST_NEIGHBOUR;
        model = new StacLandCoverModel(descriptor, resamplingMethod);
    }

    @Test
    public void getDescriptorTest() {
        LandCoverModelDescriptor result = model.getDescriptor();

        // Assert
        assertEquals(descriptor, result);
    }

    @Test
    public void getResamplingTest() {
        Resampling result = model.getResampling();

        // Assert
        assertEquals(resamplingMethod, result);
    }
}

