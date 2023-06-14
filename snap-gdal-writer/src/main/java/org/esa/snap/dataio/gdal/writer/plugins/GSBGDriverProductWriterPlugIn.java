package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class GSBGDriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public GSBGDriverProductWriterPlugIn() {
        super(".grd", "GSBG", "Golden Software Binary Grid", "Byte Int16 UInt16 Float32");
    }
}
