package org.esa.snap.dataio.gdal;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.util.Locale;

import static org.apache.commons.lang3.SystemUtils.*;
import static org.esa.snap.dataio.gdal.OSCategory.ENV_NAME;
import static org.junit.Assert.*;

public class OSCategoryTest {

    private static final String LINUX_OS_NAME = "Linux";
    private static final String WINDOWS_OS_NAME = "Windows";
    private static final String MACOS_OS_NAME = "MacOSX";
    private static final String UNKNOWN_OS_NAME = "";
    private static final String ARCHITECTURE_X64 = "x64";
    private static final String ARCHITECTURE_X86 = "x86";
    private static final String ARCHITECTURE_UNKNOWN = "x86";
    private static final String EXECUTABLE_NAME = "ping";
    private static final String UNIX_EXECUTABLE_NAME = "sh";
    private static final String LINUX_OS_EXECUTABLE_LOCATION = "/bin";
    private static final String WINDOWS_OS_EXECUTABLE_LOCATION = "\\system32";
    private static final String MACOS_OS_EXECUTABLE_LOCATION = "/sbin";

    @Test
    public void testGetOSCategory() {
        final OSCategory osCategory = OSCategory.getOSCategory();
        assertNotNull(osCategory);
        if (IS_OS_LINUX) {
            assertEquals(OSCategory.LINUX_64, osCategory);
        } else if (IS_OS_MAC_OSX) {
            assertEquals(OSCategory.MAC_OS_X, osCategory);
        } else if (IS_OS_WINDOWS) {
            final String sysArch = System.getProperty("os.arch").toLowerCase();
            if (sysArch.contains("amd64") || sysArch.contains("x86_x64")) {
                assertEquals(OSCategory.WIN_64, osCategory);
            } else {
                assertEquals(OSCategory.WIN_32, osCategory);
            }
        } else {
            assertEquals(OSCategory.UNSUPPORTED, osCategory);
        }
    }

    @Test
    public void testGetEnvironmentVariablesFileName() {
        final OSCategory osCategory = OSCategory.getOSCategory();
        assertNotNull(osCategory);
        assertEquals(ENV_NAME, osCategory.getEnvironmentVariablesFileName());
    }

    @Test
    public void testGetOSSpecificEnvironmentVariablesFileName() {
        final OSCategory osCategory = OSCategory.getOSCategory();
        assertNotNull(osCategory);
        assertEquals(System.mapLibraryName(ENV_NAME), osCategory.getOSSpecificEnvironmentVariablesFileName());
    }

    @Test
    public void testGetOperatingSystemName() {
        final OSCategory osCategory = OSCategory.getOSCategory();
        assertNotNull(osCategory);
        if (IS_OS_LINUX) {
            assertEquals(LINUX_OS_NAME, osCategory.getOperatingSystemName());
        } else if (IS_OS_MAC_OSX) {
            assertEquals(MACOS_OS_NAME, osCategory.getOperatingSystemName());
        } else if (IS_OS_WINDOWS) {
            assertEquals(WINDOWS_OS_NAME, osCategory.getOperatingSystemName());
        } else {
            assertEquals(UNKNOWN_OS_NAME, osCategory.getOperatingSystemName());
        }
    }

    @Test
    public void testGetArchitecture() {
        final OSCategory osCategory = OSCategory.getOSCategory();
        assertNotNull(osCategory);
        if (IS_OS_LINUX) {
            assertEquals(ARCHITECTURE_X64, osCategory.getArchitecture());
        } else if (IS_OS_MAC_OSX) {
            assertEquals(ARCHITECTURE_X64, osCategory.getArchitecture());
        } else if (IS_OS_WINDOWS) {
            final String sysArch = System.getProperty("os.arch").toLowerCase();
            if (sysArch.contains("amd64") || sysArch.contains("x86_x64")) {
                assertEquals(ARCHITECTURE_X64, osCategory.getArchitecture());
            } else {
                assertEquals(ARCHITECTURE_X86, osCategory.getArchitecture());
            }
        } else {
            assertEquals(ARCHITECTURE_UNKNOWN, osCategory.getArchitecture());
        }
    }

    @Test
        @STTM("SNAP-3523")
    public void testGetExecutableLocations() {
        final OSCategory osCategory = OSCategory.getOSCategory();
        assertNotNull(osCategory);
        if (IS_OS_LINUX) {
            final String[] executableLocations = osCategory.getExecutableLocations(UNIX_EXECUTABLE_NAME);
            assertNotNull(executableLocations);
            assertTrue(executableLocations.length > 0);
            assertTrue(executableLocations[0].endsWith(LINUX_OS_EXECUTABLE_LOCATION));
        } else if (IS_OS_MAC_OSX) {
            final String[] executableLocations = osCategory.getExecutableLocations(EXECUTABLE_NAME);
            assertNotNull(executableLocations);
            assertTrue(executableLocations.length > 0);
            assertTrue(executableLocations[0].endsWith(MACOS_OS_EXECUTABLE_LOCATION));
        } else if (IS_OS_WINDOWS) {
            final String[] executableLocations = osCategory.getExecutableLocations(EXECUTABLE_NAME + ".exe");
            assertNotNull(executableLocations);
            assertTrue(executableLocations.length > 0);
            assertTrue(executableLocations[0].toLowerCase(Locale.ROOT).endsWith(WINDOWS_OS_EXECUTABLE_LOCATION));
        }
    }

}
