package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class BTDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public BTDriverProductReaderPlugIn() {
        super(".bt", "BT", "VTP .bt (Binary Terrain) 1.3 Format");
    }
}
