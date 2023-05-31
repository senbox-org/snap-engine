package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class PNGDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public PNGDriverProductReaderPlugIn() {
        super(".png", "PNG", "Portable Network Graphics");
    }
}
