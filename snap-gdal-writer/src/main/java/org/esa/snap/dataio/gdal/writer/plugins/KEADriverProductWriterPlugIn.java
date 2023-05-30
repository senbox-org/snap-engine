package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class KEADriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public KEADriverProductWriterPlugIn() {
        super(".kea", "KEA", "KEA Image Format", "Byte Int16 UInt16 Int32 UInt32 Float32 Float64");
    }
}
