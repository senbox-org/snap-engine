package org.esa.snap.dataio.gdal;

import com.bc.ceres.annotation.STTM;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class GDALInstallerTest {

    private static final String JAVA_LIB_PATH = "java.library.path";

    private void deleteFTree(Path target) throws IOException {
        if (Files.isDirectory(target)) {
            try (final Stream<Path> sp = Files.list(target)) {
                sp.forEach(target1 -> {
                    try {
                        deleteFTree(target1);
                    } catch (Exception ignored) {
                        //ignore
                    }
                });
            }
            try (final Stream<Path> sp = Files.list(target)) {
                if (sp.findAny().isEmpty()) {
                    Files.delete(target);
                }
            }
        } else {
            Files.delete(target);
        }
    }

    private void deleteGDALDirs() throws IOException {
        if (Files.exists(GDALVersionTest.getExpectedNativeLibrariesRootFolderPath())) {
            Path targetPath = GDALVersionTest.getExpectedGDALVersionLocation(GDALVersion.GDAL_32X_JNI);
            if (Files.exists(targetPath)) {
                deleteFTree(targetPath);
            }
            targetPath = GDALVersionTest.getExpectedGDALVersionLocation(GDALVersion.GDAL_321_FULL);
            if (Files.exists(targetPath)) {
                deleteFTree(targetPath);
            }
        }
    }

    @Before
    public void setUp() {
        try {
            deleteGDALDirs();
        } catch (Exception e) {
            fail("Error on setUp(): " + e.getMessage());
        }
    }

    @After
    public void cleanUp() {
        try {
            deleteGDALDirs();
        } catch (Exception e) {
            fail("Error on cleanUp(): " + e.getMessage());
        }
    }

    @Test
    @STTM("SNAP-3523")
    public void testCopyDistribution() {
        try {
            final GDALVersion gdalVersion = GDALVersion.getInternalVersion();
            gdalVersion.setOsCategory(OSCategory.getOSCategory());
            assertEquals(GDALVersionTest.getExpectedGDALVersionLocation(gdalVersion), gdalVersion.getNativeLibrariesFolderPath());
            GDALInstaller.copyDistribution(gdalVersion);
            assertTrue(Files.exists(GDALVersionTest.getExpectedNativeLibrariesRootFolderPath()));
        } catch (IOException e) {
            fail("Error on testCopyDistribution(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    @STTM("SNAP-3523")
    public void testSetupEnvironmentVariablesNativeLibrary() {
        try {
            GDALInstaller.setupEnvironmentVariablesNativeLibrary();
            assertTrue(Files.exists(GDALVersionTest.getExpectedEnvironmentVariablesFilePath()));
            final Path nativeLibrariesRootFolderPath = GDALVersionTest.getExpectedNativeLibrariesRootFolderPath();
            assertTrue(Files.exists(nativeLibrariesRootFolderPath));
            assertTrue(System.getProperty(JAVA_LIB_PATH).contains(nativeLibrariesRootFolderPath.toString()));
        } catch (IOException e) {
            fail("Error on testInstallBundleDistribution(): " + e.getMessage());
            e.printStackTrace();
        }
    }
}
