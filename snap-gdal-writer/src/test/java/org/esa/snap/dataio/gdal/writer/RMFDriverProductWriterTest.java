package org.esa.snap.dataio.gdal.writer;

import org.esa.snap.dataio.gdal.reader.plugins.RMFDriverProductReaderPlugIn;
import org.esa.snap.dataio.gdal.writer.plugins.RMFDriverProductWriterPlugIn;

/**
 * @author Jean Coravu
 */
public class RMFDriverProductWriterTest extends AbstractTestDriverProductWriter {

    public RMFDriverProductWriterTest() {
        super("RMF", ".rsw", "Byte Int16 Int32 Float64", new RMFDriverProductReaderPlugIn(), new RMFDriverProductWriterPlugIn());
    }
}
