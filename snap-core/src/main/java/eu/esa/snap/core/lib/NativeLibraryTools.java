package eu.esa.snap.core.lib;

import org.esa.snap.core.util.SystemUtils;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

public class NativeLibraryTools {

    private static final String NATIVE_LOADER_LIBRARY_JAR = "NativeLibraryLoader.jar";
    public static final String GDAL_NATIVE_LIBRARIES_ROOT = "gdal";

    public static void loadLibrary() {
        final Path loaderFilePath =  SystemUtils.getAuxDataPath().resolve(GDAL_NATIVE_LIBRARIES_ROOT);

       // final URL libraryFileURLFromSources
    }

}
