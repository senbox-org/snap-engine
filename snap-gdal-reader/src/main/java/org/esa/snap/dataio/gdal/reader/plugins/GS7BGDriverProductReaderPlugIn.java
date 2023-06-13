package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class GS7BGDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public GS7BGDriverProductReaderPlugIn() {
        super(".grd", "GS7BG", "Golden Software 7 Binary Grid");
    }
}
