package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.dataio.DecodeQualification;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;


public class TestReader {

    @Test
    public void testDecode() throws Exception{
        CopernicusGeoTIFFReaderPlugIn plugin = new CopernicusGeoTIFFReaderPlugIn();

        assertTrue(plugin.getDecodeQualification(new File("file.tif")) == DecodeQualification.INTENDED);
        assertTrue(plugin.getDecodeQualification(new File("file.tar")) == DecodeQualification.UNABLE);
        assertTrue(plugin.getDecodeQualification(new File("file.xml")) == DecodeQualification.UNABLE);

    }

    @Test
    public void test() {

    }



}
