package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class PNMDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public PNMDriverProductReaderPlugIn() {
        super(".pnm", "PNM", "Portable Pixmap Format");
    }
}
