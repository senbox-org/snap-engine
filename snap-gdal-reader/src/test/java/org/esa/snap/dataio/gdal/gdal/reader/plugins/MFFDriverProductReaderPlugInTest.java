package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.plugins.MFFDriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class MFFDriverProductReaderPlugInTest extends AbstractTestDriverProductReaderPlugIn {

    public MFFDriverProductReaderPlugInTest() {
        super(".hdr", "MFF", new MFFDriverProductReaderPlugIn());
    }
}
