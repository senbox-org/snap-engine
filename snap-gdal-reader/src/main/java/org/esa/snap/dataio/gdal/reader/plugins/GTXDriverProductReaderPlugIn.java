package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class GTXDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public GTXDriverProductReaderPlugIn() {
        super(".gtx", "GTX", "NOAA Vertical Datum .GTX");
    }
}
