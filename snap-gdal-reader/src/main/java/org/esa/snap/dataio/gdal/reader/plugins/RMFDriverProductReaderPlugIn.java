package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class RMFDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public RMFDriverProductReaderPlugIn() {
        super(".rsw", "RMF", "Raster Matrix Format");
    }
}
