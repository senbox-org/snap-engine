package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class AVCE00DriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public AVCE00DriverProductReaderPlugIn() {
        super(".e00", "AVCE00", "VDV-451/VDV-452/INTREST Data Format");
    }
}
