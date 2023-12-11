package org.esa.snap.dataio.gdal;

import org.esa.snap.core.util.SystemUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GDAL Version enum for defining compatible GDAL versions with SNAP.
 *
 * @author Adrian Drăghici
 */
public enum GDALVersion {

    GDAL_372_FULL("3.7.2", "3-7-2", true, new String[]{"gdalalljni"});

    static final String VERSION_NAME = "{version}";
    static final String DIR_NAME = "gdal-" + VERSION_NAME;
    static final String ZIP_NAME = DIR_NAME + ".zip";
    static final String SHA256_NAME = DIR_NAME + ".sha256";
    public static final String GDAL_NATIVE_LIBRARIES_ROOT = "gdal";
    static final String GDAL_NATIVE_LIBRARIES_SRC = "auxdata/gdal";
    static final String GDAL_JNI_LIBRARY_FILE = "java/gdal.jar";
    private static final String GDAL_LOADER_LIBRARY_FILE = "NativeLibraryLoader.jar";

    private static final Logger logger = Logger.getLogger(GDALVersion.class.getName());

    private static GDALVersion INTERNAL_VERSION = null;

    String id;
    final String name;
    final String location;
    final boolean cogCapable;
    final String[] nativeLibraryNames;

    /**
     * Creates new instance for this enum.
     *
     * @param id   the id of version
     * @param name the name of version
     * @param cogCapable  the COG compatibility of version: {@code true} if version is COG compatible
     * @param nativeLibraryNames the array with native libraries names to be loaded
     */
    GDALVersion(String id, String name, boolean cogCapable, String[] nativeLibraryNames) {
        this.id = id;
        this.name = name;
        this.cogCapable = cogCapable;
        this.nativeLibraryNames = nativeLibraryNames;
        this.location = getNativeLibrariesFolderPath().toString();
    }

    /**
     * Gets the GDAL version.
     *
     * @return the GDAL version
     */
    public static GDALVersion getGDALVersion() {
        logger.log(Level.FINE, () -> "Internal GDAL " + getInternalVersion().getId() + " will be used by SNAP.");
        return getInternalVersion();
    }

    /**
     * Gets internal GDAL version.
     *
     * @return the internal GDAL version
     */
    public static GDALVersion getInternalVersion() {
        if (INTERNAL_VERSION == null) {
            INTERNAL_VERSION = GDAL_372_FULL;
        }
        return INTERNAL_VERSION;
    }

    /**
     * Gets the id of this version.
     *
     * @return the id of this version
     */
    public String getId() {
        return this.id;
    }

    /**
     * Sets the id for this version.
     *
     * @param id the new id
     */
    void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the location of this version.
     *
     * @return the location of this version
     */
    public String getLocation() {
        return this.location;
    }

    /**
     * Gets whether this version is COG capable (writes COG GeoTIFF).
     *
     * @return {@code true} if this version is COG capable
     */
    public boolean isCOGCapable() {
        return this.cogCapable;
    }

    /**
     * Gets the name of directory for this version.
     *
     * @return the name of directory for this version
     */
    private String getDirName() {
        return DIR_NAME.replace(VERSION_NAME, this.name);
    }

    /**
     * Gets the name of ZIP archive for this version.
     *
     * @return the name of ZIP archive for this version
     */
    private String getZipName() {
        return ZIP_NAME.replace(VERSION_NAME, this.name);
    }

    /**
     * Gets the name of SHA256 file for this version.
     *
     * @return the name of SHA256 file for this version
     */
    private String getSHA256Name() {
        return SHA256_NAME.replace(VERSION_NAME, this.name);
    }

    /**
     * Gets the ZIP archive URL from SNAP distribution packages for this version.
     *
     * @return the ZIP archive URL from SNAP distribution packages for this version
     */
    public URL getZipFileURLFromSources() {
        final String zipFileDirectoryFromSources = GDAL_NATIVE_LIBRARIES_SRC + "/" + OSCategory.getDirectory() + "/" + getZipName();
        try {
            logger.log(Level.FINE, "version zip archive URL from sources: '" + zipFileDirectoryFromSources + "'.");
            return getClass().getClassLoader().getResource(zipFileDirectoryFromSources.replace(File.separator, "/"));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Gets the SHA256 InputStream from SNAP distribution packages for the current version.
     *
     * @return the SHA256 InputStream from SNAP distribution packages
     */
    public InputStream getSHA256InputStreamFromSources() {
        final String sha256FileDirectoryFromSources = GDAL_NATIVE_LIBRARIES_SRC + "/" + OSCategory.getDirectory() + "/" + getSHA256Name();
        try {
            logger.log(Level.FINE, "version SHA256 file URL from sources: '" + sha256FileDirectoryFromSources + "'.");
            return getClass().getClassLoader().getResourceAsStream(sha256FileDirectoryFromSources.replace(File.separator, "/"));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Gets the ZIP archive root directory path for install this version.
     *
     * @return the ZIP archive root directory path for install this version
     */
    public Path getZipFilePath() {
        return getNativeLibrariesFolderPath().resolve(getZipName());
    }

    /**
     * Gets the directory path for install this version.
     *
     * @return the directory path for install this version
     */
    public Path getNativeLibrariesFolderPath() {
        return getNativeLibrariesRootFolderPath().resolve(getDirName());
    }

    /**
     * Gets the location path of this version.
     *
     * @return the location path of this version
     */
    Path getLocationPath() {
        return Paths.get(getLocation());
    }

    /**
     * Gets the root directory path for install this version.
     *
     * @return the root directory path for install this version
     */
    public static Path getNativeLibrariesRootFolderPath() {
        return SystemUtils.getAuxDataPath().resolve(GDAL_NATIVE_LIBRARIES_ROOT);
    }

    /**
     * Gets the path for JNI drivers of this version.
     *
     * @return the path for JNI drivers of this version
     */
    public Path getJNILibraryFilePath() {
        return getNativeLibrariesFolderPath().resolve(GDAL_JNI_LIBRARY_FILE);
    }

    /**
     * Gets the loader library URL from SNAP distribution packages for this version.
     *
     * @return the loader library URL from SNAP distribution packages for this version
     */
    public static URL getLoaderFilePathFromSources() {
        final String loaderFileDirectoryFromSources = GDAL_NATIVE_LIBRARIES_SRC + "/" + GDAL_LOADER_LIBRARY_FILE;
        try {
            return GDALVersion.class.getClassLoader().getResource(loaderFileDirectoryFromSources.replace(File.separator, "/"));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Gets the GDAL native library files path for install this version.
     *
     * @return the GDAL native library files path for install this version
     */
    public Path[] getGDALNativeLibraryFilesPath() {
        final Path[] gdalNativeLibaryFiles = new Path[this.nativeLibraryNames!=null?this.nativeLibraryNames.length:0];
        for (int i = 0; i < gdalNativeLibaryFiles.length; i++) {
            gdalNativeLibaryFiles[i] = getLocationPath();
            if (org.apache.commons.lang3.SystemUtils.IS_OS_UNIX) {
                gdalNativeLibaryFiles[i] = gdalNativeLibaryFiles[i].resolve("lib").resolve("jni");
            }
            gdalNativeLibaryFiles[i] = gdalNativeLibaryFiles[i].resolve(System.mapLibraryName(this.nativeLibraryNames[i]));
        }
        return gdalNativeLibaryFiles;
    }

    /**
     * Gets the path for Loader of this version.
     *
     * @return the path for Loader of this version
     */
    public static Path getLoaderLibraryFilePath() {
        return getNativeLibrariesRootFolderPath().resolve(GDAL_LOADER_LIBRARY_FILE);
    }
}

