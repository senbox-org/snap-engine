package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.plugins.ILWISDriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class ILWISDriverProductReaderPlugInTest extends AbstractTestDriverProductReaderPlugIn {

    public ILWISDriverProductReaderPlugInTest() {
        super("ILWIS", new ILWISDriverProductReaderPlugIn());

        addExtensin(".mpr");
        addExtensin(".mpl");
    }
}
