package org.esa.snap.dataio.gdal;

import com.bc.ceres.annotation.STTM;
import org.esa.lib.gdal.GDALTestUtils;
import org.esa.snap.jni.EnvironmentVariables;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.esa.snap.dataio.gdal.GDALVersion.GDAL_NATIVE_LIBRARIES_SRC;
import static org.junit.Assert.*;

public class EnvironmentVariablesNativeLoaderTest {


    private static URL getExpectedEnvironmentVariablesFilePathFromSources() {
        return GDALVersion.class.getClassLoader().getResource(getExpectedEnvironmentVariablesDirectory().replace(File.separator, "/"));
    }

    private static String getExpectedEnvironmentVariablesDirectory() {
        return GDAL_NATIVE_LIBRARIES_SRC + "/" + GDALTestUtils.getExpectedDirectory() + "/" + getExpectedEnvironmentVariablesFileName();
    }

    static String getExpectedEnvironmentVariablesFileName() {
        return GDALTestUtils.getExpectedOSCategory().getOSSpecificEnvironmentVariablesFileName();
    }

    static Path getExpectedEnvironmentVariablesFilePath() {
        return GDALTestUtils.getExpectedNativeLibrariesRootFolderPath().resolve(getExpectedEnvironmentVariablesFileName());
    }


    @Test
    @STTM("SNAP-3472")
    public void testInitEnvironmentVariablesNativeLibrary() {
        EnvironmentVariablesNativeLoader.ensureEnvironmentVariablesNativeInitialised();
        assertTrue(Files.exists(EnvironmentVariablesNativeLoaderTest.getExpectedEnvironmentVariablesFilePath()));
        assertTrue(Files.exists(GDALTestUtils.getExpectedNativeLibrariesRootFolderPath()));
        assertFalse(EnvironmentVariables.getEnvironmentVariable("PATH").isEmpty());
    }

    @Test
    @STTM("SNAP-3523")
    public void testGetEnvironmentVariablesFilePathFromSources() {
        try {
            assertEquals(getExpectedEnvironmentVariablesFilePathFromSources(), EnvironmentVariablesNativeLoader.getEnvironmentVariablesFilePathFromSources());
            assertTrue(Files.exists(Paths.get(Objects.requireNonNull(EnvironmentVariablesNativeLoader.getEnvironmentVariablesFilePathFromSources()).toURI())));
        } catch (Exception e) {
            fail("Error on testGetEnvironmentVariablesFilePathFromSources(): " + e.getMessage());
        }
    }

    @Test
    @STTM("SNAP-3523")
    public void testGetEnvironmentVariablesFilePath() {
        assertEquals(getExpectedEnvironmentVariablesFilePath(), EnvironmentVariablesNativeLoader.getEnvironmentVariablesFilePath());
    }

}
