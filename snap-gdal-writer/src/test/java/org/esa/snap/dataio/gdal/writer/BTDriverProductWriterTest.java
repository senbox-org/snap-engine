package org.esa.snap.dataio.gdal.writer;

import org.esa.snap.dataio.gdal.reader.plugins.BTDriverProductReaderPlugIn;
import org.esa.snap.dataio.gdal.writer.plugins.BTDriverProductWriterPlugIn;

/**
 * @author Jean Coravu
 */
public class BTDriverProductWriterTest extends AbstractTestDriverProductWriter {

    public BTDriverProductWriterTest() {
        super("BT", ".bt", "Int16 Int32 Float32", new BTDriverProductReaderPlugIn(), new BTDriverProductWriterPlugIn());
    }
}
