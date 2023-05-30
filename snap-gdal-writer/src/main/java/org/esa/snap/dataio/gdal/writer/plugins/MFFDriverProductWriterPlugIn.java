package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class MFFDriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public MFFDriverProductWriterPlugIn() {
        super(".hdr", "MFF", "Vexcel MFF Raster", "Byte UInt16 Float32 CInt16 CFloat32");
    }
}
