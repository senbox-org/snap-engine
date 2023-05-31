package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.plugins.NITFDriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class NITFDriverProductReaderPlugInTest extends AbstractTestDriverProductReaderPlugIn {

    public NITFDriverProductReaderPlugInTest() {
        super(".ntf", "NITF", new NITFDriverProductReaderPlugIn());
    }
}
