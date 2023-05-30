package org.esa.snap.dataio.gdal.writer;

import org.esa.snap.dataio.gdal.reader.plugins.ILWISDriverProductReaderPlugIn;
import org.esa.snap.dataio.gdal.writer.plugins.ILWISDriverProductWriterPlugIn;

/**
 * @author Jean Coravu
 */
public class ILWISDriverProductWriterTest extends AbstractTestDriverProductWriter {

    public ILWISDriverProductWriterTest() {
        super("ILWIS", ".mpr", "Byte Int16 Int32 Float64", new ILWISDriverProductReaderPlugIn(), new ILWISDriverProductWriterPlugIn());
    }
}
