package org.esa.snap.dataio.gdal.drivers;

import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.Hashtable;
import java.util.Vector;

/**
 * GDAL Dataset JNI driver class
 *
 * @author Adrian Drăghici
 */
public class Dataset extends GDALBase implements Closeable {

    /**
     * The name of JNI GDAL Dataset class
     */
    static final String CLASS_NAME = "org.gdal.gdal.Dataset";
    private static final Class<?> datasetClass;

    private final Object jniDatasetInstance;
    private final MethodHandle getFileListHandle;
    private final MethodHandle getRasterXSizeHandle;
    private final MethodHandle getRasterYSizeHandle;
    private final MethodHandle getRasterCountHandle;
    private final MethodHandle getRasterBandHandle;
    private final MethodHandle buildOverviewsHandle;
    private final MethodHandle deleteHandle;
    private final MethodHandle getProjectionRefHandle;
    private final MethodHandle getGCPProjectionHandle;
    private final MethodHandle getGeoTransformHandle;
    private final MethodHandle getDriverHandle;
    private final MethodHandle getMetadataDictHandle;
    private final MethodHandle getGCPsHandle;
    private final MethodHandle getGCPCountHandle;
    private final MethodHandle setProjectionHandle;
    private final MethodHandle setGeoTransformHandle;
    private final MethodHandle addBandHandle;

    static {
        datasetClass = GDALReflection.fetchGDALLibraryClass(CLASS_NAME);
    }

    public Dataset() {
        this(null);
    }

