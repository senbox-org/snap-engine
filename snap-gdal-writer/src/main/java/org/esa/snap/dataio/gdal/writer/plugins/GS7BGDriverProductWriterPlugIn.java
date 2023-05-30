package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class GS7BGDriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public GS7BGDriverProductWriterPlugIn() {
        super(".grd", "GS7BG", "Golden Software 7 Binary Grid", "Byte Int16 UInt16 Float32 Float64");
    }
}
