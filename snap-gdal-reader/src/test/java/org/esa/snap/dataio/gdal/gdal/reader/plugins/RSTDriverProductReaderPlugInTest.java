package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.plugins.RSTDriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class RSTDriverProductReaderPlugInTest extends AbstractTestDriverProductReaderPlugIn {

    public RSTDriverProductReaderPlugInTest() {
        super(".rst", "RST", new RSTDriverProductReaderPlugIn());
    }
}
