package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * Writer plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class BMPDriverProductWriterPlugIn extends AbstractDriverProductWriterPlugIn {

    public BMPDriverProductWriterPlugIn() {
        super(".bmp", "BMP", "MS Windows Device Independent Bitmap", "Byte");
    }
}
