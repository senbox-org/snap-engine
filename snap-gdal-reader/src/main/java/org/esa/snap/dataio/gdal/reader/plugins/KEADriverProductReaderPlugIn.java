package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class KEADriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public KEADriverProductReaderPlugIn() {
        super(".kea", "KEA", "KEA Image Format");
    }
}
