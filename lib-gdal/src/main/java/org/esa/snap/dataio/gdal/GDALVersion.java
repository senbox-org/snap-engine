package org.esa.snap.dataio.gdal;

import org.esa.snap.core.util.SystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GDAL Version enum for defining compatible GDAL versions with SNAP.
 *
 * @author Adrian Drăghici
 */
public enum GDALVersion {

    GDAL_321_FULL("3.2.1", "3-2-1", true, new String[]{"gdalalljni"}, false),
    GDAL_32X_JNI("3.2.x", "3-2-X", true, new String[]{"gdalalljni"}, true),
    GDAL_31X_JNI("3.1.x", "3-1-X", true, new String[]{"gdalalljni"}, true),
    GDAL_30X_JNI("3.0.x", "3-0-X", false, new String[]{"gdalalljni"}, true),
    GDAL_24X_JNI("2.4.x", "2-4-X", false, new String[]{"gdalalljni"}, true),
    GDAL_23X_JNI("2.3.x", "2-3-X", false, new String[]{"gdalalljni"}, true),
    GDAL_22X_JNI("2.2.x", "2-2-X", false, new String[]{"libgdalconstjni.dylib", "libgdaljni.dylib", "libgnmjni.dylib", "libogrjni.dylib", "libosrjni.dylib"}, true),
    GDAL_21X_JNI("2.1.x", "2-1-X", false, new String[]{"libgdalconstjni.dylib", "libgdaljni.dylib", "libgnmjni.dylib", "libogrjni.dylib", "libosrjni.dylib"}, true),
    GDAL_20X_JNI("2.0.x", "2-0-X", false, new String[]{"libgdalconstjni.dylib", "libgdaljni.dylib", "libgnmjni.dylib", "libogrjni.dylib", "libosrjni.dylib"}, true);

    static final String VERSION_NAME = "{version}";
    static final String JNI_NAME = "{jni}";
    static final String DIR_NAME = "gdal-" + VERSION_NAME + JNI_NAME;
    static final String ZIP_NAME = DIR_NAME + ".zip";
    static final String SHA256_NAME = DIR_NAME + ".sha256";
    public static final String GDAL_NATIVE_LIBRARIES_ROOT = "gdal";
    static final String GDAL_NATIVE_LIBRARIES_SRC = "auxdata/gdal";
    static final String GDAL_JNI_LIBRARY_FILE = "java/gdal.jar";
    private static final String GDAL_LOADER_LIBRARY_FILE = "NativeLibraryLoader.jar";

    static final String GDALINFIO_EXECUTABLE_NAME = "gdalinfo";
    static final String GDALINFO_EXECUTABLE_ARGS = "--version";
    static final Map<String, GDALVersion> JNI_VERSIONS = buildJNIVersionsMap();

    private static final Logger logger = Logger.getLogger(GDALVersion.class.getName());

    private static GDALVersion INTERNAL_VERSION = null;
    private static Map<String, GDALVersion> INSTALLED_VERSIONS = null;
    private static GDALVersion INSTALLED_VERSION = null;

    private static final int TIMEOUT_FOR_PROCESS = 30;// allow only 30 seconds of running time for the process

    String id;
    final String name;
    final String location;
    final boolean cogCapable;
    final String[] nativeLibraryNames;
    final boolean isJNI;

    /**
     * Creates new instance for this enum.
     *
     * @param id   the id of version
     * @param name the name of version
     * @param cogCapable  the COG compatibility of version: {@code true} if version is COG compatible
     * @param nativeLibraryNames the array with native libraries names to be loaded
     */
    GDALVersion(String id, String name, boolean cogCapable, String[] nativeLibraryNames, boolean isJNI) {
        this.id = id;
        this.name = name;
        this.cogCapable = cogCapable;
        this.nativeLibraryNames = nativeLibraryNames;
        this.isJNI = isJNI;
        this.location = getNativeLibrariesFolderPath().toString();
    }

