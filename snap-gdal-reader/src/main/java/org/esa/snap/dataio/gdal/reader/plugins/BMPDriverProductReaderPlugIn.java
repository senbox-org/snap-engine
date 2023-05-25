package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class BMPDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public BMPDriverProductReaderPlugIn() {
        super(".bmp", "BMP", "MS Windows Device Independent Bitmap");
    }
}
