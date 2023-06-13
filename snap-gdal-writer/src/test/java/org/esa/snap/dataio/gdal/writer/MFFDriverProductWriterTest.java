package org.esa.snap.dataio.gdal.writer;

import org.esa.snap.dataio.gdal.reader.plugins.MFFDriverProductReaderPlugIn;
import org.esa.snap.dataio.gdal.writer.plugins.MFFDriverProductWriterPlugIn;

/**
 * @author Jean Coravu
 */
public class MFFDriverProductWriterTest extends AbstractTestDriverProductWriter {

    public MFFDriverProductWriterTest() {
        super("MFF", ".hdr", "Byte UInt16 Float32 CInt16 CFloat32", new MFFDriverProductReaderPlugIn(), new MFFDriverProductWriterPlugIn());
    }
}
