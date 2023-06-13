package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class KRODriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public KRODriverProductReaderPlugIn() {
        super(".kro", "KRO", "KOLOR Raw");
    }
}
