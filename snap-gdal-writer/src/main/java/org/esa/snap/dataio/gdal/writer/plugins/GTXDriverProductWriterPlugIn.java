package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class GTXDriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public GTXDriverProductWriterPlugIn() {
        super(".gtx", "GTX", "NOAA Vertical Datum .GTX", "Float32");
    }
}
