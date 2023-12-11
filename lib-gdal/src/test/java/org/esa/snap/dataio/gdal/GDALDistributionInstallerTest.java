package org.esa.snap.dataio.gdal;

import com.bc.ceres.annotation.STTM;
import org.esa.lib.gdal.AbstractGDALTest;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GDALDistributionInstallerTest {

    private void testInstallBundleDistribution() {
        try {
            final GDALVersion gdalVersion = GDALVersion.GDAL_372_FULL;
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
        testInstallBundleDistribution();
    }

}
