package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class SAGADriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public SAGADriverProductWriterPlugIn() {
        super(".sdat", "SAGA", "SAGA GIS Binary Grid", "Byte Int16 UInt16 Int32 UInt32 Float32 Float64");
    }
}
