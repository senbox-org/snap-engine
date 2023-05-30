package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class GTiffDriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public GTiffDriverProductWriterPlugIn() {
        super(".tif", "GTiff", "GeoTIFF", "Byte UInt16 Int16 UInt32 Int32 Float32 Float64 CInt16 CInt32 CFloat32 CFloat64");
    }
}
