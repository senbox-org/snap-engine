package org.esa.snap.dataio.gdal.writer;

import org.esa.snap.dataio.gdal.reader.plugins.KRODriverProductReaderPlugIn;
import org.esa.snap.dataio.gdal.writer.plugins.KRODriverProductWriterPlugIn;

/**
 * @author Jean Coravu
 */
public class KRODriverProductWriterTest extends AbstractTestDriverProductWriter {

    public KRODriverProductWriterTest() {
        super("KRO", ".kro", "Byte UInt16 Float32", new KRODriverProductReaderPlugIn(), new KRODriverProductWriterPlugIn());
    }
}
