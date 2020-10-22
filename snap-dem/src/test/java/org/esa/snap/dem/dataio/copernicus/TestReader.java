package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dem.dataio.copernicus90m.CopernicusFile;
import org.esa.snap.dem.dataio.copernicus90m.CopernicusGeoTIFFReaderPlugIn;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;


public class TestReader {

    @Test
    public void testDecode() throws Exception{
        CopernicusGeoTIFFReaderPlugIn plugin = new CopernicusGeoTIFFReaderPlugIn();

        assertTrue(plugin.getDecodeQualification(new File("file.tar")) == DecodeQualification.INTENDED);
        assertTrue(plugin.getDecodeQualification(new File("file.tif")) == DecodeQualification.INTENDED);
        assertTrue(plugin.getDecodeQualification(new File("file.xml")) == DecodeQualification.UNABLE);

    }

    @Test
    public void test() {

    }



}
