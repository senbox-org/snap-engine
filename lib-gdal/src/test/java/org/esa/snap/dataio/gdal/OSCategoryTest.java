package org.esa.snap.dataio.gdal;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_MAC_OSX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.esa.snap.dataio.gdal.OSCategory.ENV_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OSCategoryTest {

    private static final String LINUX_OS_NAME = "Linux";
    private static final String WINDOWS_OS_NAME = "Windows";
    private static final String MACOS_OS_NAME = "MacOSX";
    private static final String UNKNOWN_OS_NAME = "";
    private static final String ARCHITECTURE_X64 = "x64";
    private static final String ARCHITECTURE_AARCH64 = "aarch64";
    private static final String ARCHITECTURE_X86 = "x86";
    private static final String ARCHITECTURE_UNKNOWN = "";

    @Test
    @STTM("SNAP-3440")
    public void testGetOSCategory() {
        final OSCategory osCategory = OSCategory.getOSCategory();
        assertNotNull(osCategory);
        final String sysArch = System.getProperty("os.arch").toLowerCase();
        if (IS_OS_LINUX && sysArch.contains("amd64")) {
            assertEquals(OSCategory.LINUX_64, osCategory);
        } else if (IS_OS_MAC_OSX) {
            if (sysArch.contains("amd64")) {
                assertEquals(OSCategory.MAC_OS_X, osCategory);
            } else if (sysArch.contains("aarch64")) {
                assertEquals(OSCategory.MAC_OS_X_AARCH64, osCategory);
            }
        } else if (IS_OS_WINDOWS) {
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
    @STTM("SNAP-3440")
    public void testGetArchitecture() {
        final OSCategory osCategory = OSCategory.getOSCategory();
        assertNotNull(osCategory);
        final String sysArch = System.getProperty("os.arch").toLowerCase();
        if (IS_OS_LINUX && sysArch.contains("amd64")) {
            assertEquals(ARCHITECTURE_X64, osCategory.getArchitecture());
        } else if (IS_OS_MAC_OSX) {
            if (sysArch.contains("amd64")) {
                assertEquals(ARCHITECTURE_X64, osCategory.getArchitecture());
            } else if (sysArch.contains("aarch64")) {
                assertEquals(ARCHITECTURE_AARCH64, osCategory.getArchitecture());
            }
        } else if (IS_OS_WINDOWS) {
            if (sysArch.contains("amd64") || sysArch.contains("x86_x64")) {
                assertEquals(ARCHITECTURE_X64, osCategory.getArchitecture());
            } else {
                assertEquals(ARCHITECTURE_X86, osCategory.getArchitecture());
            }
        } else {
            assertEquals(ARCHITECTURE_UNKNOWN, osCategory.getArchitecture());
        }
    }

}
