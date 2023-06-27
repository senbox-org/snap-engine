package org.esa.snap.dataio.gdal.drivers;

import java.lang.invoke.MethodHandle;

/**
 * GDAL GCP JNI driver class
 *
 * @author Adrian Drăghici
 */
public class GCP extends GDALBase {

    /**
     * The name of JNI GDAL GCP class
     */
    private static final String CLASS_NAME = "org.gdal.gdal.GCP";
    private static final Class<?> gcpClass;

    static {
        gcpClass = GDALReflection.fetchGDALLibraryClass(CLASS_NAME);
    }

    private final Object jniGCPInstance;
    private final MethodHandle getGCPXHandle;
    private final MethodHandle getGCPYHandle;
    private final MethodHandle getGCPZHandle;
    private final MethodHandle getGCPPixelHandle;
    private final MethodHandle getGCPLineHandle;

    /**
     * Creates new instance for this driver
     *
     * @param jniGCPInstance the JNI GDAL GCP class instance
     */
    public GCP(Object jniGCPInstance) {
        this.jniGCPInstance = jniGCPInstance;
        try {
            getGCPXHandle = createHandle(gcpClass, "getGCPX", double.class);
            getGCPYHandle = createHandle(gcpClass, "getGCPY", double.class);
            getGCPZHandle = createHandle(gcpClass, "getGCPZ", double.class);
            getGCPPixelHandle = createHandle(gcpClass, "getGCPPixel", double.class);
            getGCPLineHandle = createHandle(gcpClass, "getGCPLine", double.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calls the JNI GDAL GCP class getGCPX() method
     *
     * @return the JNI GDAL GCP class getGCPX() method result
     */
    public Double getGCPX() {
        return (Double) invoke(getGCPXHandle, this.jniGCPInstance);
    }

    /**
     * Calls the JNI GDAL GCP class getGCPY() method
     *
     * @return the JNI GDAL GCP class getGCPY() method result
     */
    public Double getGCPY() {
        return (Double) invoke(getGCPYHandle, this.jniGCPInstance);
    }

    /**
     * Calls the JNI GDAL GCP class getGCPZ() method
     *
     * @return the JNI GDAL GCP class getGCPZ() method result
     */
    public Double getGCPZ() {
        return (Double) invoke(getGCPZHandle, this.jniGCPInstance);
    }

    /**
     * Calls the JNI GDAL GCP class getGCPPixel() method
     *
     * @return the JNI GDAL GCP class getGCPPixel() method result
     */
    public Double getGCPPixel() {
        return (Double) invoke(getGCPPixelHandle, this.jniGCPInstance);
    }

    /**
     * Calls the JNI GDAL GCP class getGCPLine() method
     *
     * @return the JNI GDAL GCP class getGCPLine() method result
     */
    public Double getGCPLine() {
        return (Double) invoke(getGCPLineHandle, this.jniGCPInstance);
    }

}
