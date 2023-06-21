package org.esa.snap.dataio.gdal;

import org.esa.snap.jni.EnvironmentVariables;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang3.SystemUtils.*;

/**
 * GDAL OS category enum for defining GDAL compatible operating systems types with SNAP.
 *
 * @author Cosmin Cara
 * @author Adrian Drăghici
 */
public enum OSCategory {
    WIN_32("Windows", "x86"),
    WIN_64("Windows", "x64"),
    LINUX_64("Linux", "x64"),
    MAC_OS_X("MacOSX", "x64"),
    UNSUPPORTED("", "");

    private static final Logger logger = Logger.getLogger(OSCategory.class.getName());

    static final String ENV_NAME = "environment-variables";

    private static final OSCategory osCategory = retrieveOSCategory();

    private final String operatingSystemName;
    private final String architecture;

    /**
     * Creates new instance for this enum.
     *
     * @param operatingSystemName       the operating system name
     * @param architecture              the operating system architecture
     */
    OSCategory(String operatingSystemName, String architecture) {
        this.operatingSystemName = operatingSystemName;
        this.architecture = architecture;
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
     * Gets the OS specific environment variable native library file name for runtime update of OS environment variables.
     *
     * @return  the environment variable native library file name
     */
    public String getOSSpecificEnvironmentVariablesFileName() {
        return System.mapLibraryName(getEnvironmentVariablesFileName());
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
     * Gets the absolute location of an executable on system by checking all PATH environment variable values.
     *
     * @param executableName the target executable name
     * @return the absolute location of executable
     */
    public String[] getExecutableLocations(String executableName) {
        List<String> executableLocations = new ArrayList<>();
        final String pathEVValue = EnvironmentVariables.getEnvironmentVariable("PATH");
        final String[] pathValues = pathEVValue.split(File.pathSeparator);
        for (String pathValue : pathValues) {
            final Path executableFilePath = Paths.get(pathValue).resolve(executableName);
            if (Files.exists(executableFilePath)) {
                executableLocations.add(pathValue);
            }
        }
        if(executableLocations.isEmpty()){
            logger.log(Level.WARNING, () -> executableName + " not found");
        }
        return executableLocations.toArray(new String[0]);
    }
}
