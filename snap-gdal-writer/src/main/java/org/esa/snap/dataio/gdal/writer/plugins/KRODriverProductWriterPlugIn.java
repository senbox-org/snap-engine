package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class KRODriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public KRODriverProductWriterPlugIn() {
        super(".kro", "KRO", "KOLOR Raw", "Byte UInt16 Float32");
    }
}
