package eu.esa.snap.core.lib;

import org.esa.snap.core.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NativeLibraryTools {

    public static final String NATIVE_LOADER_LIBRARY_JAR = "eu.esa.snap.core.lib.NativeLibraryLoader";
    private static final String LOADER_LIBRARY_FILE = "NativeLibraryLoader.jar";

    public static final String GDAL_NATIVE_LIBRARIES_ROOT = "gdal";
    public static final String NETCDF_NATIVE_LIBRARIES_ROOT = "netcdf_natives";

    static final String GDAL_NATIVE_LIBRARIES_SRC = "auxdata/gdal";

    private static final Logger logger = Logger.getLogger(NativeLibraryTools.class.getName());


    /**
     * Copies the loader library used for load native library.
     *
     * @throws IOException When IO error occurs
     */
    public static void copyLoaderLibrary(String libraryRoot) throws IOException {
        final Path loaderFilePath = getLoaderLibraryFilePath(libraryRoot);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Copy the loader library file.");
        }

        final URL libraryFileURLFromSources = getLoaderFilePathFromSources();
        if (libraryFileURLFromSources != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("The loader library file path on the local disk is '" + loaderFilePath + "' and the library file name from sources is '" + libraryFileURLFromSources + "'.");
            }

            FileHelper.copyFile(libraryFileURLFromSources, loaderFilePath);
        } else {
            throw new IllegalStateException("Unable to get loader libraryFileURLFromSources");
        }
    }

    /**
     * Gets the path for Loader of this version.
     *
     * @return the path for Loader of this version
     */
    public static Path getLoaderLibraryFilePath(String libraryRoot) {
        return getNativeLibrariesRootFolderPath(libraryRoot).resolve(LOADER_LIBRARY_FILE);
    }

    public static Path getNativeLibrariesRootFolderPath(String libraryRoot) {
        return SystemUtils.getAuxDataPath().resolve(libraryRoot);
    }

    public static URL getLoaderFilePathFromSources() {
        final String loaderFileDirectoryFromSources = "auxdata/" + LOADER_LIBRARY_FILE;
        try {
            return NativeLibraryTools.class.getClassLoader().getResource(loaderFileDirectoryFromSources.replace(File.separator, "/"));
        } catch (Exception ignored) {
            return null;
        }
    }
}
