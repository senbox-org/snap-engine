package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class HFADriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public HFADriverProductWriterPlugIn() {
        super(".img", "HFA", "Erdas Imagine Images", "Byte Int16 UInt16 Int32 UInt32 Float32 Float64 CFloat32 CFloat64");
    }
}
