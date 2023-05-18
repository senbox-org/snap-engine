package org.esa.snap.dataio.gdal.drivers;

import java.awt.image.IndexColorModel;
import java.lang.invoke.MethodHandle;

/**
 * GDAL ColorTable JNI driver class
 *
 * @author Adrian Drăghici
 */
public class ColorTable extends GDALBase {

    /**
     * The name of JNI GDAL ColorTable class
     */
    private static final String CLASS_NAME = "org.gdal.gdal.ColorTable";
    private static final Class<?> colorTableClass;

    static {
        colorTableClass = GDALReflection.fetchGDALLibraryClass(CLASS_NAME);
    }

    private final Object jniColorTable;
    private final MethodHandle getIndexColorModelHandle;

    /**
     * Creates new instance for this driver
     *
     * @param jniColorTable the JNI GDAL ColorTable class instance
     */
    ColorTable(Object jniColorTable) {
        this.jniColorTable = jniColorTable;
        try {
            getIndexColorModelHandle = createHandle(colorTableClass, "getIndexColorModel", IndexColorModel.class, int.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calls the JNI GDAL ColorTable class GetIndexColorModel(int bits) method
     *
     * @param bits the JNI GDAL ColorTable class GetIndexColorModel(int bits) method 'bits' argument
     * @return the JNI GDAL ColorTable class GetIndexColorModel(int bits) method result
     */
    public IndexColorModel getIndexColorModel(int bits) {
        return (IndexColorModel) invoke(getIndexColorModelHandle, this.jniColorTable, bits);
    }
}
