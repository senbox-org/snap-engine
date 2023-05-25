package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class SAGADriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public SAGADriverProductReaderPlugIn() {
        super(".sdat", "SAGA", "SAGA GIS Binary Grid");
    }
}
