package org.esa.snap.dataio.gdal.drivers;

import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandle;

/**
 * GDAL ColorTable JNI driver class
 *
 * @author Adrian Drăghici
 */
public class ColorTable extends GDALBase implements Closeable {

    /**
     * The name of JNI GDAL ColorTable class
     */
    static final String CLASS_NAME = "org.gdal.gdal.ColorTable";
    private static final Class<?> colorTableClass;

    static {
        colorTableClass = GDALReflection.fetchGDALLibraryClass(CLASS_NAME);
    }

    private final Object jniColorTable;
    private final MethodHandle getIndexColorModelHandle;
    private final MethodHandle getCountHandle;
    private final MethodHandle getColorEntryHandle;
    private final MethodHandle deleteHandle;

    /**
     * Creates new instance for this driver
     *
     * @param jniColorTable the JNI GDAL ColorTable class instance
     */
    ColorTable(Object jniColorTable) {
        this.jniColorTable = jniColorTable;
        try {
            getIndexColorModelHandle = createHandle(colorTableClass, "getIndexColorModel", IndexColorModel.class, int.class);
            getCountHandle = createHandle(colorTableClass, "GetCount", int.class);
            getColorEntryHandle = createHandle(colorTableClass, "GetColorEntry", Color.class, int.class);
            deleteHandle = createHandle(colorTableClass, "delete", void.class);
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

    /**
     * Calls the JNI GDAL ColorTable class GetCount() method
     *
     * @return the JNI GDAL ColorTable class GetCount() method result
     */
    public int getCount(){
        return (int) invoke(getCountHandle, this.jniColorTable);
    }

    /**
     * Calls the JNI GDAL ColorTable class GetColorEntry(int entry) method
     *
     * @param entry the JNI GDAL ColorTable class GetColorEntry(int entry) method 'entry' argument
     * @return the JNI GDAL ColorTable class GetColorEntry(int entry) method result
     */
    public Color getColorEntry(int entry){
        return (Color) invoke(getColorEntryHandle, jniColorTable, entry);
    }

    /**
     * Calls the JNI GDAL Dataset class delete() method
     */
    public void delete() {
        invoke(deleteHandle, this.jniColorTable);
    }

    @Override
    public void close() throws IOException {
        delete();
    }
}
