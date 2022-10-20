package org.esa.s2tbx.jni;

/**
 * Environment Variables JNI class for control OS environment variables during runtime
 *
 * @author Jean Coravu
 */
public class EnvironmentVariablesNative {

    static {
        //Loads the native library used by this JNI
        System.loadLibrary("environment-variables");
    }

    public static native int chdir(String dir);

    public static native String getcwd();

    public static native String getenv(String key);

    public static native int putenv(String keyEqualValue);

    private EnvironmentVariablesNative(){
        //noting to init
    }
}

/*
 1) javac org/esa/s2tbx/jni/EnvironmentVariablesNative.java

 2) javah org.esa.s2tbx.jni.EnvironmentVariablesNative

 3a)---- for Windows 32-bit ----
    gcc -m32 -Wl,--add-stdcall-alias -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" -shared -o environment-variables-win32.dll org_esa_s2tbx_jni_EnvironmentVariablesNative.c

    ---- for Windows 64-bit ----
    gcc -m64 -Wl,--add-stdcall-alias -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" -shared -o environment-variables-win64.dll org_esa_s2tbx_jni_EnvironmentVariablesNative.c

 3b) ---- for Linux ----
    gcc -fPIC -I{$JAVA_HOME}/include -I{$JAVA_HOME}/include/linux -shared -o environment-variables.so org_esa_s2tbx_jni_EnvironmentVariablesNative.c
*/