    /**
     * Creates the Map with JNI GDAL versions.
     *
     * @return the Map with JNI GDAL versions.
     */
    private static Map<String, GDALVersion> buildJNIVersionsMap() {
        final Map<String, GDALVersion> jniVersions = new HashMap<>(8);
        jniVersions.put(GDAL_20X_JNI.id, GDAL_20X_JNI);
        jniVersions.put(GDAL_21X_JNI.id, GDAL_21X_JNI);
        jniVersions.put(GDAL_22X_JNI.id, GDAL_22X_JNI);
        jniVersions.put(GDAL_23X_JNI.id, GDAL_23X_JNI);
        jniVersions.put(GDAL_24X_JNI.id, GDAL_24X_JNI);
        jniVersions.put(GDAL_30X_JNI.id, GDAL_30X_JNI);
        jniVersions.put(GDAL_31X_JNI.id, GDAL_31X_JNI);
        jniVersions.put(GDAL_32X_JNI.id, GDAL_32X_JNI);
        return Collections.unmodifiableMap(jniVersions);
    }

    /**
     * Gets the installed GDAL version when found or internal GDAL version otherwise.
     *
     * @return the installed GDAL version when found or internal GDAL version otherwise
     */
    public static GDALVersion getGDALVersion() {
        if (INSTALLED_VERSION == null) {
            INSTALLED_VERSION = getSelectedInstalledVersion();
        }
        if (GDALLoaderConfig.getInstance().useInstalledGDALLibrary() && INSTALLED_VERSION != null) {
            logger.log(Level.FINE, () -> "Installed GDAL " + INSTALLED_VERSION.getId() + " set to be used by SNAP.");
            return INSTALLED_VERSION;
        }
        logger.log(Level.FINE, () -> "Internal GDAL " + getInternalVersion().getId() + " set to be used by SNAP.");
        return getInternalVersion();
    }

    /**
     * Gets installed GDAL version.
     *
     * @return the installed GDAL version or {@code null} if not found
     */
    public static GDALVersion getSelectedInstalledVersion() {
        if (getInstalledVersions() != null) {
            final GDALVersion selectedInstalledVersion = INSTALLED_VERSIONS.get(GDALLoaderConfig.getInstance().getSelectedInstalledGDALLibrary());
            if (selectedInstalledVersion == null && INSTALLED_VERSIONS.size() > 0) {
                return INSTALLED_VERSIONS.values().iterator().next();
            }
            return selectedInstalledVersion;
        }
        return null;
    }

    /**
     * Gets installed GDAL version.
     *
     * @return the installed GDAL version or {@code null} if not found
     */
    public static Map<String, GDALVersion> getInstalledVersions() {
        if (INSTALLED_VERSIONS == null) {
            INSTALLED_VERSIONS = retrieveInstalledVersions();
        }
        return INSTALLED_VERSIONS;
    }

    static String fetchProcessOutput(Process process) throws IOException {
        final StringBuilder output = new StringBuilder();
        Thread.yield(); // yield the control to other threads for ensure that the process has started
        try (final BufferedReader commandBufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            boolean done = false;
            final long startTime = System.currentTimeMillis();
            long endTime;
            long elapsedTime = 0;
            while (!done && elapsedTime <= TIMEOUT_FOR_PROCESS) {
                Thread.yield(); // yield the control to other threads
                while (commandBufferedReader.ready()) {
                    final String line = commandBufferedReader.readLine();
                    if (line != null && !line.isEmpty()) {
                        output.append(line).append("\n");
                    } else {
                        break;
                    }
                    done = true;
                }
                endTime = System.currentTimeMillis();
                elapsedTime = (endTime - startTime) / 1000;
            }
            return output.toString();
        }
    }

    /**
     * Checks whether the internal GDAL distribution is detected as installed version
     * @param installedVersionPath the path of the detected installed version
     * @return {@code true} if the internal GDAL distribution is detected as installed version
     */
    private static boolean isInternalVersionDetectedAsInstalledVersion(String installedVersionPath){
        if (installedVersionPath.equals(getInternalVersion().getLocation())) {
            logger.log(Level.FINE, () -> "Skipping detected internal GDAL " + getInternalVersion().id + " from distribution.");
            return true;
        }
        return false;
    }

