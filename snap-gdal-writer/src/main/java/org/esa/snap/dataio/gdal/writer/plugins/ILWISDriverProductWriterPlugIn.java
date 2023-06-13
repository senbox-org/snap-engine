package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class ILWISDriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public ILWISDriverProductWriterPlugIn() {
        super(".mpr", "ILWIS", "ILWIS Raster Map", "Byte Int16 Int32 Float64");
    }
}
