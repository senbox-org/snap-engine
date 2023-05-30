package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class JP2OpenJPEGDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public JP2OpenJPEGDriverProductReaderPlugIn() {
        super("JP2OpenJPEG", "JPEG-2000 driver based on OpenJPEG library");

        addExtension(".jp2");
        addExtension(".j2k");
    }
}
