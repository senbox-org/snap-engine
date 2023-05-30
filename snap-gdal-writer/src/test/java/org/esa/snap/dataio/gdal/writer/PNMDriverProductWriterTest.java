package org.esa.snap.dataio.gdal.writer;

import org.esa.snap.dataio.gdal.reader.plugins.PNMDriverProductReaderPlugIn;
import org.esa.snap.dataio.gdal.writer.plugins.PNMDriverProductWriterPlugIn;

/**
 * @author Jean Coravu
 */
public class PNMDriverProductWriterTest extends AbstractTestDriverProductWriter {

    public PNMDriverProductWriterTest() {
        super("PNM", ".pnm", "Byte UInt16", new PNMDriverProductReaderPlugIn(), new PNMDriverProductWriterPlugIn());
    }
}