    /**
     * Creates new instance for this driver
     *
     * @param jniDatasetInstance the JNI GDAL Dataset class instance
     */
    Dataset(Object jniDatasetInstance) {
        this.jniDatasetInstance = jniDatasetInstance;
        try {
            getFileListHandle = createHandle(datasetClass, "GetFileList", Vector.class);
            getRasterXSizeHandle = createHandle(datasetClass, "GetRasterXSize", int.class);
            getRasterYSizeHandle = createHandle(datasetClass, "GetRasterYSize", int.class);
            getRasterCountHandle = createHandle(datasetClass, "GetRasterCount", int.class);
            getRasterBandHandle = createHandle(datasetClass, "GetRasterBand", GDALReflection.fetchGDALLibraryClass(Band.CLASS_NAME), int.class);
            buildOverviewsHandle = createHandle(datasetClass, "BuildOverviews", int.class, String.class, int[].class);
            deleteHandle = createHandle(datasetClass, "delete", void.class);
            getProjectionRefHandle = createHandle(datasetClass, "GetProjectionRef", String.class);
            getGCPProjectionHandle = createHandle(datasetClass, "GetGCPProjection", String.class);
            getGeoTransformHandle = createHandle(datasetClass, "GetGeoTransform", void.class, double[].class);
            getDriverHandle = createHandle(datasetClass, "GetDriver", GDALReflection.fetchGDALLibraryClass(Driver.CLASS_NAME));
            getMetadataDictHandle = createHandle(datasetClass, "GetMetadata_Dict", Hashtable.class, String.class);
            getGCPsHandle = createHandle(datasetClass, "GetGCPs", Vector.class);
            getGCPCountHandle = createHandle(datasetClass, "GetGCPCount", int.class);
            setProjectionHandle = createHandle(datasetClass, "SetProjection", int.class, String.class);
            setGeoTransformHandle = createHandle(datasetClass, "SetGeoTransform", int.class, double[].class);
            addBandHandle = createHandle(datasetClass, "AddBand", int.class, int.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Object getJniDatasetInstance(){
        return jniDatasetInstance;
    }

    /**
     * Calls the JNI GDAL gdal class GetFileList() method
     */
    public Vector getFileList(){
        return (Vector) invoke(getFileListHandle, this.jniDatasetInstance);
    }
    /**
     * Calls the JNI GDAL Dataset class GetRasterXSize() method
     *
     * @return the JNI GDAL Dataset class GetRasterXSize() method result
     */
    public Integer getRasterXSize() {
        return (Integer) invoke(getRasterXSizeHandle, this.jniDatasetInstance);
    }

    /**
     * Calls the JNI GDAL Dataset class GetRasterYSize() method
     *
     * @return the JNI GDAL Dataset class GetRasterYSize() method result
     */
    public Integer getRasterYSize() {
        return (Integer) invoke(getRasterYSizeHandle, this.jniDatasetInstance);
    }

    /**
     * Calls the JNI GDAL Dataset class GetRasterCount() method
     *
     * @return the JNI GDAL Dataset class GetRasterCount() method result
     */
    public Integer getRasterCount() {
        return (Integer) invoke(getRasterCountHandle, this.jniDatasetInstance);
    }

    /**
     * Calls the JNI GDAL Dataset class GetRasterBand(int nBand) method
     *
     * @param nBand the JNI GDAL Dataset class GetRasterBand(int nBand) method 'nBand' argument
     * @return the JNI GDAL Dataset class GetRasterBand(int nBand) method result
     */
    public Band getRasterBand(int nBand) {
        Object jniBandInstance = invoke(getRasterBandHandle, this.jniDatasetInstance, nBand);
        return jniBandInstance != null ? new Band(jniBandInstance): null;
    }

    /**
     * Calls the JNI GDAL Dataset class BuildOverviews(String resampling, int[] overviewlist) method
     *
     * @param resampling   the JNI GDAL Dataset class BuildOverviews(String resampling, int[] overviewlist) method 'resampling' argument
     * @param overviewList the JNI GDAL Dataset class BuildOverviews(String resampling, int[] overviewlist) method 'overviewlist' argument
     * @return the JNI GDAL Dataset class BuildOverviews(String resampling, int[] overviewlist) method result
     */
    public Integer buildOverviews(String resampling, int[] overviewList) {
        return (Integer) invoke(buildOverviewsHandle, this.jniDatasetInstance ,resampling, overviewList);
    }

    /**
     * Calls the JNI GDAL Dataset class delete() method
     */
    public void delete() {
        invoke(deleteHandle, this.jniDatasetInstance);
    }

    /**
     * Calls the JNI GDAL Dataset class GetProjectionRef() method
     *
     * @return the JNI GDAL Dataset class GetProjectionRef() method result
     */
    public String getProjectionRef() {
        return (String) invoke(getProjectionRefHandle, this.jniDatasetInstance);
    }

    /**
     * Calls the JNI GDAL Dataset class GetGCPProjection() method
     *
     * @return the JNI GDAL Dataset class GetGCPProjection() method result
     */
    public String getGCPProjection() {
        return (String) invoke(getGCPProjectionHandle, this.jniDatasetInstance);
    }

    /**
     * Calls the JNI GDAL Dataset class GetGeoTransform(double[] argout) method
     *
     * @param argout the JNI GDAL Dataset class GetGeoTransform(double[] argout) method 'argout' argument
     */
    public void getGeoTransform(double[] argout) {
        invoke(getGeoTransformHandle, this.jniDatasetInstance, (Object) argout);
    }

    /**
     * Calls the JNI GDAL Dataset class GetDriver() method
     *
     * @return the JNI GDAL Dataset class GetDriver() method result
     */
    public Driver getDriver() {
        Object jniDriverInstance = invoke(getDriverHandle, this.jniDatasetInstance);
        return jniDriverInstance != null ? new Driver(jniDriverInstance) : null;
    }

    /**
     * Calls the JNI GDAL Dataset class GetMetadata_Dict(String pszDomain) method
     *
     * @param pszDomain the JNI GDAL Dataset class GetMetadata_Dict(String pszDomain) method 'pszDomain' argument
     * @return the JNI GDAL Dataset class GetMetadata_Dict(String pszDomain) method result
     */
    public Hashtable getMetadataDict(String pszDomain) {
        return (Hashtable) invoke(getMetadataDictHandle, this.jniDatasetInstance, pszDomain);
    }

    /**
     * Calls the JNI GDAL Dataset class GetGCPs() method
     *
     * @return the JNI GDAL Dataset class GetGCPs() method result
     */
    public Vector getGCPs() {
        return (Vector) invoke(getGCPsHandle, this.jniDatasetInstance);
    }

    /**
     * Calls the JNI GDAL Dataset class GetGCPCount() method
     *
     * @return the JNI GDAL Dataset class GetGCPCount() method result
     */
    public Integer getGCPCount() {
        return (Integer) invoke(getGCPCountHandle, this.jniDatasetInstance);
    }

    /**
     * Calls the JNI GDAL Dataset class SetProjection(String prj) method
     *
     * @param prj the JNI GDAL Dataset class SetProjection(String prj) method 'prj' argument
     * @return the JNI GDAL Dataset class SetProjection(String prj) method result
     */
    public Integer setProjection(String prj) {
        return (Integer) invoke(setProjectionHandle, this.jniDatasetInstance, prj);
    }

    /**
     * Calls the JNI GDAL Dataset class SetGeoTransform(double[] gdalGeoTransform) method
     *
     * @param gdalGeoTransform the JNI GDAL Dataset class SetGeoTransform(double[] gdalGeoTransform) method 'gdalGeoTransform' argument
     * @return the JNI GDAL Dataset class SetGeoTransform(double[] gdalGeoTransform) method result
     */
    public Integer setGeoTransform(double[] gdalGeoTransform) {
        return (Integer) invoke(setGeoTransformHandle, this.jniDatasetInstance, (Object) gdalGeoTransform);
    }

    public Integer addBand(int dataType) {
        return (Integer) invoke(addBandHandle, this.jniDatasetInstance, dataType);
    }

    @Override
    public void close() throws IOException {
        delete();
    }
}
