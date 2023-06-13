package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.plugins.SGIDriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class SGIDriverProductReaderPlugInTest extends AbstractTestDriverProductReaderPlugIn {

    public SGIDriverProductReaderPlugInTest() {
        super(".rgb", "SGI", new SGIDriverProductReaderPlugIn());
    }
}
