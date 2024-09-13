package eu.esa.snap.core.lib;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * NativeLibraryClassLoader class for custom ClassLoader used to load native libraries without overriding 'java.library.path'
 */
public class NativeLibraryClassLoader extends URLClassLoader {

    private final Path[] nativeLibraryPaths;

    /**
     * Initialize the NativeLibraryClassLoader class.
     *
     * @param urls               the absolute URL paths to JAR files to be loaded using this custom ClassLoader
     * @param nativeLibraryPaths the absolute paths to native libraries to be loaded using this custom ClassLoader
     */
    public NativeLibraryClassLoader(URL[] urls, Path[] nativeLibraryPaths) {
        super(urls, NativeLibraryClassLoader.class.getClassLoader());
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
