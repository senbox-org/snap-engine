package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.plugins.GTXDriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class GTXDriverProductReaderPlugInTest extends AbstractTestDriverProductReaderPlugIn {

    public GTXDriverProductReaderPlugInTest() {
        super(".gtx", "GTX", new GTXDriverProductReaderPlugIn());
    }
}
