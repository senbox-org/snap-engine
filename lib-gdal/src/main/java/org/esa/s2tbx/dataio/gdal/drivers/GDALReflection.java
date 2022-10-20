package org.esa.s2tbx.dataio.gdal.drivers;

import org.esa.s2tbx.dataio.gdal.GDALLoader;

import java.lang.reflect.Method;

/**
 * GDAL Reflection class which uses Java reflection API to invoke methods from JNI GDAL
 */
class GDALReflection {

    /**
     * Creates new instance for this class
     */
    private GDALReflection() {
        //nothing to init
    }

    /**
     * Fetches the GDAL constants from JNI GDAL class
     *
     * @param className    the target JNI GDAL class name
     * @param constantName the target GDAL constant name
     * @param type         the target GDAL constant type
     * @param <T>          the target GDAL constant data type parameter
     * @return the GDAL constant
     */
    static <T> T fetchGDALLibraryConstant(String className, String constantName, Class<T> type) {
        try {
            Class<?> gdalconstConstantsClass = fetchGDALLibraryClass(className);
            return type.cast(gdalconstConstantsClass.getField(constantName).get(null));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Calls the JNI GDAL class method using Java reflection API
     *
     * @param className      the target JNI GDAL class name
     * @param methodName     the target JNI GDAL class method name
     * @param returnType     the target GDAL class method return type
     * @param instance       the target JNI GDAL class instance
     * @param argumentsTypes the target GDAL class method arguments types
     * @param arguments      the target GDAL class method arguments values
     * @param <T>            the target GDAL data type parameter
     * @return the result returned by JNI GDAL class method
     */
    static <T> T callGDALLibraryMethod(String className, String methodName, Class<T> returnType, Object instance, Class[] argumentsTypes, Object[] arguments) {
        try {
            Class<?> gdalClass = fetchGDALLibraryClass(className);
            Method gdalClassMethod = gdalClass.getMethod(methodName, argumentsTypes);
            Object returnResult = gdalClassMethod.invoke(instance, arguments);
            if (returnResult != null && returnType != null) {
                return returnType.cast(returnResult);
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return null;
    }

    static Class<?> fetchGDALLibraryClass(String className) {
        try {
            return Class.forName(className, false, GDALLoader.getInstance().getGDALVersionLoader());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
