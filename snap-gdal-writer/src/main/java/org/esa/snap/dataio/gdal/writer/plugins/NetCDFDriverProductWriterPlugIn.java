package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class NetCDFDriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public NetCDFDriverProductWriterPlugIn() {
        super(".nc", "netCDF", "Network Common Data Format", null);
    }
}
