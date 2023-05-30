package org.esa.snap.dataio.gdal.writer;

import org.esa.snap.dataio.gdal.reader.plugins.RSTDriverProductReaderPlugIn;
import org.esa.snap.dataio.gdal.writer.plugins.RSTDriverProductWriterPlugIn;

/**
 * @author Jean Coravu
 */
public class RSTDriverProductWriterTest extends AbstractTestDriverProductWriter {

    public RSTDriverProductWriterTest() {
        super("RST", ".rst", "Byte Int16 Float32", new RSTDriverProductReaderPlugIn(), new RSTDriverProductWriterPlugIn());
    }
}