    /**
     * Retrieves the installed GDAl versions on host OS by invoking 'gdalinfo --version' on every 'gdalinfo' executable path command and parsing the output.
     *
     * @return the installed GDAl versions on host OS or {@code null} if not found
     */
    private static Map<String, GDALVersion> retrieveInstalledVersions() {
        final OSCategory osCategory = OSCategory.getOSCategory();
        final String[] installedVersionsPaths = osCategory.getExecutableLocations(GDALINFIO_EXECUTABLE_NAME);
        if (installedVersionsPaths.length < 1) {
            logger.log(Level.FINE, () -> "GDAL not found on system. Internal GDAL " + getInternalVersion().id + " from distribution will be used.");
            return null;
        }
        final Map<String, GDALVersion> gdalVersions = new LinkedHashMap<>();
        for (final String installedVersionsPath : installedVersionsPaths) {
            if (isInternalVersionDetectedAsInstalledVersion(installedVersionsPath)) {
                continue;
            }
            try {
                final String result = fetchProcessOutput(Runtime.getRuntime().exec(new String[]{installedVersionsPath + File.separator + GDALINFIO_EXECUTABLE_NAME, GDALINFO_EXECUTABLE_ARGS}));
                final String versionId = result.replaceAll("[\\s\\S]*?(\\d*\\.\\d*\\.\\d*)[\\s\\S]*$", "$1");
                final String version = versionId.replaceAll("(\\d*\\.\\d*)[\\s\\S]*$", "$1.x");
                final GDALVersion gdalVersion = JNI_VERSIONS.get(version);
                if (gdalVersion != null) {
                    gdalVersion.setId(versionId);
                    logger.log(Level.FINE, () -> "GDAL " + versionId + " found on system. JNI driver will be used.");
                    gdalVersions.putIfAbsent(version, gdalVersion);
                } else {
                    logger.log(Level.FINE, () -> "Incompatible GDAL " + versionId + " found on system. Internal GDAL " + getInternalVersion().id + " from distribution will be used.");
                }
            } catch (IOException ex) {
                logger.log(Level.WARNING, () -> "Error occurred while checking installed GDAL version(s): " + ex.getMessage());
            }
        }
        return gdalVersions;
    }

    /**
     * Gets internal GDAL version.
     *
     * @return the internal GDAL version
     */
    public static GDALVersion getInternalVersion() {
        if (INTERNAL_VERSION == null) {
            INTERNAL_VERSION = GDAL_321_FULL;
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
     * Gets whether this version is JNI driver.
     *
     * @return {@code true} if this version is JNI driver
     */
    public boolean isJni() {
        return this.isJNI;
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
        if (isJni()) {
            return DIR_NAME.replace(VERSION_NAME, this.name).replace(JNI_NAME, "-jni");
        } else {
            return DIR_NAME.replace(VERSION_NAME, this.name).replace(JNI_NAME, "");
        }
    }

    /**
     * Gets the name of ZIP archive for this version.
     *
     * @return the name of ZIP archive for this version
     */
    private String getZipName() {
        if (isJni()) {
            return ZIP_NAME.replace(VERSION_NAME, this.name).replace(JNI_NAME, "-jni");
        } else {
            return ZIP_NAME.replace(VERSION_NAME, this.name).replace(JNI_NAME, "");
        }
    }

    /**
     * Gets the name of SHA256 file for this version.
     *
     * @return the name of SHA256 file for this version
     */
    private String getSHA256Name() {
        if (isJni()) {
            return SHA256_NAME.replace(VERSION_NAME, this.name).replace(JNI_NAME, "-jni");
        } else {
            return SHA256_NAME.replace(VERSION_NAME, this.name).replace(JNI_NAME, "");
        }
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

