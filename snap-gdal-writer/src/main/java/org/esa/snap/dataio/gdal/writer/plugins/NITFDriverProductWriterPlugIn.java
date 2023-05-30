package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class NITFDriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public NITFDriverProductWriterPlugIn() {
        super(".ntf", "NITF", "National Imagery Transmission Format", "Byte UInt16 Int16 UInt32 Int32 Float32");
    }
}
