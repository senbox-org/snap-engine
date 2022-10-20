package org.esa.s2tbx.dataio.gdal;

import org.esa.s2tbx.jni.EnvironmentVariables;
import org.esa.snap.core.util.NativeLibraryUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GDAL Distribution Installer class for installing GDAL on SNAP (internal distribution or JNI drivers).
 *
 * @author Jean Coravu
 * @author Adrian Drăghici
 */
 class GDALDistributionInstaller {
    private static final Logger logger = Logger.getLogger(GDALDistributionInstaller.class.getName());

    private GDALDistributionInstaller() {
    }

    /**
     * Installs the internal GDAL library distribution if missing from SNAP and not installed on OS.
     *
     * @param gdalVersion the GDAL version to be installed
     * @throws IOException When IO error occurs
     */
    private static void installDistribution(GDALVersion gdalVersion) throws IOException {
        // install the GDAL library from the distribution
        OSCategory osCategory = gdalVersion.getOsCategory();
        if (osCategory.getArchitecture() == null) {
            String msg = "No distribution folder found on " + osCategory.getOperatingSystemName() + ".";
            logger.log(Level.INFO, msg);
            throw new IllegalStateException(msg);
        }
        Path gdalNativeLibrariesRootFolderPath = gdalVersion.getNativeLibrariesRootFolderPath();

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Install the GDAL library from the distribution on " + osCategory.getOperatingSystemName() + ".");
        }

        GDALInstaller installer = new GDALInstaller();
        Path gdalDistributionRootFolderPath = installer.copyDistribution(gdalNativeLibrariesRootFolderPath, gdalVersion);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.INFO, "The GDAL library has been copied on the local disk.");
        }

        if (org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Process the GDAL library on Windows.");
            }

            processInstalledWindowsDistribution(gdalDistributionRootFolderPath);
        } else if (org.apache.commons.lang.SystemUtils.IS_OS_LINUX || org.apache.commons.lang.SystemUtils.IS_OS_MAC_OSX) {
            String currentFolderPath = EnvironmentVariables.getCurrentDirectory();
            GDALInstaller.fixUpPermissions(gdalDistributionRootFolderPath);
            try {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Process the GDAL library on Linux. The current folder is '" + currentFolderPath + "'.");
                }
                processInstalledLinuxDistribution(gdalDistributionRootFolderPath);
            } finally {
                EnvironmentVariables.changeCurrentDirectory(currentFolderPath);
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "The GDAL library has been successfully installed.");
        }
    }

    /**
     * Installs the GDAL JNI drivers if missing from SNAP and GDAL distribution installed on OS.
     *
     * @param gdalVersion the GDAL version to which JNI drivers be installed
     * @throws IOException When IO error occurs
     */
    private static void installJNI(GDALVersion gdalVersion) throws IOException {
        // install the GDAL JNI drivers from the distribution
        OSCategory osCategory = gdalVersion.getOsCategory();
        if (osCategory.getArchitecture() == null) {
            String msg = "No distribution folder found on " + osCategory.getOperatingSystemName() + ".";
            logger.log(Level.INFO, msg);
            throw new IllegalStateException(msg);
        }
        Path gdalNativeLibrariesRootFolderPath = gdalVersion.getNativeLibrariesRootFolderPath();

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Install the GDAL JNI drivers from the distribution on " + osCategory.getOperatingSystemName() + ".");
        }

        GDALInstaller installer = new GDALInstaller();
        Path gdalDistributionRootFolderPath = installer.copyDistribution(gdalNativeLibrariesRootFolderPath, gdalVersion);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.INFO, "The GDAL JNI drivers has been copied on the local disk.");
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Process the GDAL JNI drivers on " + gdalVersion.getOsCategory().getOperatingSystemName() + ".");
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Register native lib paths on " + gdalVersion.getOsCategory().getOperatingSystemName() + " for folder '" + gdalDistributionRootFolderPath.toString() + "'.");
        }
        NativeLibraryUtils.registerNativePaths(gdalDistributionRootFolderPath);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "The GDAL library has been successfully installed.");
        }
    }

    /**
     * Processes the UNIX OS specific post-install steps.
     * - adds the absolute path of the internal GDAL distribution installation location to the 'java.library.path'
     * - updates the PATH environment variable with the absolute path of the internal GDAL distribution installation location, when needed
     * - adds GDAL_DATA, GDAL_PLUGINS and PROJ_LIB environment variables
     *
     * @param gdalDistributionRootFolderPath the absolute path to the internal GDAL distribution installation location
     */
    private static void processInstalledLinuxDistribution(Path gdalDistributionRootFolderPath) {
        Path libFolderPath = gdalDistributionRootFolderPath.resolve("lib");

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Register native lib paths on Linux for folder '" + libFolderPath.toString() + "'.");
        }
        NativeLibraryUtils.registerNativePaths(libFolderPath.resolve("jni"));

        Path gdalDataFolderPath = gdalDistributionRootFolderPath.resolve("share/gdal");
        StringBuilder gdalDataValue = new StringBuilder();
        gdalDataValue.append("GDAL_DATA")
                .append("=")
                .append(gdalDataFolderPath.toString());
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Set the GDAL_DATA environment variable on Linux with '" + gdalDataValue.toString() + "'.");
        }
        EnvironmentVariables.setEnvironmentVariable(gdalDataValue.toString());
        Path gdalPluginsFolderPath = libFolderPath.resolve("gdalplugins");
        StringBuilder gdalPluginsValue = new StringBuilder();
        gdalPluginsValue.append("GDAL_PLUGINS")
                .append("=")
                .append(gdalPluginsFolderPath.toString());
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Set the GDAL_DATA environment variable on Linux with '" + gdalPluginsValue.toString() + "'.");
        }
        EnvironmentVariables.setEnvironmentVariable(gdalPluginsValue.toString());
        Path projDataFolderPath = gdalDistributionRootFolderPath.resolve("share/proj");
        StringBuilder projDataValue = new StringBuilder();
        projDataValue.append("PROJ_LIB")
                .append("=")
                .append(projDataFolderPath.toString());
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Set the PROJ_LIB environment variable on MacOSX with '" + projDataValue.toString() + "'.");
        }
        EnvironmentVariables.setEnvironmentVariable(projDataValue.toString());
    }

    /**
     * Processes the Windows OS specific post-install steps.
     * - adds the absolute path of the internal GDAL distribution installation location to the 'java.library.path'
     * - updates the PATH environment variable with the absolute path of the internal GDAL distribution installation location, when needed
     * - adds GDAL_DATA, GDAL_PLUGINS and PROJ_LIB environment variables
     *
     * @param gdalDistributionRootFolderPath the absolute path to the internal GDAL distribution installation location
     * @throws IOException When IO error occurs
     */
    private static void processInstalledWindowsDistribution(Path gdalDistributionRootFolderPath) throws IOException {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Register native lib paths on Windows for folder '" + gdalDistributionRootFolderPath.toString() + "'.");
        }
        NativeLibraryUtils.registerNativePaths(gdalDistributionRootFolderPath);

        String pathEnvironment = EnvironmentVariables.getEnvironmentVariable("PATH");
        boolean foundBinFolderInPath = findFolderInPathEnvironment(gdalDistributionRootFolderPath, pathEnvironment);
        if (!foundBinFolderInPath) {
            StringBuilder newPathValue = new StringBuilder();
            newPathValue.append("PATH")
                    .append("=")
                    .append(gdalDistributionRootFolderPath.toString())
                    .append(File.pathSeparator)
                    .append(pathEnvironment);
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Set the PATH environment variable on Windows with '" + newPathValue.toString() + "'.");
            }
            EnvironmentVariables.setEnvironmentVariable(newPathValue.toString());
        }

        Path gdalDataFolderPath = gdalDistributionRootFolderPath.resolve("gdal-data");
        StringBuilder gdalDataValue = new StringBuilder();
        gdalDataValue.append("GDAL_DATA")
                .append("=")
                .append(gdalDataFolderPath.toString());
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Set the GDAL_DATA environment variable on Windows with '" + gdalDataValue.toString() + "'.");
        }
        EnvironmentVariables.setEnvironmentVariable(gdalDataValue.toString());
        Path gdalPluginsFolderPath = gdalDistributionRootFolderPath.resolve("gdalplugins");
        StringBuilder gdalPluginsValue = new StringBuilder();
        gdalPluginsValue.append("GDAL_PLUGINS")
                .append("=")
                .append(gdalPluginsFolderPath.toString());
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Set the GDAL_DATA environment variable on Windows with '" + gdalPluginsValue.toString() + "'.");
        }
        EnvironmentVariables.setEnvironmentVariable(gdalPluginsValue.toString());
        Path projDataFolderPath = gdalDistributionRootFolderPath.resolve("projlib");
        StringBuilder projDataValue = new StringBuilder();
        projDataValue.append("PROJ_LIB")
                .append("=")
                .append(projDataFolderPath.toString());
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Set the PROJ_LIB environment variable on Windows with '" + projDataValue.toString() + "'.");
        }
        EnvironmentVariables.setEnvironmentVariable(projDataValue.toString());
    }

    /**
     * Checks whether or not an directory path exists on the environment variable value.
     *
     * @param folderPathToCheck the directory path to be checked
     * @param pathEnvironment  the environment variable value
     * @return {@code true} if directory path found on environment variable value
     * @throws IOException When IO error occurs
     */
    private static boolean findFolderInPathEnvironment(Path folderPathToCheck, String pathEnvironment) throws IOException {
        String fullFolderPath = folderPathToCheck.toFile().getCanonicalPath();
        boolean foundFolderInPath = false;
        StringTokenizer str = new StringTokenizer(pathEnvironment, File.pathSeparator);
        while (str.hasMoreTokens() && !foundFolderInPath) {
            String currentFolderPathAsString = str.nextToken();
            Path currentFolderPath = Paths.get(currentFolderPathAsString);
            String currentFullFolderPath = currentFolderPath.toFile().getCanonicalPath();
            if (currentFullFolderPath.equals(fullFolderPath)) {
                foundFolderInPath = true;
            }
        }
        return foundFolderInPath;
    }

    /**
     * Setups the GDAL distribution for SNAP
     *
     * @param gdalVersion the GDAL version to be setup
     * @throws IOException When IO error occurs
     */
    static void setupDistribution(GDALVersion gdalVersion) throws IOException {
        if (gdalVersion.isJni()) {
            installJNI(gdalVersion);
        } else {
            installDistribution(gdalVersion);
        }
    }
}
