package org.esa.snap.dataio.gdal.drivers;

/**
 * GDAL osr JNI driver class
 *
 * @author Adrian Drăghici
 */
public class OSR {

    /**
     * The name of JNI GDAL osr class
     */
    private static final String CLASS_NAME = "org.gdal.osr.osr";

    /**
     * Creates new instance for this driver
     */
    private OSR() {
        //nothing to init
    }

    /**
     * Calls the JNI GDAL osr class SetPROJSearchPath(String utf8_path) method
     *
     * @param utf8Path the JNI GDAL osr class SetPROJSearchPath(String utf8_path) method 'utf8_path' argument
     */
    public static void setPROJSearchPath(String utf8Path) {
        GDALReflection.callGDALLibraryMethod(CLASS_NAME, "SetPROJSearchPath", null, null, new Class[]{String.class}, new Object[]{utf8Path});
    }

}
