package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.plugins.HFADriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class HFADriverProductReaderPlugInTest extends AbstractTestDriverProductReaderPlugIn {

    public HFADriverProductReaderPlugInTest() {
        super(".img", "HFA", new HFADriverProductReaderPlugIn());
    }
}
