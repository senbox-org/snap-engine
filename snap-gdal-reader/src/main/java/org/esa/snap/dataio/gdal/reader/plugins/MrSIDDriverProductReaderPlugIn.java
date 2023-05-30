package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class MrSIDDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public MrSIDDriverProductReaderPlugIn() {
        super(".sid", "MrSID", "Multi-resolution Seamless Image Database");
    }
}
