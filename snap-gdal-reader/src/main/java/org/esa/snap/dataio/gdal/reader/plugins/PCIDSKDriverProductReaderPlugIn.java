package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class PCIDSKDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public PCIDSKDriverProductReaderPlugIn() {
        super(".pix", "PCIDSK", "PCIDSK Database File");
    }
}
