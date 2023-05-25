package org.esa.snap.dataio.gdal.writer.plugins;

/**
 * @author Jean Coravu
 */
public class GTiffDriverProductWriterPlugInTest extends AbstractTestDriverProductWriterPlugIn {

    public GTiffDriverProductWriterPlugInTest() {
        super("GTiff", new GTiffDriverProductWriterPlugIn());
    }
}
