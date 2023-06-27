package org.esa.snap.jni;

import org.esa.snap.dataio.gdal.EnvironmentVariablesNativeLoader;

/**
 * Environment Variables class for control OS environment variables during runtime
 *
 * @author Jean Coravu
 */
public class EnvironmentVariables {

    /**
     * Changes current directory to specified directory path
     *
     * @param dir the target directory path
     */
    public static void changeCurrentDirectory(String dir) {
        EnvironmentVariablesNativeLoader.ensureEnvironmentVariablesNativeInitialised();
        final int result = EnvironmentVariablesNative.chdir(dir);
        if (result != 0) {
            throw new IllegalStateException("Unable to set change the current directory: " + result);
        }
    }

    /**
     * Gets the current directory
     *
     * @return the current directory
     */
    public static String getCurrentDirectory() {
        EnvironmentVariablesNativeLoader.ensureEnvironmentVariablesNativeInitialised();
        return EnvironmentVariablesNative.getcwd();
    }

    /**
     * Gets the value of OS environment variable
     *
     * @param key the OS environment variable key
     * @return the value of OS environment variable
     */
    public static String getEnvironmentVariable(String key) {
        EnvironmentVariablesNativeLoader.ensureEnvironmentVariablesNativeInitialised();
        return EnvironmentVariablesNative.getenv(key);
    }

    /**
     * Sets the OS environment variable
     *
     * @param keyEqualValue the key=value environment variable
     */
    public static void setEnvironmentVariable(String keyEqualValue) {
        EnvironmentVariablesNativeLoader.ensureEnvironmentVariablesNativeInitialised();
        final int result = EnvironmentVariablesNative.putenv(keyEqualValue);
        if (result != 0) {
            throw new IllegalStateException("Unable to set environment variable: " + result);
        }
    }

    private EnvironmentVariables(){
        //noting to init
    }

}
