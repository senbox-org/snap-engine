package org.esa.snap.dataio.gdal.writer;

import org.esa.snap.dataio.gdal.reader.plugins.SAGADriverProductReaderPlugIn;
import org.esa.snap.dataio.gdal.writer.plugins.SAGADriverProductWriterPlugIn;

/**
 * @author Jean Coravu
 */
public class SAGADriverProductWriterTest extends AbstractTestDriverProductWriter {

    public SAGADriverProductWriterTest() {
        super("SAGA", ".sdat", "Byte Int16 UInt16 Int32 UInt32 Float32 Float64", new SAGADriverProductReaderPlugIn(), new SAGADriverProductWriterPlugIn());
    }
}
