package org.esa.snap.dataio.gdal;

import com.bc.ceres.annotation.STTM;
import org.esa.lib.gdal.AbstractGDALTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class GDALDistributionInstallerTest {

    private void deleteFTree(Path target) throws IOException {
        if (Files.isDirectory(target)) {
            try (Stream<Path> sp = Files.list(target)) {
                sp.forEach(target1 -> {
                    try {
                        deleteFTree(target1);
                    } catch (Exception ignored) {
                        //ignore
                    }
                });

            }
            try (final Stream<Path> sp = Files.list(target)) {
                if (!sp.findAny().isPresent()) {
                    Files.delete(target);
                }
            }
        } else {
            Files.delete(target);
        }
    }

    private void deleteGDALDirs() throws IOException {
        if (Files.exists(AbstractGDALTest.getExpectedNativeLibrariesRootFolderPath())) {
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

    private void testInstallJNIDistribution() {
        try {
            final GDALVersion gdalVersion = GDALVersion.GDAL_32X_JNI;
            GDALDistributionInstaller.setupDistribution(gdalVersion);
            assertTrue(Files.exists(AbstractGDALTest.getExpectedNativeLibrariesRootFolderPath()));
            assertTrue(Files.exists(GDALVersionTest.getExpectedGDALVersionLocation(gdalVersion)));
            assertTrue(Files.exists(EnvironmentVariablesNativeLoaderTest.getExpectedEnvironmentVariablesFilePath()));
        } catch (IOException e) {
            fail("Error on testSetupDistribution(): " + e.getMessage());
        }
    }

    private void testInstallBundleDistribution() {
        try {
            final GDALVersion gdalVersion = GDALVersion.GDAL_321_FULL;
            GDALDistributionInstaller.setupDistribution(gdalVersion);
            assertTrue(Files.exists(AbstractGDALTest.getExpectedNativeLibrariesRootFolderPath()));
            assertTrue(Files.exists(GDALVersionTest.getExpectedGDALVersionLocation(gdalVersion)));
            assertTrue(Files.exists(EnvironmentVariablesNativeLoaderTest.getExpectedEnvironmentVariablesFilePath()));
        } catch (IOException e) {
            fail("Error on testSetupDistribution(): " + e.getMessage());
        }
    }

    @Test
    @STTM("SNAP-3523")
    public void testSetupDistribution() {
        testInstallJNIDistribution();
        testInstallBundleDistribution();
    }

}
