package org.esa.s2tbx.dataio.gdal.drivers;

/**
 * GDAL GCP JNI driver class
 *
 * @author Adrian Drăghici
 */
public class GCP {

    /**
     * The name of JNI GDAL GCP class
     */
    private static final String CLASS_NAME = "org.gdal.gdal.GCP";

    private Object jniGCPInstance;

    /**
     * Creates new instance for this driver
     *
     * @param jniGCPInstance the JNI GDAL GCP class instance
     */
    public GCP(Object jniGCPInstance) {
        this.jniGCPInstance = jniGCPInstance;
    }

    static Class<?> getJNIInstanceClass() {
        return GDALReflection.fetchGDALLibraryClass(CLASS_NAME);
    }

    /**
     * Calls the JNI GDAL GCP class getGCPX() method
     *
     * @return the JNI GDAL GCP class getGCPX() method result
     */
    public Double getGCPX() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "getGCPX", Double.class, this.jniGCPInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL GCP class getGCPY() method
     *
     * @return the JNI GDAL GCP class getGCPY() method result
     */
    public Double getGCPY() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "getGCPY", Double.class, this.jniGCPInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL GCP class getGCPZ() method
     *
     * @return the JNI GDAL GCP class getGCPZ() method result
     */
    public Double getGCPZ() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "getGCPZ", Double.class, this.jniGCPInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL GCP class getGCPPixel() method
     *
     * @return the JNI GDAL GCP class getGCPPixel() method result
     */
    public Double getGCPPixel() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "getGCPPixel", Double.class, this.jniGCPInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL GCP class getGCPLine() method
     *
     * @return the JNI GDAL GCP class getGCPLine() method result
     */
    public Double getGCPLine() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "getGCPLine", Double.class, this.jniGCPInstance, new Class[]{}, new Object[]{});
    }

}
