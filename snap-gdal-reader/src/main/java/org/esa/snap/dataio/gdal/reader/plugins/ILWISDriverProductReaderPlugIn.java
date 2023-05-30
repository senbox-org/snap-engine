package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class ILWISDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public ILWISDriverProductReaderPlugIn() {
        super("ILWIS", "ILWIS Raster Map");

        addExtension(".mpr");
        addExtension(".mpl");
    }
}
