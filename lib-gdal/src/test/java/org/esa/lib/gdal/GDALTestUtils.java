package org.esa.lib.gdal;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.gdal.OSCategory;

import java.nio.file.Path;

import static org.apache.commons.lang3.SystemUtils.*;
import static org.esa.snap.dataio.gdal.GDALVersion.GDAL_NATIVE_LIBRARIES_ROOT;

public class GDALTestUtils {

    public static String getExpectedDirectory() {
        final OSCategory osCategory = getExpectedOSCategory();
        return osCategory.getOperatingSystemName() + "/" + osCategory.getArchitecture();
    }

    public static OSCategory getExpectedOSCategory() {
        final String sysArch = System.getProperty("os.arch").toLowerCase();
        if (IS_OS_LINUX && sysArch.contains("amd64")) {
            return OSCategory.LINUX_64;
        } else if (IS_OS_MAC_OSX) {
            if (sysArch.contains("amd64") || sysArch.contains("x86_64")) {
                return OSCategory.MAC_OS_X;
            } else if (sysArch.contains("aarch64")) {
                return OSCategory.MAC_OS_X_AARCH64;
            }
        } else if (IS_OS_WINDOWS) {
            if (sysArch.contains("amd64") || sysArch.contains("x86_x64")) {
                return OSCategory.WIN_64;
            } else {
                return OSCategory.WIN_32;
            }
        }
        return OSCategory.UNSUPPORTED;
    }

    public static Path getExpectedNativeLibrariesRootFolderPath() {
        return SystemUtils.getAuxDataPath().resolve(GDAL_NATIVE_LIBRARIES_ROOT);
    }

}
