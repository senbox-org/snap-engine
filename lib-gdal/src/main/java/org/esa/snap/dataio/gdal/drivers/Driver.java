package org.esa.snap.dataio.gdal.drivers;

import java.lang.invoke.MethodHandle;

/**
 * GDAL Driver JNI driver class
 *
 * @author Adrian Drăghici
 */
public class Driver extends GDALBase {

    /**
     * The name of JNI GDAL Driver class
     */
    private static final String CLASS_NAME = "org.gdal.gdal.Driver";
    private static final Class<?> driverClass;

    static {
        driverClass = GDALReflection.fetchGDALLibraryClass(CLASS_NAME);
    }

    private final Object jniDriverInstance;
    private final MethodHandle getShortNameHandle;
    private final MethodHandle getLongNameHandle;
    private final MethodHandle create1Handle;
    private final MethodHandle create2Handle;
    private final MethodHandle createCopyHandle;
    private final MethodHandle deleteHandle;

    /**
     * Creates new instance for this driver
     *
     * @param jniDriverInstance the JNI GDAL Driver class instance
     */
    Driver(Object jniDriverInstance) {
        this.jniDriverInstance = jniDriverInstance;
        try {
            getShortNameHandle = createHandle(driverClass, "getShortName", String.class);
            getLongNameHandle = createHandle(driverClass, "getLongName", String.class);
            create1Handle = createHandle(driverClass, "Create", GDALReflection.fetchGDALLibraryClass("org.gdal.gdal.Dataset"),
                                         String.class, int.class, int.class, int.class, int.class);
            create2Handle = createHandle(driverClass, "Create", GDALReflection.fetchGDALLibraryClass("org.gdal.gdal.Dataset"),
                                         String.class, int.class, int.class, int.class, int.class, String[].class);
            createCopyHandle = createHandle(driverClass, "CreateCopy", GDALReflection.fetchGDALLibraryClass("org.gdal.gdal.Dataset"),
                                            String.class, GDALReflection.fetchGDALLibraryClass("org.gdal.gdal.Dataset"), String[].class);
            deleteHandle = createHandle(driverClass, "Delete", int.class, String.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calls the JNI GDAL Driver class getShortName() method
     *
     * @return the JNI GDAL Driver class getShortName() method result
     */
    public String getShortName() {
        return (String) invoke(getShortNameHandle, this.jniDriverInstance);
    }

    /**
     * Calls the JNI GDAL Driver class getLongName() method
     *
     * @return the JNI GDAL Driver class getLongName() method result
     */
    public String getLongName() {
        return (String) invoke(getLongNameHandle, this.jniDriverInstance);
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
        Object jniDatasetInstance = invoke(create1Handle, this.jniDriverInstance, utf8Path, xsize, ysize, bands, eType);
        return jniDatasetInstance != null ? new Dataset(jniDatasetInstance) : null;
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
        Object jniDatasetInstance = invoke(create2Handle, this.jniDriverInstance, utf8Path, xsize, ysize, bands, eType, options);
        return jniDatasetInstance != null ? new Dataset(jniDatasetInstance) : null;
    }

    public Dataset createCopy(String name, Dataset src, String[] options) {
        Object jniDatasetInstance = invoke(createCopyHandle, this.jniDriverInstance, name, src.getJniDatasetInstance(), options);
        return jniDatasetInstance != null ? new Dataset(jniDatasetInstance) : null;
    }

    /**
     * Calls the JNI GDAL Dataset class Delete(String utf8_path) method
     *
     * @param utf8_path the JNI GDAL Dataset class Delete(String utf8_path) method 'utf8_path' argument
     * @return the JNI GDAL Dataset class Delete(String utf8_path) method result
     */
    public Integer delete(String utf8_path){
        return (Integer) invoke(deleteHandle, this.jniDriverInstance, utf8_path);
    }
}
