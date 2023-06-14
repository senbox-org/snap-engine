package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class PCIDSKDriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public PCIDSKDriverProductWriterPlugIn() {
        super(".pix", "PCIDSK", "PCIDSK Database File", "Byte UInt16 Int16 Float32 CInt16 CFloat32");
    }
}
