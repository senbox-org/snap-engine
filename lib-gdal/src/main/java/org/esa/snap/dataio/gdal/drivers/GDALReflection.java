package org.esa.snap.dataio.gdal.drivers;

import org.esa.snap.dataio.gdal.GDALLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * GDAL Reflection class which uses Java reflection API to invoke methods from JNI GDAL
 */
class GDALReflection {
    private static final Map<String, Integer> cachedConstants = Collections.synchronizedMap(new HashMap<>());

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
     * @return the GDAL constant
     */
    static Integer fetchGDALLibraryConstant(String className, String constantName) {
        try {
            if (!cachedConstants.containsKey(constantName)) {
                Class<?> gdalconstConstantsClass = fetchGDALLibraryClass(className);
                cachedConstants.put(constantName, (Integer) gdalconstConstantsClass.getField(constantName).get(null));
            }
            return cachedConstants.get(constantName);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    static Class<?> fetchGDALLibraryClass(String className) {
        GDALLoader.ensureGDALInitialised();
        try {
            return Class.forName(className, false, GDALLoader.getInstance().getGDALVersionLoader());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    static Object fetchGDALLibraryClassInstance(String className, Class[] argumentsTypes, Object[] arguments) {
        try {
            Class<?> gdalClass = fetchGDALLibraryClass(className);
            Constructor gdalClassConstructor = gdalClass.getConstructor(argumentsTypes);
            return gdalClassConstructor.newInstance(arguments);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
