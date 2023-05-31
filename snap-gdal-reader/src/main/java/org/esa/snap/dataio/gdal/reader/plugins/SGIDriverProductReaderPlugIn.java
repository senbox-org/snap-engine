package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class SGIDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public SGIDriverProductReaderPlugIn() {
        super(".rgb", "SGI", "SGI Image File Format 1.0");
    }
}
