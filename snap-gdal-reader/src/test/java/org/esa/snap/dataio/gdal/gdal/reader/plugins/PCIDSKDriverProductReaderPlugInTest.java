package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.plugins.PCIDSKDriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class PCIDSKDriverProductReaderPlugInTest extends AbstractTestDriverProductReaderPlugIn {

    public PCIDSKDriverProductReaderPlugInTest() {
        super(".pix", "PCIDSK", new PCIDSKDriverProductReaderPlugIn());
    }
}
