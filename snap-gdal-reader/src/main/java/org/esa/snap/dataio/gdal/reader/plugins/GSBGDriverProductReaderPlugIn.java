package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class GSBGDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public GSBGDriverProductReaderPlugIn() {
        super(".grd", "GSBG", "Golden Software Binary Grid");
    }
}
