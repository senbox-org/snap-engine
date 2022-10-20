package org.esa.s2tbx.dataio.gdal.drivers;

import java.awt.image.IndexColorModel;

/**
 * GDAL ColorTable JNI driver class
 *
 * @author Adrian Drăghici
 */
public class ColorTable {

    /**
     * The name of JNI GDAL ColorTable class
     */
    private static final String CLASS_NAME = "org.gdal.gdal.ColorTable";

    private Object jniColorTable;

    /**
     * Creates new instance for this driver
     *
     * @param jniColorTable the JNI GDAL ColorTable class instance
     */
    ColorTable(Object jniColorTable) {
        this.jniColorTable = jniColorTable;
    }

    /**
     * Calls the JNI GDAL ColorTable class GetIndexColorModel(int bits) method
     *
     * @param bits the JNI GDAL ColorTable class GetIndexColorModel(int bits) method 'bits' argument
     * @return the JNI GDAL ColorTable class GetIndexColorModel(int bits) method result
     */
    public IndexColorModel getIndexColorModel(int bits) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "getIndexColorModel", IndexColorModel.class, this.jniColorTable, new Class[]{int.class}, new Object[]{bits});
    }
}
