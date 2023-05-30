package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.plugins.BTDriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class BTDriverProductReaderPlugInTest extends AbstractTestDriverProductReaderPlugIn {

    public BTDriverProductReaderPlugInTest() {
        super(".bt", "BT", new BTDriverProductReaderPlugIn());
    }
}
