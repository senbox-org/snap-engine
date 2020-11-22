package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dem.dataio.copernicus90m.CopernicusDownloader;
import org.esa.snap.dem.dataio.copernicus90m.CopernicusGeoTIFFReaderPlugIn;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;


public class TestOpenSearch {


    @Test @Ignore
    public void testSearch() throws Exception {
        CopernicusDownloader d = new CopernicusDownloader(new File("C:/Users/Alex/.snap/auxdata/dem/Copernicus 90m Europe"));

    }
    @Test @Ignore
    public void testReadFromTARFile() throws Exception {
        CopernicusGeoTIFFReaderPlugIn readerPlugIn = new CopernicusGeoTIFFReaderPlugIn();

        ProductReader p = readerPlugIn.createReaderInstance();
        Path path = (new File("C:\\Users\\Alex\\WORK\\tmp_data\\DEM1_SAR_DGE_90_20110319T164209_20140125T050123_ADS_000000_5050.DEM.tar")).toPath();
        assertTrue(readerPlugIn.getDecodeQualification(path) == DecodeQualification.INTENDED);

        Product dtedProd = p.readProductNodes(path, null);


    }


}
