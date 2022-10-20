package org.esa.s2tbx.dataio.gdal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang.SystemUtils.IS_OS_MAC_OSX;
import static org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS;

/**
 * GDAL OS category enum for defining GDAL compatible operating systems types with SNAP.
 *
 * @author Cosmin Cara
 * @author Adrian Drăghici
 */
public enum OSCategory {
    WIN_32("Windows", "x86", "where"),
    WIN_64("Windows", "x64", "where"),
    LINUX_64("Linux", "x64", "which -a"),
    MAC_OS_X("MacOSX", "x64", "which -a"),
    UNSUPPORTED("", "", "");

    private static final Logger logger = Logger.getLogger(OSCategory.class.getName());

    private static final String ENV_NAME = "environment-variables";

    private static final OSCategory osCategory = retrieveOSCategory();

    private String operatingSystemName;
    private String architecture;
    private String findExecutableLocationCmd;

    /**
     * Creates new instance for this enum.
     *
     * @param operatingSystemName       the operating system name
     * @param architecture              the operating system architecture
     * @param findExecutableLocationCmd the operating system specific command for finding executables location present on PATH environment variable
     */
    OSCategory(String operatingSystemName, String architecture, String findExecutableLocationCmd) {
        this.operatingSystemName = operatingSystemName;
        this.architecture = architecture;
        this.findExecutableLocationCmd = findExecutableLocationCmd;
    }

    /**
     * Gets the actual OS category for host OS
     *
     * @return the actual OS category for host OS
     */
    public static OSCategory getOSCategory() {
        return osCategory;
    }

    /**
     * Retrieves the OS category for host OS by checking java.io.File.SystemUtils constants.
     *
     * @return the OS category for host OS
     */
    private static OSCategory retrieveOSCategory() {
        OSCategory category;
        if (IS_OS_LINUX) {
            category = OSCategory.LINUX_64;
        } else if (IS_OS_MAC_OSX) {
            category = OSCategory.MAC_OS_X;
        } else if (IS_OS_WINDOWS) {
            String sysArch = System.getProperty("os.arch").toLowerCase();
            if (sysArch.contains("amd64") || sysArch.contains("x86_x64")) {
                category = OSCategory.WIN_64;
            } else {
                category = OSCategory.WIN_32;
            }
        } else {
            // we should never be here since we do not release installers for other systems.
            category = OSCategory.UNSUPPORTED;
        }
        return category;
    }

    /**
     * Gets the environment variable native library file name for runtime update of OS environment variables.
     *
     * @return  the environment variable native library file name
     */
    public String getEnvironmentVariablesFileName() {
        return ENV_NAME;
    }

    /**
     * Gets the name of OS.
     *
     * @return the name of OS
     */
    public String getOperatingSystemName() {
        return this.operatingSystemName;
    }

    /**
     * Gets the architecture name of OS.
     *
     * @return the architecture name of OS
     */
    public String getArchitecture() {
        return this.architecture;
    }

    /**
     * Gets the OS specific command for finding executables location present on PATH environment variable by invoking 'where EXECUTABLE_NAME' or 'which EXECUTABLE_NAME' command and parsing the output.
     *
     * @param executableName the target executable name
     * @return the absolute location of executable
     */
    public String[] getExecutableLocations(String executableName) {
        try (java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(this.findExecutableLocationCmd + " " + executableName).getInputStream()).useDelimiter("\\A")) {
            String executableFilePath = s.hasNext() ? s.next() : "";
            executableFilePath = executableFilePath.replaceAll("\\r?\\n", File.pathSeparator);
            if (!executableFilePath.isEmpty()) {
                String[] executableFilePaths = executableFilePath.split(File.pathSeparator);
                String[] executableLocations = new String[executableFilePaths.length];
                for (int i = 0; i < executableFilePaths.length; i++) {
                    executableLocations[i] = Paths.get(executableFilePaths[i]).getParent().toString();
                }
                return executableLocations;
            }
        } catch (IOException ignored) {
            logger.log(Level.INFO, () -> executableName + " not found");
        }
        return new String[0];
    }
}
