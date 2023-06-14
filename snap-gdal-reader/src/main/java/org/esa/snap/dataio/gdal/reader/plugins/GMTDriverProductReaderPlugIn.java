package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class GMTDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public GMTDriverProductReaderPlugIn() {
        super(".nc", "GMT", "GMT NetCDF Grid Format");
    }
}
