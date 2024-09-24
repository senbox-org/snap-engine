package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.dem.dataio.copernicus.copernicus30m.Copernicus30mElevationModelDescriptor;
import org.esa.snap.dem.dataio.copernicus.copernicus90m.Copernicus90mElevationModelDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TestElevationDescriptor {

    @Test
    public void testCreateCopernicus90mDescriptors (){

        Copernicus90mElevationModelDescriptor descriptor = new Copernicus90mElevationModelDescriptor();

        assertEquals(descriptor.getName(), "Copernicus 90m Global DEM");

        assert descriptor.getNoDataValue() == 0;
        assert descriptor.getRasterWidth() == 1200;
        assert descriptor.getRasterHeight() == 1200;
        assert descriptor.getTileWidthInDegrees() == 1;
        assert descriptor.getTileWidth() == 1200;
        assert descriptor.getNumXTiles() == 360;
        assert descriptor.getNumYTiles() == 180;
        assert descriptor.canBeDownloaded();

    }

    @Test
    public void testCreateCopernicus30mDescriptors (){

        Copernicus30mElevationModelDescriptor descriptor = new Copernicus30mElevationModelDescriptor();

        assertEquals(descriptor.getName(), "Copernicus 30m Global DEM");

        assert descriptor.getNoDataValue() == 0;
        assert descriptor.getRasterWidth() == 3600;
        assert descriptor.getRasterHeight() == 3600;
        assert descriptor.getTileWidthInDegrees() == 1;
        assert descriptor.getTileWidth() == 3600;
        assert descriptor.getNumXTiles() == 360;
        assert descriptor.getNumYTiles() == 180;
        assert descriptor.canBeDownloaded();

    }


}
