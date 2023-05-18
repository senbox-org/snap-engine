package org.esa.snap.dataio.gdal.drivers;

import java.lang.invoke.MethodHandle;

/**
 * GDAL osr JNI driver class
 *
 * @author Adrian Drăghici
 */
public class OSR extends GDALBase {

    /**
     * The name of JNI GDAL osr class
     */
    private static final String CLASS_NAME = "org.gdal.osr.osr";
    private static final Class<?> osrClass;
    private static final OSR instance;
    private final MethodHandle setPROJSearchPathHandle;

    static {
        osrClass = GDALReflection.fetchGDALLibraryClass(CLASS_NAME);
        instance = new OSR();
    }

    /**
     * Creates new instance for this driver
     */
    private OSR() {
        try {
            setPROJSearchPathHandle = createStaticHandle(osrClass, "SetPROJSearchPath", void.class, String.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calls the JNI GDAL osr class SetPROJSearchPath(String utf8_path) method
     *
     * @param utf8Path the JNI GDAL osr class SetPROJSearchPath(String utf8_path) method 'utf8_path' argument
     */
    public static void setPROJSearchPath(String utf8Path) {
        invokeStatic(instance.setPROJSearchPathHandle, utf8Path);
    }

}
