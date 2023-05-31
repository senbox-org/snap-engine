package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.plugins.GSBGDriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class GSBGDriverProductReaderPlugInTest extends AbstractTestDriverProductReaderPlugIn {

    public GSBGDriverProductReaderPlugInTest() {
        super(".grd", "GSBG", new GSBGDriverProductReaderPlugIn());
    }
}
