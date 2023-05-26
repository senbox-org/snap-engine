/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.dataio.gdal;

import org.esa.snap.core.util.NativeLibraryUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.file.FileHelper;
import org.esa.snap.runtime.Config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import static org.apache.commons.lang.SystemUtils.IS_OS_UNIX;
import static org.esa.snap.dataio.gdal.GDALLoaderConfig.CONFIG_NAME;

/**
 * GDAL installer class for deploying GDAL binaries to the aux data dir.
 *
 * @author Cosmin Cara
 * @author Adrian Drăghici
 */
class GDALInstaller {

    private static final String PREFERENCE_KEY_INSTALLER_VERSION = "gdal.installer";
    private static final String PREFERENCE_KEY_DISTRIBUTION_HASH = "gdal.distribution.hash";
    private static final Logger logger = Logger.getLogger(GDALInstaller.class.getName());

    private final Path gdalNativeLibrariesFolderPath;

    GDALInstaller(Path gdalNativeLibrariesFolderPath) {
        this.gdalNativeLibrariesFolderPath = gdalNativeLibrariesFolderPath;
    }

    /**
     * Fixes the permissions issue with executables on UNIX OS.
     *
     * @param destPath the target directory/executable file path
     * @throws IOException When IO error occurs
     */
    static void fixUpPermissions(Path destPath) throws IOException {
        try (final Stream<Path> files = Files.list(destPath)) {
            files.forEach(path -> {
                if (Files.isDirectory(path)) {
                    try {
                        fixUpPermissions(path);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "GDAL configuration error: failed to fix permissions on " + path, e);
                    }
                } else {
                    setExecutablePermissions(path);
                }
            });
        }
    }

    /**
     * Sets required permissions for executables on UNIX OS.
     *
     * @param executablePathName the target executable file path
     */
    private static void setExecutablePermissions(Path executablePathName) {
        if (IS_OS_UNIX) {
            try {
                Files.setPosixFilePermissions(executablePathName, new HashSet<>(Arrays.asList(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ,
                        PosixFilePermission.OTHERS_EXECUTE)));
            } catch (IOException e) {
                // can't set the permissions for this file, eg. the file was installed as root
                // send a warning message, user will have to do that by hand.
                logger.log(Level.SEVERE, "Can't set execution permissions for executable " + executablePathName + ". If required, please ask an authorised user to make the file executable.", e);
            }
        }
    }

    /**
     * Compares module version strings.
     *
     * @param currentModuleVersion the current module version string
     * @param savedModuleVersion   the saved module version string
     * @return comparision result index where:
     * >0 when current module version greater than saved module version
     * 0 when current module version same as saved module version
     * <0 when current module version lower than saved module version
     */
    private static int compareVersions(String currentModuleVersion, String savedModuleVersion) {
        final int[] moduleVersionFragments = parseVersion(currentModuleVersion);
        final int[] savedVersionFragments = parseVersion(savedModuleVersion);

        final int max = Math.max(moduleVersionFragments.length, savedVersionFragments.length);
        for (int i = 0; i < max; ++i) {
            final int d1 = (i < moduleVersionFragments.length) ? moduleVersionFragments[i] : 0;
            final int d2 = (i < savedVersionFragments.length) ? savedVersionFragments[i] : 0;
            if (d1 != d2) {
                return d1 - d2;
            }
        }
        return 0;
    }

    /**
     * Parses the module version string
     *
     * @param version the module version string
     * @return parsed module version array
     */
    private static int[] parseVersion(String version) {
        if (version.toLowerCase().contentEquals("unknown")) {
            return new int[]{};
        }
        final StringTokenizer tok = new StringTokenizer(version, ".", true);
        final int len = tok.countTokens();
        if (len % 2 == 0) {
            throw new NumberFormatException("Even number of pieces in a spec version: `" + version + "'");
        }
        final int[] digits = new int[len / 2 + 1];
        int index = 0;
        boolean expectingNumber = true;
        while (tok.hasMoreTokens()) {
            final String fragment = tok.nextToken();
            if (expectingNumber) {
                expectingNumber = false;
                final int piece = Integer.parseInt(fragment);
                if (piece < 0) {
                    throw new NumberFormatException("Spec version component '" + piece + "' is negative.");
                }
                digits[index++] = piece;
            } else {
                if (!".".equals(fragment)) {
                    throw new NumberFormatException("Expected dot in version '" + version + "'.");
                }
                expectingNumber = true;
            }
        }

        return digits;
    }

    /**
     * Fetches the saved module specification version from SNAP config.
     *
     * @return the saved module specification version
     */
    private static String fetchSavedModuleSpecificationVersion() {
        final Config config = Config.instance(CONFIG_NAME);
        config.load();
        return config.preferences().get(PREFERENCE_KEY_INSTALLER_VERSION, null);
    }

    private static boolean isModuleUpdated(String moduleVersion){
        final String savedVersion = fetchSavedModuleSpecificationVersion();

        logger.log(Level.FINE, "The saved GDAL distribution folder version is '" + savedVersion + "'.");

        return StringUtils.isNullOrEmpty(savedVersion) || compareVersions(savedVersion, moduleVersion) != 0;
    }

    private static boolean isDistributionInstalled(Path gdalNativeLibrariesFolderPath, GDALVersion gdalVersion){
        final Path gdalDistributionRootFolderPath = gdalVersion.getNativeLibrariesFolderPath();
        if (Files.exists(gdalNativeLibrariesFolderPath)) {
            boolean isDistributionRootFolderEmpty = true;
            try (final Stream<Path> distributionRootFolderContents = Files.list(gdalDistributionRootFolderPath)) {
                isDistributionRootFolderEmpty = !distributionRootFolderContents.findAny().isPresent();
            } catch (Exception ignored) {
                //nothing to do
            }
            return Files.exists(gdalDistributionRootFolderPath) && !isDistributionRootFolderEmpty && Files.exists(gdalVersion.getEnvironmentVariablesFilePath());
        }
        return false;
    }

    /**
     * Fetches the saved distribution hash from SNAP config.
     *
     * @return the saved distribution hash
     */
    private static String fetchSavedDistributionHash() {
        final Config config = Config.instance(CONFIG_NAME);
        config.load();
        return config.preferences().get(PREFERENCE_KEY_DISTRIBUTION_HASH, null);
    }

    private static String fetchDistributionDirectoryHash(GDALVersion gdalVersion) throws Exception {
        final URL sha256FileURLFromSources = gdalVersion.getSHA256FileURLFromSources();
        if (sha256FileURLFromSources != null) {
            try (Stream<String> lines = Files.lines(Paths.get(sha256FileURLFromSources.toURI()))) {
                return lines.findFirst().orElse(null);
            }
        }
        return null;
    }

    private static boolean isDistributionUpdated(String distributionHash){
        final String savedDistributionHash = fetchSavedDistributionHash();
        logger.log(Level.FINE, "The saved GDAL distribution hash is '" + savedDistributionHash + "'.");
        return !distributionHash.equals(savedDistributionHash);
    }

    /**
     * Sets the saved module specification version to SNAP config.
     */
    private static void setSavedModuleSpecificationVersion(String newModuleSpecificationVersion) {
        final Config config = Config.instance(CONFIG_NAME);
        config.load();
        final Preferences preferences = config.preferences();
        preferences.put(PREFERENCE_KEY_INSTALLER_VERSION, newModuleSpecificationVersion);
        try {
            preferences.flush();
        } catch (BackingStoreException exception) {
            // ignore exception
        }
    }

    /**
     * Sets the saved distribution hash to SNAP config.
     */
    private static void setSavedDistributionHash(String newDistributionHash) {
        final Config config = Config.instance(CONFIG_NAME);
        config.load();
        final Preferences preferences = config.preferences();
        preferences.put(PREFERENCE_KEY_DISTRIBUTION_HASH, newDistributionHash);
        try {
            preferences.flush();
        } catch (BackingStoreException exception) {
            // ignore exception
        }
    }

    /**
     * Registers the environment variables native library used for access OS environment variables.
     *
     * @param gdalVersion the GDAL version to which JNI environment variables native library be installed
     */
    private static void registerEnvironmentVariablesNativeLibrary(GDALVersion gdalVersion) {
        final Path evFilePath = gdalVersion.getEnvironmentVariablesFilePath();
        logger.log(Level.FINE, "Register the native paths for folder '" + evFilePath.getParent() + "'.");
        NativeLibraryUtils.registerNativePaths(evFilePath.getParent());
    }

    /**
     * Copies the environment variables native library used for access OS environment variables.
     *
     * @param gdalVersion the GDAL version to which JNI environment variables native library be installed
     * @throws IOException When IO error occurs
     */
    private static void copyEnvironmentVariablesNativeLibrary(GDALVersion gdalVersion) throws IOException {
        final Path evFilePath = gdalVersion.getEnvironmentVariablesFilePath();
        if (!Files.exists(evFilePath)) {
            logger.log(Level.FINE, "Copy the environment variables library file.");

            final URL libraryFileURLFromSources = gdalVersion.getEnvironmentVariablesFilePathFromSources();
            if (libraryFileURLFromSources != null) {
                logger.log(Level.FINE, "The environment variables library file path on the local disk is '" + evFilePath + "' and the library file name from sources is '" + libraryFileURLFromSources + "'.");

                FileHelper.copyFile(libraryFileURLFromSources, evFilePath);
            } else {
                throw new IllegalStateException("Unable to get environment variables libraryFileURLFromSources");
            }
        }
    }

    private static void copyDistributionArchive(Path gdalDistributionRootFolderPath, GDALVersion gdalVersion) throws IOException {
        if (!Files.exists(gdalDistributionRootFolderPath)) {
            logger.log(Level.FINE, "create the distribution root folder '" + gdalDistributionRootFolderPath + "'.");
            Files.createDirectories(gdalDistributionRootFolderPath);
        }
        logger.log(Level.FINE, "The distribution root folder '" + gdalDistributionRootFolderPath + "' exists on the local disk.");
        final Path zipFilePath = gdalVersion.getZipFilePath();
        logger.log(Level.FINE, "Copy the zip archive to folder '" + zipFilePath + "'.");
        final URL zipFileURLFromSources = gdalVersion.getZipFileURLFromSources();
        if (zipFileURLFromSources == null) {
            throw new ExceptionInInitializerError("No GDAL distribution drivers provided for this OS.");
        }
        FileHelper.copyFile(zipFileURLFromSources, zipFilePath);
    }

    private static void installDistribution(Path gdalDistributionRootFolderPath, GDALVersion gdalVersion) throws IOException {
        final Path zipFilePath = gdalVersion.getZipFilePath();
        try {
            logger.log(Level.FINE, "Decompress the zip archive to folder '" + gdalDistributionRootFolderPath + "'.");
            FileHelper.unzip(zipFilePath, gdalDistributionRootFolderPath, true);
        } finally {
            try {
                Files.deleteIfExists(zipFilePath);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "GDAL configuration error: failed to delete the zip archive after decompression.", e);
            }
        }
    }

    private static void deleteDistribution(Path gdalNativeLibrariesFolderPath){
        if (Files.exists(gdalNativeLibrariesFolderPath)) {
            if (!FileUtils.deleteTree(gdalNativeLibrariesFolderPath.toFile())) {
                logger.log(Level.WARNING, () -> "Failed to delete the GDAL distribution folder '" + gdalNativeLibrariesFolderPath + "'.");
            }
        }
    }

    /**
     * Copies and extracts the GDAL distribution archive on specified directory.
     *
     * @param gdalDistributionRootFolderPath the GDAL distribution root directory for install
     * @param gdalVersion                    the GDAL version to which GDAL distribution be installed
     * @throws IOException When IO error occurs
     */
    private static void copyDistributionArchiveAndInstall(Path gdalDistributionRootFolderPath, GDALVersion gdalVersion) throws IOException {
        copyDistributionArchive(gdalDistributionRootFolderPath, gdalVersion);
        installDistribution(gdalDistributionRootFolderPath, gdalVersion);
    }

    private static String computeDistributionHash(Path gdalDistributionRootFolderPath, GDALVersion gdalVersion) throws IOException {
        copyDistributionArchive(gdalDistributionRootFolderPath, gdalVersion);
        final Path zipFilePath = gdalVersion.getZipFilePath();
        try {
            return FileUtils.computeHashForFile(zipFilePath);
        } finally {
            Files.delete(zipFilePath);
        }
    }

    private static void checkDistributionIntegrity(GDALVersion gdalVersion) {
        final Path distributionDirectory = gdalVersion.getLocationPath();
        try {

            final String installedDistributionDirectoryHash = FileUtils.computeHashForDirectory(distributionDirectory);
            final String distributionDirectoryHash = fetchDistributionDirectoryHash(gdalVersion);
            if (!installedDistributionDirectoryHash.equals(distributionDirectoryHash)) {
                throw new IllegalStateException("hash mismatch");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Internal GDAL distribution install integrity check FAILED! Reason: " + e.getMessage());
        }
    }

    /**
     * Fetches the current module specification version from JAR /META-INF/MANIFEST.MF file.
     *
     * @return the current module specification version
     */
    private String fetchCurrentModuleSpecificationVersion() {
        String version = "unknown";
        try {
            final Class<?> clazz = getClass();
            final URL classPathURL = clazz.getResource(clazz.getSimpleName() + ".class");
            if (classPathURL != null) {
                final String classPath = classPathURL.toString();
                String manifestPath;
                if (classPath.startsWith("jar")) {
                    manifestPath = classPath.substring(0, classPath.lastIndexOf('!') + 1) + "/META-INF/MANIFEST.MF";
                } else {
                    // class not from jar archive
                    final String relativePath = clazz.getName().replace('.', File.separatorChar) + ".class";
                    final String classFolder = classPath.substring(0, classPath.length() - relativePath.length() - 1);
                    manifestPath = classFolder + "/META-INF/MANIFEST.MF";
                }
                return new Manifest(new URL(manifestPath).openStream()).getMainAttributes().getValue("OpenIDE-Module-Specification-Version");
            }
        } catch (Exception ignored) {
            //ignored
        }
        return version;
    }

    /**
     * Copies the GDAL distribution/JNI drivers files from distribution package to the target install directory.
     *
     * @param gdalVersion the GDAL version for which files will be installed
     * @throws IOException When IO error occurs
     */
    final void copyDistribution(GDALVersion gdalVersion) throws IOException {
        logger.log(Level.FINE, "Copy the GDAL distribution to folder '" + gdalNativeLibrariesFolderPath.toString() + "'.");
        final String moduleVersion = fetchCurrentModuleSpecificationVersion();

        logger.log(Level.FINE, "The module version is '" + moduleVersion + "'.");

        String distributionHash = null;

        logger.log(Level.FINE, "Check the GDAL distribution folder from the local disk.");

        boolean canCopyGDALDistribution = true;

        final Path gdalDistributionRootFolderPath = gdalVersion.getNativeLibrariesFolderPath();

        final boolean isDistributionInstalled = isDistributionInstalled(gdalNativeLibrariesFolderPath, gdalVersion);
        if (isDistributionInstalled) {
            canCopyGDALDistribution = isModuleUpdated(moduleVersion);
        }
        if (canCopyGDALDistribution) {
            distributionHash = computeDistributionHash(gdalNativeLibrariesFolderPath, gdalVersion);
            logger.log(Level.FINE, "The distribution hash is '" + distributionHash + "'.");
            canCopyGDALDistribution = !isDistributionInstalled || isDistributionUpdated(distributionHash);
        }

        if (canCopyGDALDistribution) {
            deleteDistribution(this.gdalNativeLibrariesFolderPath);
            logger.log(Level.FINE, "create the folder '" + this.gdalNativeLibrariesFolderPath + "' to copy the GDAL distribution.");
            Files.createDirectories(this.gdalNativeLibrariesFolderPath);
            copyEnvironmentVariablesNativeLibrary(gdalVersion);
            copyDistributionArchiveAndInstall(gdalDistributionRootFolderPath, gdalVersion);
            fixUpPermissions(this.gdalNativeLibrariesFolderPath);
            if (!gdalVersion.isJni()) {
                checkDistributionIntegrity(gdalVersion);
            }
            setSavedDistributionHash(distributionHash);
            setSavedModuleSpecificationVersion(moduleVersion);
        }

        registerEnvironmentVariablesNativeLibrary(gdalVersion);
    }

}
