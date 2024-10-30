package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class NITFDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public NITFDriverProductReaderPlugIn() {
        super(".ntf", "NITF", "National Imagery Transmission Format (GDAL)");
    }
}
