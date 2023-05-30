package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class BTDriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public BTDriverProductWriterPlugIn() {
        super(".bt", "BT", "VTP .bt (Binary Terrain) 1.3 Format", "Int16 Int32 Float32");
    }
}
