package org.esa.snap.dataio.gdal;

import java.io.IOException;

public class EnvironmentVariablesNativeLoader {

    private static boolean environmentVariablesNativeInitialisationExecuted = false;

    public static void ensureEnvironmentVariablesNativeInitialised() {
        if (!environmentVariablesNativeInitialisationExecuted) {
            try {
                GDALInstaller.setupEnvironmentVariablesNativeLibrary();
                environmentVariablesNativeInitialisationExecuted = true;
            } catch (IOException e) {
                throw new IllegalStateException("EnvironmentVariablesNative NOT initialised! Check log for details.");
            }
        }
    }

}
