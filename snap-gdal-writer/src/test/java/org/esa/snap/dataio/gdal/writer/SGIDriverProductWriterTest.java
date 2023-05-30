package org.esa.snap.dataio.gdal.writer;

import org.esa.snap.dataio.gdal.reader.plugins.SGIDriverProductReaderPlugIn;
import org.esa.snap.dataio.gdal.writer.plugins.SGIDriverProductWriterPlugIn;

/**
 * @author Jean Coravu
 */
public class SGIDriverProductWriterTest extends AbstractTestDriverProductWriter {

    public SGIDriverProductWriterTest() {
        super("SGI", ".rgb", "Byte", new SGIDriverProductReaderPlugIn(), new SGIDriverProductWriterPlugIn());
    }
}
