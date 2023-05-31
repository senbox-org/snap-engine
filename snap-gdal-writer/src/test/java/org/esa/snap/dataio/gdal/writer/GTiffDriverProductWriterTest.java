package org.esa.snap.dataio.gdal.writer;

import org.esa.snap.dataio.gdal.reader.plugins.GTiffDriverProductReaderPlugIn;
import org.esa.snap.dataio.gdal.writer.plugins.GTiffDriverProductWriterPlugIn;

/**
 * @author Jean Coravu
 */
public class GTiffDriverProductWriterTest extends AbstractTestDriverProductWriter {

    public GTiffDriverProductWriterTest() {
        super("GTiff", ".tif", "Byte UInt16 Int16 UInt32 Int32 Float32 Float64 CInt16 CInt32 CFloat32 CFloat64", new GTiffDriverProductReaderPlugIn(), new GTiffDriverProductWriterPlugIn());
    }
}
