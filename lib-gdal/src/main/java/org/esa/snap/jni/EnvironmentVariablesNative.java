package org.esa.snap.jni;

/**
 * Environment Variables JNI class for control OS environment variables during runtime
 *
 * @author Jean Coravu
 */
public class EnvironmentVariablesNative {

    public static native int chdir(String dir);

    public static native String getcwd();

    public static native String getenv(String key);

    public static native int putenv(String keyEqualValue);

    private EnvironmentVariablesNative(){
        //noting to init
    }
}

/*
 1) javac org/esa/snap/jni/EnvironmentVariablesNative.java

 2) javah org.esa.snap.jni.EnvironmentVariablesNative

 3a)---- for Windows 64-bit ----
    gcc -m64 -Wl,--add-stdcall-alias -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" -shared -o environment-variables-win64.dll org_esa_snap_jni_EnvironmentVariablesNative.c

 3b) ---- for Linux ----
    gcc -fPIC -I{$JAVA_HOME}/include -I{$JAVA_HOME}/include/linux -shared -o environment-variables.so org_esa_snap_jni_EnvironmentVariablesNative.c
 3c) ---- for Mac ----
    gcc -I$JAVA_HOME/include -I$JAVA_HOME/include/darwin -shared -o environment-variables.dylib org_esa_snap_jni_EnvironmentVariablesNative.c
*/
