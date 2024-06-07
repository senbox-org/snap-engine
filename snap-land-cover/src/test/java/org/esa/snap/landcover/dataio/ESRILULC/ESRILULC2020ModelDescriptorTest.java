package org.esa.snap.landcover.dataio.ESRILULC;

import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.landcover.LandCoverModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ESRILULC2020ModelDescriptorTest {

    @Test
    public void testConstructor() {
        ESRILULC2020ModelDescriptor modelDescriptor = new ESRILULC2020ModelDescriptor();
        assertEquals("ESRILULC2020", modelDescriptor.getName());
        assertEquals(0, modelDescriptor.getNoDataValue(), 0);
    }

    @Test
    public void testCreateLandCoverModel() {
        ESRILULC2020ModelDescriptor modelDescriptor = new ESRILULC2020ModelDescriptor();
        LandCoverModel landCoverModel = modelDescriptor.createLandCoverModel(Resampling.NEAREST_NEIGHBOUR);

        assertEquals(Resampling.NEAREST_NEIGHBOUR, landCoverModel.getResampling());

        assertEquals(modelDescriptor, landCoverModel.getDescriptor());
        landCoverModel.dispose();
    }

    @Test
    public void testIsInstalled() {
        ESRILULC2020ModelDescriptor modelDescriptor = new ESRILULC2020ModelDescriptor();
        assertTrue(modelDescriptor.isInstalled());
    }
}
