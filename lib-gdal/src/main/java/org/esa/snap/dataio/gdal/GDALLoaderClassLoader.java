package org.esa.snap.dataio.gdal;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * GDALLoaderClassLoader class for custom ClassLoader used to load GDAL native library without overriding 'java.library.path'
 */
public final class GDALLoaderClassLoader extends URLClassLoader {

    private final Path[] nativeLibraryPaths;

    /**
     * Initialize the GDALLoaderClassLoader class.
     *
     * @param urls               the absolute URL paths to JAR files to be loaded using this custom ClassLoader
     * @param nativeLibraryPaths the absolute paths to native libraries to be loaded using this custom ClassLoader
     */
    public GDALLoaderClassLoader(URL[] urls, Path[] nativeLibraryPaths) {
        super(urls, GDALLoaderClassLoader.class.getClassLoader());
        this.nativeLibraryPaths = nativeLibraryPaths;
    }

    /**
     * Returns the absolute path for native library when exists in this custom ClassLoader
     *
     * @param libname The native library name
     * @return the absolute path for native library
     */
    protected String findLibrary(String libname) {
        for (Path nativeLibraryPath : nativeLibraryPaths) {
            if (nativeLibraryPath.getFileName().toString().equals(System.mapLibraryName(libname))) {
                return nativeLibraryPath.toAbsolutePath().toString();
            }
        }
        return null;
    }
}