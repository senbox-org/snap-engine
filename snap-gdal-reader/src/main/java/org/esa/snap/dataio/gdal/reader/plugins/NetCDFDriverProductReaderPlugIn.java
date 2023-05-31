package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class NetCDFDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public NetCDFDriverProductReaderPlugIn() {
        super(".nc", "netCDF", "Network Common Data Format");
    }
}
