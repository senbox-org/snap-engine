package org.esa.snap.dataio.gdal;

import org.apache.commons.lang3.SystemUtils;
import org.esa.snap.dataio.gdal.drivers.OSR;
import org.esa.snap.jni.EnvironmentVariables;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GDAL Distribution Installer class for installing GDAL on SNAP (internal distribution).
 *
 * @author Jean Coravu
 * @author Adrian Drăghici
 */
class GDALDistributionInstaller {
    private static final Logger logger = Logger.getLogger(GDALDistributionInstaller.class.getName());

    private GDALDistributionInstaller() {
    }

    /**
     * Installs the internal GDAL library distribution.
     *
     * @param gdalVersion the GDAL version to be installed
     * @throws IOException When IO error occurs
     */
    private static void installDistribution(GDALVersion gdalVersion) throws IOException {
        // install the GDAL library from the distribution
        final OSCategory osCategory = OSCategory.getOSCategory();
        if (osCategory.getArchitecture() == null) {
            final String msg = "No distribution folder found on " + osCategory.getOperatingSystemName() + ".";
            logger.log(Level.FINE, msg);
            throw new IllegalStateException(msg);
        }

        logger.log(Level.FINE, "Install the GDAL library from the distribution on " + osCategory.getOperatingSystemName() + ".");

        GDALInstaller.copyDistribution(gdalVersion);
        final Path gdalDistributionRootFolderPath = gdalVersion.getNativeLibrariesFolderPath();

        logger.log(Level.FINE, "The GDAL library has been copied on the local disk.");

        if (org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS) {
            logger.log(Level.FINE, "Process the GDAL library on Windows.");

            processInstalledWindowsDistribution(gdalDistributionRootFolderPath);
        } else if (org.apache.commons.lang3.SystemUtils.IS_OS_LINUX || org.apache.commons.lang3.SystemUtils.IS_OS_MAC_OSX) {
            final String currentFolderPath = EnvironmentVariables.getCurrentDirectory();
            GDALInstaller.fixUpPermissions(gdalDistributionRootFolderPath);
            try {
                logger.log(Level.FINE, "Process the GDAL library on Linux. The current folder is '" + currentFolderPath + "'.");
                processInstalledLinuxDistribution(gdalDistributionRootFolderPath);
            } finally {
                EnvironmentVariables.changeCurrentDirectory(currentFolderPath);
            }
        }

        logger.log(Level.FINE, "The GDAL library has been successfully installed.");
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
        final Path libFolderPath = gdalDistributionRootFolderPath.resolve("lib");

        final StringBuilder gdalEnv = new StringBuilder();
        gdalEnv.append("GDAL_DATA=").append(gdalDistributionRootFolderPath.resolve("share/gdal"));
        logger.log(Level.FINE, "Set the GDAL_DATA environment variable on Linux with '" + gdalEnv + "'.");
        EnvironmentVariables.setEnvironmentVariable(gdalEnv.toString());
        gdalEnv.setLength(0);
        gdalEnv.append("GDAL_PLUGINS=").append(libFolderPath.resolve("gdalplugins"));
        logger.log(Level.FINE, "Set the GDAL_PLUGINS environment variable on Linux with '" + gdalEnv + "'.");
        EnvironmentVariables.setEnvironmentVariable(gdalEnv.toString());
    }

    /**
     * Processes the Windows OS specific post-install steps.
     * - adds the absolute path of the internal GDAL distribution installation location to the 'java.library.path'
     * - updates the PATH environment variable with the absolute path of the internal GDAL distribution installation location, when needed
     * - adds GDAL_DATA, GDAL_PLUGINS and PROJ_LIB environment variables
     *
     * @param gdalDistributionRootFolderPath the absolute path to the internal GDAL distribution installation location
     */
    private static void processInstalledWindowsDistribution(Path gdalDistributionRootFolderPath) {
        StringBuilder gdalEnv = new StringBuilder();
        gdalEnv.append("PATH=").append(gdalDistributionRootFolderPath).append(File.pathSeparator).append(EnvironmentVariables.getEnvironmentVariable("PATH"));
        logger.log(Level.FINE, "Set the PATH environment variable on Windows with '" + gdalEnv + "'.");
        EnvironmentVariables.setEnvironmentVariable(gdalEnv.toString());

        gdalEnv.setLength(0);
        gdalEnv.append("GDAL_DATA=").append(gdalDistributionRootFolderPath.resolve("gdal-data"));
        logger.log(Level.FINE, "Set the GDAL_DATA environment variable on Windows with '" + gdalEnv + "'.");
        EnvironmentVariables.setEnvironmentVariable(gdalEnv.toString());

        gdalEnv.setLength(0);
        gdalEnv.append("GDAL_PLUGINS=").append(gdalDistributionRootFolderPath.resolve("gdalplugins"));
        logger.log(Level.FINE, "Set the GDAL_PLUGINS environment variable on Windows with '" + gdalEnv + "'.");
        EnvironmentVariables.setEnvironmentVariable(gdalEnv.toString());
    }

    /**
     * Setups the GDAL distribution for SNAP
     *
     * @param gdalVersion the GDAL version to be setup
     * @throws IOException When IO error occurs
     */
    static void setupDistribution(GDALVersion gdalVersion) throws IOException {
        installDistribution(gdalVersion);
    }

    /**
     * Setups the GDAL Proj Lib path to correctly load the proj.db from internal distribution
     *
     * @param gdalVersion the GDAL version to be setup
     */
    static void setupProj(GDALVersion gdalVersion) {
        final Path projPath = SystemUtils.IS_OS_LINUX
                ? gdalVersion.getNativeLibrariesFolderPath().resolve("share/share/proj")
                : gdalVersion.getNativeLibrariesFolderPath().resolve("projlib");
        OSR.setPROJSearchPath(projPath.toString());
    }

}
