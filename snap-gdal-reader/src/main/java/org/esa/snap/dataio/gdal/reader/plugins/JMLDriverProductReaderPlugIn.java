package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class JMLDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public JMLDriverProductReaderPlugIn() {
        super(".jml", "JML", "OpenJUMP JML");
    }
}
