package org.esa.snap.dataio.gdal;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.runtime.internal.IOHelper;
import com.bc.ceres.core.runtime.internal.UrlHelper;
import org.esa.lib.gdal.AbstractGDALTest;
import org.esa.snap.jni.EnvironmentVariables;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.esa.snap.dataio.gdal.GDALVersion.GDAL_NATIVE_LIBRARIES_SRC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EnvironmentVariablesNativeLoaderTest {


    private static URL getExpectedEnvironmentVariablesFilePathFromSources() {
        return GDALVersion.class.getClassLoader().getResource(getExpectedEnvironmentVariablesDirectory().replace(IOHelper.SEPARATOR, "/"));
    }

    private static String getExpectedEnvironmentVariablesDirectory() {
        return GDAL_NATIVE_LIBRARIES_SRC + "/" + AbstractGDALTest.getExpectedDirectory() + "/" + getExpectedEnvironmentVariablesFileName();
    }

    static String getExpectedEnvironmentVariablesFileName() {
        return AbstractGDALTest.getExpectedOSCategory().getOSSpecificEnvironmentVariablesFileName();
    }

    static Path getExpectedEnvironmentVariablesFilePath() {
        return AbstractGDALTest.getExpectedNativeLibrariesRootFolderPath().resolve(getExpectedEnvironmentVariablesFileName());
    }


    @Test
    @STTM("SNAP-3472")
    public void testInitEnvironmentVariablesNativeLibrary() {
        EnvironmentVariablesNativeLoader.ensureEnvironmentVariablesNativeInitialised();
        assertTrue(Files.exists(EnvironmentVariablesNativeLoaderTest.getExpectedEnvironmentVariablesFilePath()));
        assertTrue(Files.exists(AbstractGDALTest.getExpectedNativeLibrariesRootFolderPath()));
        assertFalse(EnvironmentVariables.getEnvironmentVariable("PATH").isEmpty());
    }

    @Test
    @STTM("SNAP-3523")
    public void testGetEnvironmentVariablesFilePathFromSources() {
        try {
            assertEquals(getExpectedEnvironmentVariablesFilePathFromSources(), EnvironmentVariablesNativeLoader.getEnvironmentVariablesFilePathFromSources());
            assertTrue(Files.exists(Objects.requireNonNull(UrlHelper.urlToFile(Objects.requireNonNull(EnvironmentVariablesNativeLoader.getEnvironmentVariablesFilePathFromSources())))));
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
