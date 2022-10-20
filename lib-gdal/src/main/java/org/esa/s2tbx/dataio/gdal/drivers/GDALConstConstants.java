package org.esa.s2tbx.dataio.gdal.drivers;

/**
 * GDAL gdalconstConstants JNI driver class
 *
 * @author Adrian Drăghici
 */
public class GDALConstConstants {

    /**
     * The name of JNI GDAL gdalconstConstants class
     */
    private static final String CLASS_NAME = "org.gdal.gdalconst.gdalconstConstants";

    /**
     * Creates new instance for this driver
     */
    private GDALConstConstants() {
        //nothing to init
    }

    /**
     * Fetches the JNI GDAL GDT_Byte constant
     *
     * @return the JNI GDAL GDT_Byte constant
     */
    public static Integer gdtByte() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GDT_Byte", Integer.class);
    }

    /**
     * Calls the JNI GDAL GDT_Int16 constant
     *
     * @return the JNI GDAL GDT_Int16 constant
     */
    public static Integer gdtInt16() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GDT_Int16", Integer.class);
    }

    /**
     * Calls the JNI GDAL GDT_UInt16 constant
     *
     * @return the JNI GDAL GDT_UInt16 constant
     */
    public static Integer gdtUint16() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GDT_UInt16", Integer.class);
    }

    /**
     * Calls the JNI GDAL GDT_Int32 constant
     *
     * @return the JNI GDAL GDT_Int32 constant
     */
    public static Integer gdtInt32() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GDT_Int32", Integer.class);
    }

    /**
     * Calls the JNI GDAL GDT_UInt32 constant
     *
     * @return the JNI GDAL GDT_UInt32 constant
     */
    public static Integer gdtUint32() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GDT_UInt32", Integer.class);
    }

    /**
     * Calls the JNI GDAL GDT_Float32 constant
     *
     * @return the JNI GDAL GDT_Float32 constant
     */
    public static Integer gdtFloat32() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GDT_Float32", Integer.class);
    }

    /**
     * Calls the JNI GDAL GDT_Float64 constant
     *
     * @return the JNI GDAL GDT_Float64 constant
     */
    public static Integer gdtFloat64() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GDT_Float64", Integer.class);
    }

    /**
     * Calls the JNI GDAL GDT_CInt16 constant
     *
     * @return the JNI GDAL GDT_CInt16 constant
     */
    public static Integer gdtCInt16() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GDT_CInt16", Integer.class);
    }

    /**
     * Calls the JNI GDAL GDT_CInt32 constant
     *
     * @return the JNI GDAL GDT_CInt32 constant
     */
    public static Integer gdtCInt32() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GDT_CInt32", Integer.class);
    }

    /**
     * Calls the JNI GDAL GDT_CFloat32 constant
     *
     * @return the JNI GDAL GDT_CFloat32 constant
     */
    public static Integer gdtCFloat32() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GDT_CFloat32", Integer.class);
    }

    /**
     * Calls the JNI GDAL GDT_CFloat64 constant
     *
     * @return the JNI GDAL GDT_CFloat64 constant
     */
    public static Integer gdtCFloat64() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GDT_CFloat64", Integer.class);
    }

    /**
     * Calls the JNI GDAL GMF_NODATA constant
     *
     * @return the JNI GDAL GMF_NODATA constant
     */
    public static Integer gmfNodata() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GMF_NODATA", Integer.class);
    }

    /**
     * Calls the JNI GDAL GMF_PER_DATASET constant
     *
     * @return the JNI GDAL GMF_PER_DATASET constant
     */
    public static Integer gmfPerDataset() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GMF_PER_DATASET", Integer.class);
    }

    /**
     * Calls the JNI GDAL GMF_ALPHA constant
     *
     * @return the JNI GDAL GMF_ALPHA constant
     */
    public static Integer gmfAlpha() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GMF_ALPHA", Integer.class);
    }

    /**
     * Calls the JNI GDAL GMF_ALL_VALID constant
     *
     * @return the JNI GDAL GMF_ALL_VALID constant
     */
    public static Integer gmfAllValid() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GMF_ALL_VALID", Integer.class);
    }

    /**
     * Calls the JNI GDAL CE_None constant
     *
     * @return the JNI GDAL CE_None constant
     */
    public static Integer ceNone() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "CE_None", Integer.class);
    }

    /**
     * Calls the JNI GDAL GCI_PaletteIndex constant
     *
     * @return the JNI GDAL GCI_PaletteIndex constant
     */
    public static Integer gciPaletteindex() {
        return GDALReflection.fetchGDALLibraryConstant(CLASS_NAME, "GCI_PaletteIndex", Integer.class);
    }
}
