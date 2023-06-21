package org.esa.snap.dataio.gdal.drivers;

import java.lang.reflect.Array;

/**
 * GDAL gdal JNI driver class
 *
 * @author Adrian Drăghici
 */
public class GDAL {

    /**
     * The name of JNI GDAL gdal class
     */
    private static final String CLASS_NAME = "org.gdal.gdal.gdal";

    /**
     * Creates new instance for this driver
     */
    private GDAL() {
        //nothing to init
    }

    /**
     * Calls the JNI GDAL gdal class AllRegister() method
     */
    public static void allRegister() {
        GDALReflection.callGDALLibraryMethod(CLASS_NAME, "AllRegister", null, null, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL gdal class GetDataTypeName(int gdalDataType) method
     *
     * @param gdalDataType the JNI GDAL gdal class GetDataTypeName(int gdalDataType) method 'gdalDataType' argument
     * @return the JNI GDAL gdal class GetDataTypeName(int gdalDataType) method result
     */
    public static String getDataTypeName(int gdalDataType) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetDataTypeName", String.class, null, new Class[]{int.class}, new Object[]{gdalDataType});
    }

    /**
     * Calls the JNI GDAL gdal class GetDataTypeByName(String pszDataTypeName) method
     *
     * @param pszDataTypeName the JNI GDAL gdal class GetDataTypeByName(String pszDataTypeName) method 'arg' pszDataTypeName
     * @return the JNI GDAL gdal class GetDataTypeByName(String pszDataTypeName) method result
     */
    public static Integer getDataTypeByName(String pszDataTypeName) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetDataTypeByName", Integer.class, null, new Class[]{String.class}, new Object[]{pszDataTypeName});
    }

    /**
     * Calls the JNI GDAL gdal class Open(String utf8Path, int eAccess) method
     *
     * @param utf8Path the JNI GDAL gdal class Open(String utf8Path, int eAccess) method 'utf8Path' argument
     * @param eAccess the JNI GDAL gdal class Open(String utf8Path, int eAccess) method 'eAccess' argument
     * @return the JNI GDAL gdal class Open(String utf8Path, int eAccess) method result
     */
    public static Dataset open(String utf8Path, int eAccess) {
        Object jniDatasetInstance = GDALReflection.callGDALLibraryMethod(CLASS_NAME, "Open", Object.class, null, new Class[]{String.class, int.class}, new Object[]{utf8Path, eAccess});
        if (jniDatasetInstance != null) {
            return new Dataset(jniDatasetInstance);
        }
        return null;
    }

    /**
     * Calls the JNI GDAL gdal class GetColorInterpretationName(int eColorInterp) method
     *
     * @param eColorInterp the JNI GDAL gdal class GetColorInterpretationName(int eColorInterp) method 'eColorInterp' argument
     * @return the JNI GDAL gdal class GetColorInterpretationName(int eColorInterp) method result
     */
    public static String getColorInterpretationName(int eColorInterp) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetColorInterpretationName", String.class, null, new Class[]{int.class}, new Object[]{eColorInterp});
    }

    /**
     * Calls the JNI GDAL gdal class GetDataTypeSize(int eDataType) method
     *
     * @param eDataType the JNI GDAL gdal class GetDataTypeSize(int eDataType) method 'eDataType' argument
     * @return the JNI GDAL gdal class GetDataTypeSize(int eDataType) method result
     */
    public static Integer getDataTypeSize(int eDataType) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetDataTypeSize", Integer.class, null, new Class[]{int.class}, new Object[]{eDataType});
    }

    /**
     * Calls the JNI GDAL gdal class GetDriverByName(String name) method
     *
     * @param name the JNI GDAL gdal class GetDriverByName(String name) method 'name' argument
     * @return the JNI GDAL gdal class GetDriverByName(String name) method result
     */
    public static Driver getDriverByName(String name) {
        Object jniDriverInstance = GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetDriverByName", Object.class, null, new Class[]{String.class}, new Object[]{name});
        if (jniDriverInstance != null) {
            return new Driver(jniDriverInstance);
        }
        return null;
    }

    /**
     * Calls the JNI GDAL gdal class BuildVRT(String dest, Dataset[] object_list_count, BuildVRTOptions options) method
     *
     * @param dest            the JNI GDAL gdal class BuildVRT(String dest, Dataset[] object_list_count, BuildVRTOptions options) method 'dest' argument
     * @param objectListCount the JNI GDAL gdal class BuildVRT(String dest, Dataset[] object_list_count, BuildVRTOptions options) method 'object_list_count' argument
     * @param options         the JNI GDAL gdal class BuildVRT(String dest, Dataset[] object_list_count, BuildVRTOptions options) method 'options' argument
     * @return the JNI GDAL gdal class BuildVRT(String dest, Dataset[] object_list_count, BuildVRTOptions options) method result
     */
    public static Dataset buildVRT(String dest, Dataset[] objectListCount, BuildVRTOptions options) {
        Object objectListCountJni = Array.newInstance(objectListCount[0].getJniDatasetInstance().getClass(), objectListCount.length);

        for (int i = 0; i < objectListCount.length; i++) {
            Array.set(objectListCountJni, i, objectListCount[i].getJniDatasetInstance());
        }
        Object jniDatasetInstance = GDALReflection.callGDALLibraryMethod(CLASS_NAME, "BuildVRT", Object.class, null, new Class[]{dest.getClass(), objectListCountJni.getClass(), options.getJniBuildVRTOptionsInstance().getClass()}, new Object[]{dest, objectListCountJni, options.getJniBuildVRTOptionsInstance()});
        if (jniDatasetInstance != null) {
            return new Dataset(jniDatasetInstance);
        }
        return null;
    }

    public static String getLastErrorMsg(){
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetLastErrorMsg", String.class, null, new Class[]{}, new Object[]{});
    }
}
