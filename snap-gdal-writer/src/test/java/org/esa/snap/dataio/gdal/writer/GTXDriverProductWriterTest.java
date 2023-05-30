package org.esa.snap.dataio.gdal.writer;

import org.esa.snap.dataio.gdal.reader.plugins.GTXDriverProductReaderPlugIn;
import org.esa.snap.dataio.gdal.writer.plugins.GTXDriverProductWriterPlugIn;

/**
 * @author Jean Coravu
 */
public class GTXDriverProductWriterTest extends AbstractTestDriverProductWriter {

    public GTXDriverProductWriterTest() {
        super("GTX", ".gtx", "Float32", new GTXDriverProductReaderPlugIn(), new GTXDriverProductWriterPlugIn());
    }
}
