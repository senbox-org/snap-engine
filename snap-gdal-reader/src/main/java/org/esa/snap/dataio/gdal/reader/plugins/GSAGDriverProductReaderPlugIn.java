package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class GSAGDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public GSAGDriverProductReaderPlugIn() {
        super(".grd", "GSAG", "Golden Software ASCII Grid");
    }
}
