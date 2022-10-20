package org.esa.s2tbx.dataio.gdal.drivers;

/**
 * GDAL Driver JNI driver class
 *
 * @author Adrian Drăghici
 */
public class Driver {

    /**
     * The name of JNI GDAL Driver class
     */
    private static final String CLASS_NAME = "org.gdal.gdal.Driver";

    private Object jniDriverInstance;

    /**
     * Creates new instance for this driver
     *
     * @param jniDriverInstance the JNI GDAL Driver class instance
     */
    Driver(Object jniDriverInstance) {
        this.jniDriverInstance = jniDriverInstance;
    }

    /**
     * Calls the JNI GDAL Driver class getShortName() method
     *
     * @return the JNI GDAL Driver class getShortName() method result
     */
    public String getShortName() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "getShortName", String.class, this.jniDriverInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Driver class getLongName() method
     *
     * @return the JNI GDAL Driver class getLongName() method result
     */
    public String getLongName() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "getLongName", String.class, this.jniDriverInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType) method
     *
     * @param utf8Path the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType) method 'utf8Path' argument
     * @param xsize    the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType) method 'xsize' argument
     * @param ysize    the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType) method 'ysize' argument
     * @param bands    the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType) method 'bands' argument
     * @param eType    the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType) method 'eType' argument
     * @return the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType) method result
     */
    public Dataset create(String utf8Path, int xsize, int ysize, int bands, int eType) {
        Object jniDatasetInstance = GDALReflection.callGDALLibraryMethod(CLASS_NAME, "Create", Object.class, this.jniDriverInstance, new Class[]{String.class, int.class, int.class, int.class, int.class}, new Object[]{utf8Path, xsize, ysize, bands, eType});
        if (jniDatasetInstance != null) {
            return new Dataset(jniDatasetInstance);
        }
        return null;
    }

    /**
     * Calls the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType) method
     *
     * @param utf8Path the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType, String[] options) method 'utf8Path' argument
     * @param xsize    the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType, String[] options) method 'xsize' argument
     * @param ysize    the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType, String[] options) method 'ysize' argument
     * @param bands    the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType, String[] options) method 'bands' argument
     * @param eType    the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType, String[] options) method 'eType' argument
     * @param options  the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType, String[] options) method 'options' argument
     * @return the JNI GDAL Driver class Create(String utf8Path, int xsize, int ysize, int bands, int eType, String[] options) method result
     */
    public Dataset create(String utf8Path, int xsize, int ysize, int bands, int eType, String[] options) {
        Object jniDatasetInstance = GDALReflection.callGDALLibraryMethod(CLASS_NAME, "Create", Object.class, this.jniDriverInstance, new Class[]{String.class, int.class, int.class, int.class, int.class, String[].class}, new Object[]{utf8Path, xsize, ysize, bands, eType, options});
        if (jniDatasetInstance != null) {
            return new Dataset(jniDatasetInstance);
        }
        return null;
    }

    public Dataset createCopy(String name, Dataset src, String[] options) {
        Object jniDatasetInstance = GDALReflection.callGDALLibraryMethod(CLASS_NAME, "CreateCopy", Object.class, this.jniDriverInstance, new Class[]{String.class, src.getJniDatasetInstance().getClass(), String[].class}, new Object[]{name, src.getJniDatasetInstance(), options});
        if (jniDatasetInstance != null) {
            return new Dataset(jniDatasetInstance);
        }
        return null;
    }

    /**
     * Calls the JNI GDAL Dataset class Delete(String utf8_path) method
     *
     * @param utf8_path the JNI GDAL Dataset class Delete(String utf8_path) method 'utf8_path' argument
     * @return the JNI GDAL Dataset class Delete(String utf8_path) method result
     */
    public Integer delete(String utf8_path){
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "Delete", Integer.class, this.jniDriverInstance, new Class[]{String.class}, new Object[]{utf8_path});
    }
}
