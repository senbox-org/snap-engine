package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.plugins.SAGADriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class SAGADriverProductReaderPlugInTest extends AbstractTestDriverProductReaderPlugIn {

    public SAGADriverProductReaderPlugInTest() {
        super(".sdat", "SAGA", new SAGADriverProductReaderPlugIn());
    }
}
