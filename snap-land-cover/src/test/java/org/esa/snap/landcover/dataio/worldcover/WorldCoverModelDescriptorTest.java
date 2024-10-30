package org.esa.snap.landcover.dataio.worldcover;

import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.landcover.dataio.ESRILULC.ESRILULC2020ModelDescriptor;
import org.esa.snap.landcover.dataio.LandCoverModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WorldCoverModelDescriptorTest {

    @Test
    public void testConstructor() {
        WorldCoverModelDescriptor modelDescriptor = new WorldCoverModelDescriptor();
        assertEquals("ESA_WorldCover", modelDescriptor.getName());
        assertEquals(0, modelDescriptor.getNoDataValue(), 230);
    }

    @Test
    public void testCreateLandCoverModel() {
        WorldCoverModelDescriptor modelDescriptor = new WorldCoverModelDescriptor();
        LandCoverModel landCoverModel = modelDescriptor.createLandCoverModel(Resampling.NEAREST_NEIGHBOUR);

        assertEquals(Resampling.NEAREST_NEIGHBOUR, landCoverModel.getResampling());

        assertEquals(modelDescriptor, landCoverModel.getDescriptor());
        landCoverModel.dispose();
    }

    @Test
    public void testIsInstalled() {
        WorldCoverModelDescriptor modelDescriptor = new WorldCoverModelDescriptor();
        assertTrue(modelDescriptor.isInstalled());
    }
}
