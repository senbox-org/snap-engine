package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class SGIDriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public SGIDriverProductWriterPlugIn() {
        super(".rgb", "SGI", "SGI Image File Format 1.0", "Byte");
    }
}
