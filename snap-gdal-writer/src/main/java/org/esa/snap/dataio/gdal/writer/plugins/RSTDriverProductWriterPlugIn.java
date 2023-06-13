package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class RSTDriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public RSTDriverProductWriterPlugIn() {
        super(".rst", "RST", "Idrisi Raster A.1", "Byte Int16 Float32");
    }
}
