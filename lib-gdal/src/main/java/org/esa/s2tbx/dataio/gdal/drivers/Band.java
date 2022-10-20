package org.esa.s2tbx.dataio.gdal.drivers;

import java.nio.ByteBuffer;

/**
 * GDAL Band JNI driver class
 *
 * @author Adrian Drăghici
 */
public class Band {

    /**
     * The name of JNI GDAL Band class
     */
    private static final String CLASS_NAME = "org.gdal.gdal.Band";

    private Object jniBandInstance;

    /**
     * Creates new instance for this driver
     *
     * @param jniBandInstance the JNI GDAL Band class instance
     */
    public Band(Object jniBandInstance) {
        this.jniBandInstance = jniBandInstance;
    }

    /**
     * Calls the JNI GDAL Band class delete() method
     */
    public void delete() {
        GDALReflection.callGDALLibraryMethod(CLASS_NAME, "delete", null, this.jniBandInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Band class getDataType() method
     *
     * @return the JNI GDAL Band class getDataType() method result
     */
    public Integer getDataType() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "getDataType", Integer.class, this.jniBandInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Band class GetBlockXSize() method
     *
     * @return the JNI GDAL Band class GetBlockXSize() method result
     */
    public Integer getBlockXSize() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetBlockXSize", Integer.class, this.jniBandInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Band class GetBlockYSize() method
     *
     * @return the JNI GDAL Band class GetBlockYSize() method result
     */
    public Integer getBlockYSize() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetBlockYSize", Integer.class, this.jniBandInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Band class GetXSize() method
     *
     * @return the JNI GDAL Band class GetXSize() method result
     */
    public Integer getXSize() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetXSize", Integer.class, this.jniBandInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Band class GetYSize() method
     *
     * @return the JNI GDAL Band class GetYSize() method result
     */
    public Integer getYSize() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetYSize", Integer.class, this.jniBandInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Band class GetOverviewCount() method
     *
     * @return the JNI GDAL Band class GetOverviewCount() method result
     */
    public Integer getOverviewCount() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetOverviewCount", Integer.class, this.jniBandInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Band class GetRasterColorInterpretation() method
     *
     * @return the JNI GDAL Band class GetRasterColorInterpretation() method result
     */
    public Integer getRasterColorInterpretation() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetRasterColorInterpretation", Integer.class, this.jniBandInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Band class GetRasterColorTable() method
     *
     * @return the JNI GDAL Band class GetRasterColorTable() method result
     */
    public ColorTable getRasterColorTable() {
        Object jniColorTable = GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetRasterColorTable", Object.class, this.jniBandInstance, new Class[]{}, new Object[]{});
        if (jniColorTable != null) {
            return new ColorTable(jniColorTable);
        }
        return null;
    }

    /**
     * Calls the JNI GDAL Band class GetDescription() method
     *
     * @return the JNI GDAL Band class GetDescription() method result
     */
    public String getDescription() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetDescription", String.class, this.jniBandInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Band class GetOverview(int i) method
     *
     * @param i the JNI GDAL Band class GetOverview(int i) method 'i' argument
     * @return the JNI GDAL Band class GetOverview(int i) method result
     */
    public Band getOverview(int i) {
        Object newJNIBandInstance = GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetOverview", Object.class, this.jniBandInstance, new Class[]{int.class}, new Object[]{i});
        if (newJNIBandInstance != null) {
            return new Band(newJNIBandInstance);
        }
        return null;
    }

    /**
     * Calls the JNI GDAL Band class GetOffset(Double[] val) method
     *
     * @param val the JNI GDAL Band class GetOffset(Double[] val) method 'val' argument
     */
    public void getOffset(Double[] val) {
        GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetOffset", null, this.jniBandInstance, new Class[]{Double[].class}, new Object[]{val});
    }

    /**
     * Calls the JNI GDAL Band class GetScale(Double[] val) method
     *
     * @param val the JNI GDAL Band class GetScale(Double[] val) method 'val' argument
     */
    public void getScale(Double[] val) {
        GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetScale", null, this.jniBandInstance, new Class[]{Double[].class}, new Object[]{val});
    }

    /**
     * Calls the JNI GDAL Band class GetUnitType() method
     *
     * @return the JNI GDAL Band class GetUnitType() method result
     */
    public String getUnitType() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetUnitType", String.class, this.jniBandInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Band class GetNoDataValue(Double[] val) method
     *
     * @param val the JNI GDAL Band class GetNoDataValue(Double[] val) method 'val' argument
     */
    public void getNoDataValue(Double[] val) {
        GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetNoDataValue", null, this.jniBandInstance, new Class[]{Double[].class}, new Object[]{val});
    }

    /**
     * Calls the JNI GDAL Band class GetMaskBand() method
     *
     * @return the JNI GDAL Band class GetMaskBand() method result
     */
    public Band getMaskBand() {
        Object newJNIBandInstance = GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetMaskBand", Object.class, this.jniBandInstance, new Class[]{}, new Object[]{});
        if (newJNIBandInstance != null) {
            return new Band(newJNIBandInstance);
        }
        return null;
    }

    /**
     * Calls the JNI GDAL Band class GetMaskFlags() method
     *
     * @return the JNI GDAL Band class GetMaskFlags() method result
     */
    public Integer getMaskFlags() {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "GetMaskFlags", Integer.class, this.jniBandInstance, new Class[]{}, new Object[]{});
    }

    /**
     * Calls the JNI GDAL Band class ReadBlock_Direct(int nXBlockOff, int nYBlockOff, ByteBuffer nioBuffer) method
     *
     * @param nXBlockOff the JNI GDAL Band class ReadBlock_Direct(int nXBlockOff, int nYBlockOff, ByteBuffer nioBuffer) method 'nXBlockOff' argument
     * @param nYBlockOff the JNI GDAL Band class ReadBlock_Direct(int nXBlockOff, int nYBlockOff, ByteBuffer nioBuffer) method 'nYBlockOff' argument
     * @param nioBuffer  the JNI GDAL Band class ReadBlock_Direct(int nXBlockOff, int nYBlockOff, ByteBuffer nioBuffer) method 'nioBuffer' argument
     * @return the JNI GDAL Band class ReadBlock_Direct(int nXBlockOff, int nYBlockOff, ByteBuffer nioBuffer) method result
     */
    public Integer readBlockDirect(int nXBlockOff, int nYBlockOff, ByteBuffer nioBuffer) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "ReadBlock_Direct", Integer.class, this.jniBandInstance, new Class[]{int.class, int.class, ByteBuffer.class}, new Object[]{nXBlockOff, nYBlockOff, nioBuffer});
    }

    /**
     * Calls the JNI GDAL Band class ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize, int buf_type, ByteBuffer nioBuffer) method
     *
     * @param xoff      the JNI GDAL Band class ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize, int buf_type, ByteBuffer nioBuffer) method 'xoff' argument
     * @param yoff      the JNI GDAL Band class ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize, int buf_type, ByteBuffer nioBuffer) method 'yoff' argument
     * @param xsize     the JNI GDAL Band class ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize, int buf_type, ByteBuffer nioBuffer) method 'xsize' argument
     * @param ysize     the JNI GDAL Band class ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize, int buf_type, ByteBuffer nioBuffer) method 'ysize' argument
     * @param bufxsize  the JNI GDAL Band class ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize, int buf_type, ByteBuffer nioBuffer) method 'buf_xsize' argument
     * @param bufysize  the JNI GDAL Band class ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize, int buf_type, ByteBuffer nioBuffer) method 'buf_ysize' argument
     *                  @param buftype  the JNI GDAL Band class ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize, int buf_type, ByteBuffer nioBuffer) method 'buf_type' argument
     * @param nioBuffer the JNI GDAL Band class ReadBlock_Direct(int nXBlockOff, int nYBlockOff, ByteBuffer nioBuffer) method 'nioBuffer' argument
     * @return the JNI GDAL Band class ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize, int buf_type, ByteBuffer nioBuffer) method result
     */
    public Integer readRasterDirect(int xoff, int yoff, int xsize, int ysize, int bufxsize, int bufysize, int buftype, ByteBuffer nioBuffer) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "ReadRaster_Direct", Integer.class, this.jniBandInstance, new Class[]{int.class, int.class, int.class, int.class, int.class, int.class, int.class, ByteBuffer.class}, new Object[]{xoff, yoff, xsize, ysize, bufxsize, bufysize, buftype, nioBuffer});
    }

    /**
     * Calls the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, byte[] array) method
     *
     * @param xoff    the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, byte[] array) method 'xoff' argument
     * @param yoff    the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, byte[] array) method 'yoff' argument
     * @param xsize   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, byte[] array) method 'xsize' argument
     * @param ysize   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, byte[] array) method 'ysize' argument
     * @param bufType the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, byte[] array) method 'bufType' argument
     * @param array   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, byte[] array) method 'array' argument
     * @return the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, byte[] array) method result
     */
    public Integer writeRaster(int xoff, int yoff, int xsize, int ysize, int bufType, byte[] array) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "WriteRaster", Integer.class, this.jniBandInstance, new Class[]{int.class, int.class, int.class, int.class, int.class, byte[].class}, new Object[]{xoff, yoff, xsize, ysize, bufType, array});
    }

    /**
     * Calls the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, short[] array) method
     *
     * @param xoff    the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, short[] array) method 'xoff' argument
     * @param yoff    the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, short[] array) method 'yoff' argument
     * @param xsize   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, short[] array) method 'xsize' argument
     * @param ysize   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, short[] array) method 'ysize' argument
     * @param bufType the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, short[] array) method 'bufType' argument
     * @param array   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, short[] array) method 'array' argument
     * @return the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, short[] array) method result
     */
    public Integer writeRaster(int xoff, int yoff, int xsize, int ysize, int bufType, short[] array) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "WriteRaster", Integer.class, this.jniBandInstance, new Class[]{int.class, int.class, int.class, int.class, int.class, short[].class}, new Object[]{xoff, yoff, xsize, ysize, bufType, array});
    }

    /**
     * Calls the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, int[] array) method
     *
     * @param xoff    the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, int[] array) method 'xoff' argument
     * @param yoff    the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, int[] array) method 'yoff' argument
     * @param xsize   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, int[] array) method 'xsize' argument
     * @param ysize   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, int[] array) method 'ysize' argument
     * @param bufType the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, int[] array) method 'bufType' argument
     * @param array   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, int[] array) method 'array' argument
     * @return the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, int[] array) method result
     */
    public Integer writeRaster(int xoff, int yoff, int xsize, int ysize, int bufType, int[] array) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "WriteRaster", Integer.class, this.jniBandInstance, new Class[]{int.class, int.class, int.class, int.class, int.class, int[].class}, new Object[]{xoff, yoff, xsize, ysize, bufType, array});
    }

    /**
     * Calls the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, float[] array) method
     *
     * @param xoff    the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, float[] array) method 'xoff' argument
     * @param yoff    the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, float[] array) method 'yoff' argument
     * @param xsize   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, float[] array) method 'xsize' argument
     * @param ysize   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, float[] array) method 'ysize' argument
     * @param bufType the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, float[] array) method 'bufType' argument
     * @param array   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, float[] array) method 'array' argument
     * @return the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, float[] array) method result
     */
    public Integer writeRaster(int xoff, int yoff, int xsize, int ysize, int bufType, float[] array) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "WriteRaster", Integer.class, this.jniBandInstance, new Class[]{int.class, int.class, int.class, int.class, int.class, float[].class}, new Object[]{xoff, yoff, xsize, ysize, bufType, array});
    }

    /**
     * Calls the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, double[] array) method
     *
     * @param xoff    the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, double[] array) method 'xoff' argument
     * @param yoff    the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, double[] array) method 'yoff' argument
     * @param xsize   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, double[] array) method 'xsize' argument
     * @param ysize   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, double[] array) method 'ysize' argument
     * @param bufType the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, double[] array) method 'bufType' argument
     * @param array   the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, double[] array) method 'array' argument
     * @return the JNI GDAL Band class WriteRaster(int xoff, int yoff, int xsize, int ysize, int bufType, double[] array) method result
     */
    public Integer writeRaster(int xoff, int yoff, int xsize, int ysize, int bufType, double[] array) {
        return GDALReflection.callGDALLibraryMethod(CLASS_NAME, "WriteRaster", Integer.class, this.jniBandInstance, new Class[]{int.class, int.class, int.class, int.class, int.class, double[].class}, new Object[]{xoff, yoff, xsize, ysize, bufType, array});
    }
}
