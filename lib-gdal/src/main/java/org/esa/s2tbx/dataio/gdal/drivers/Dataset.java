package org.esa.s2tbx.dataio.gdal.drivers;

import java.util.Hashtable;
import java.util.Vector;

/**
 * GDAL Dataset JNI driver class
 *
 * @author Adrian Drăghici
 */
public class Dataset {

    /**
     * The name of JNI GDAL Dataset class
     */
    private static final String CLASS_NAME = "org.gdal.gdal.Dataset";

    private Object jniDatasetInstance;

    /**
     * Creates new instance for this driver
     *
     * @param jniDatasetInstance the JNI GDAL Dataset class instance
     */
    Dataset(Object jniDatasetInstance) {
        this.jniDatasetInstance = jniDatasetInstance;
    }

    public Object getJniDatasetInstance(){
        return jniDatasetInstance;
    }

    /**
     * Calls the JNI GDAL gdal class GetFileList() method
     */
    public Vector getFileList(){
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetFileList", Vector.class, this.jniDatasetInstance, new Class[]{}, new Object[]{});
    }
    /**
     * Calls the JNI GDAL Dataset class GetRasterXSize() method
     *
     * @return the JNI GDAL Dataset class GetRasterXSize() method result
     */
    public Integer getRasterXSize() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetRasterXSize", Integer.class, this.jniDatasetInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Dataset class GetRasterYSize() method
     *
     * @return the JNI GDAL Dataset class GetRasterYSize() method result
     */
    public Integer getRasterYSize() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetRasterYSize", Integer.class, this.jniDatasetInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Dataset class GetRasterCount() method
     *
     * @return the JNI GDAL Dataset class GetRasterCount() method result
     */
    public Integer getRasterCount() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetRasterCount", Integer.class, this.jniDatasetInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Dataset class GetRasterBand(int nBand) method
     *
     * @param nBand the JNI GDAL Dataset class GetRasterBand(int nBand) method 'nBand' argument
     * @return the JNI GDAL Dataset class GetRasterBand(int nBand) method result
     */
    public Band getRasterBand(int nBand) {
        Object jniBandInstance = GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetRasterBand", Object.class, this.jniDatasetInstance, new Class[]{int.class}, new Object[]{nBand});
        if (jniBandInstance != null) {
            return new Band(jniBandInstance);
        }
        return null;
    }

    /**
     * Calls the JNI GDAL Dataset class BuildOverviews(String resampling, int[] overviewlist) method
     *
     * @param resampling   the JNI GDAL Dataset class BuildOverviews(String resampling, int[] overviewlist) method 'resampling' argument
     * @param overviewlist the JNI GDAL Dataset class BuildOverviews(String resampling, int[] overviewlist) method 'overviewlist' argument
     * @return the JNI GDAL Dataset class BuildOverviews(String resampling, int[] overviewlist) method result
     */
    public Integer buildOverviews(String resampling, int[] overviewlist) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "BuildOverviews", Integer.class, this.jniDatasetInstance, new Class[]{String.class, int[].class}, new Object[]{resampling, overviewlist});
    }

    /**
     * Calls the JNI GDAL Dataset class delete() method
     */
    public void delete() {
        GDALReflection.callGDALLibraryMethod(CLASS_NAME, "delete", null, this.jniDatasetInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Dataset class GetProjectionRef() method
     *
     * @return the JNI GDAL Dataset class GetProjectionRef() method result
     */
    public String getProjectionRef() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetProjectionRef", String.class, this.jniDatasetInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Dataset class GetGCPProjection() method
     *
     * @return the JNI GDAL Dataset class GetGCPProjection() method result
     */
    public String getGCPProjection() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetGCPProjection", String.class, this.jniDatasetInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Dataset class GetGeoTransform(double[] argout) method
     *
     * @param argout the JNI GDAL Dataset class GetGeoTransform(double[] argout) method 'argout' argument
     */
    public void getGeoTransform(double[] argout) {
        GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetGeoTransform", null, this.jniDatasetInstance, new Class[]{double[].class}, new Object[]{argout});
    }

    /**
     * Calls the JNI GDAL Dataset class GetDriver() method
     *
     * @return the JNI GDAL Dataset class GetDriver() method result
     */
    public Driver getDriver() {
        Object jniDriverInstance = GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetDriver", Object.class, this.jniDatasetInstance, new Class[]{}, new Object[]{});
        if (jniDriverInstance != null) {
            return new Driver(jniDriverInstance);
        }
        return null;
    }

    /**
     * Calls the JNI GDAL Dataset class GetMetadata_Dict(String pszDomain) method
     *
     * @param pszDomain the JNI GDAL Dataset class GetMetadata_Dict(String pszDomain) method 'pszDomain' argument
     * @return the JNI GDAL Dataset class GetMetadata_Dict(String pszDomain) method result
     */
    public Hashtable getMetadataDict(String pszDomain) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetMetadata_Dict", Hashtable.class, this.jniDatasetInstance, new Class[]{String.class}, new Object[]{pszDomain});
    }

    /**
     * Calls the JNI GDAL Dataset class GetGCPs() method
     *
     * @return the JNI GDAL Dataset class GetGCPs() method result
     */
    public Vector getGCPs() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetGCPs", Vector.class, this.jniDatasetInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Dataset class GetGCPCount() method
     *
     * @return the JNI GDAL Dataset class GetGCPCount() method result
     */
    public Integer getGCPCount() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetGCPCount", Integer.class, this.jniDatasetInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Dataset class SetProjection(String prj) method
     *
     * @param prj the JNI GDAL Dataset class SetProjection(String prj) method 'prj' argument
     * @return the JNI GDAL Dataset class SetProjection(String prj) method result
     */
    public Integer setProjection(String prj) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "SetProjection", Integer.class, this.jniDatasetInstance, new Class[]{String.class}, new Object[]{prj});
    }

    /**
     * Calls the JNI GDAL Dataset class SetGeoTransform(double[] gdalGeoTransform) method
     *
     * @param gdalGeoTransform the JNI GDAL Dataset class SetGeoTransform(double[] gdalGeoTransform) method 'gdalGeoTransform' argument
     * @return the JNI GDAL Dataset class SetGeoTransform(double[] gdalGeoTransform) method result
     */
    public Integer setGeoTransform(double[] gdalGeoTransform) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "SetGeoTransform", Integer.class, this.jniDatasetInstance, new Class[]{double[].class}, new Object[]{gdalGeoTransform});
    }

}
