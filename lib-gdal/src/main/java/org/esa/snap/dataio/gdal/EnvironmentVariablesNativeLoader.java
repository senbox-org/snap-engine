package org.esa.snap.dataio.gdal;

import org.esa.snap.engine_utilities.file.FileHelper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EnvironmentVariablesNativeLoader {

    private static final Logger logger = Logger.getLogger(GDALLoader.class.getName());

    private static boolean environmentVariablesNativeInitialisationExecuted = false;

    /**
     * Ensures EnvironmentVariables library was initialised
     */
    public static void ensureEnvironmentVariablesNativeInitialised() {
        if (!environmentVariablesNativeInitialisationExecuted) {
            try {
                initEnvironmentVariablesNativeLibrary();
                environmentVariablesNativeInitialisationExecuted = true;
            } catch (IOException e) {
                throw new IllegalStateException("EnvironmentVariablesNative NOT initialised! Check log for details.");
            }
        }
    }

    /**
     * Gets the environment variables native library URL from SNAP distribution packages for this version.
     *
     * @return the environment variables native library URL from SNAP distribution packages for this version
     */
    public static URL getEnvironmentVariablesFilePathFromSources() {
        final String evFileDirectoryFromSources = GDALVersion.GDAL_NATIVE_LIBRARIES_SRC + "/" + OSCategory.getDirectory() + "/" + OSCategory.getOSCategory().getOSSpecificEnvironmentVariablesFileName();
        try {
            return GDALVersion.class.getClassLoader().getResource(evFileDirectoryFromSources.replace(File.separator, "/"));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Gets the environment variables native library root directory path for install this version.
     *
     * @return the environment variables native library root directory path for install this version
     */
    public static Path getEnvironmentVariablesFilePath() {
        return GDALVersion.getNativeLibrariesRootFolderPath().resolve(OSCategory.getOSCategory().getOSSpecificEnvironmentVariablesFileName());
    }

    /**
     * Initializes EnvironmentVariables native library to be used by SNAP.
     */
    private static void initEnvironmentVariablesNativeLibrary() throws IOException {
        copyEnvironmentVariablesNativeLibrary();
        loadEnvironmentVariablesNativeLibrary();
    }

    /**
     * Copies the environment variables native library used for access OS environment variables.
     *
     * @throws IOException When IO error occurs
     */
    static void copyEnvironmentVariablesNativeLibrary() throws IOException {
        final Path evFilePath = getEnvironmentVariablesFilePath();
        if (!Files.exists(evFilePath)) {
            logger.log(Level.FINE, "Copy the environment variables library file.");

            final URL libraryFileURLFromSources = getEnvironmentVariablesFilePathFromSources();
            if (libraryFileURLFromSources != null) {
                logger.log(Level.FINE, "The environment variables library file path on the local disk is '" + evFilePath + "' and the library file name from sources is '" + libraryFileURLFromSources + "'.");

                FileHelper.copyFile(libraryFileURLFromSources, evFilePath);
            } else {
                throw new IllegalStateException("Unable to get environment variables libraryFileURLFromSources");
            }
        }
    }

    /**
     * Loads the environment variables native library used for access OS environment variables.
     */
    private static void loadEnvironmentVariablesNativeLibrary() {
        final Path evFilePath = getEnvironmentVariablesFilePath();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Load the native library '" + evFilePath.getFileName() + "'.");
        }
        System.load(evFilePath.toAbsolutePath().toString());
    }


}
