package org.esa.snap.dataio.gdal.drivers;

import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandle;

/**
 * GDAL GCP JNI driver class
 *
 * @author Adrian Drăghici
 */
public class GCP extends GDALBase implements Closeable {

    /**
     * The name of JNI GDAL GCP class
     */
    static final String CLASS_NAME = "org.gdal.gdal.GCP";
    static final String GCP_ARRAY_CLASS_NAME = "[Lorg.gdal.gdal.GCP;";
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
    private final MethodHandle deleteHandle;

    /**
     * Creates a new GDAL Ground Control Point (GCP) wrapper instance.
     * @param pixel the raster pixel (column) coordinate associated with the GCP
     * @param line  the raster line (row) coordinate associated with the GCP
     * @param x     the geographic or projected X coordinate (longitude when using EPSG:4326)
     * @param y     the geographic or projected Y coordinate (latitude when using EPSG:4326)
     * @param z     the elevation value associated with the GCP (typically 0.0)
     *
     * @return a {@code GCP} wrapper containing the newly created GDAL JNI GCP instance
     */
    public static GCP create(double pixel, double line, double x, double y, double z) {
        Object jni = GDALReflection.fetchGDALLibraryClassInstance(CLASS_NAME,
                                                                    new Class[]{double.class, double.class, double.class, double.class, double.class},
                                                                    new Object[]{x, y, z, pixel, line}
                                                                );
        return new GCP(jni);
    }

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
            deleteHandle = createHandle(gcpClass, "delete", void.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the JNI GDAL GCP class instance
     * @return the JNI GCP instance
     */
    public Object getJniGCPInstance() {
        return jniGCPInstance;
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

    /**
     * Calls the JNI GDAL Dataset class delete() method
     */
    public void delete() {
        invoke(deleteHandle, this.jniGCPInstance);
    }

    @Override
    public void close() throws IOException {
        delete();
    }
}
