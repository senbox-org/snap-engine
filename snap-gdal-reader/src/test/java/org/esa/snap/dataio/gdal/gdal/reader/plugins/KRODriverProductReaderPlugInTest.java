package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.plugins.KRODriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class KRODriverProductReaderPlugInTest extends AbstractTestDriverProductReaderPlugIn {

    public KRODriverProductReaderPlugInTest() {
        super(".kro", "KRO", new KRODriverProductReaderPlugIn());
    }
}
