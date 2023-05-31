package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.plugins.RMFDriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class RMFDriverProductReaderPlugInTest extends AbstractTestDriverProductReaderPlugIn {

    public RMFDriverProductReaderPlugInTest() {
        super(".rsw", "RMF", new RMFDriverProductReaderPlugIn());
    }
}
