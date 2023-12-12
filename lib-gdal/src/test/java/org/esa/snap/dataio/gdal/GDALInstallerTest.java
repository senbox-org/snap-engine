package org.esa.snap.dataio.gdal;

import com.bc.ceres.annotation.STTM;
import org.esa.lib.gdal.AbstractGDALTest;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GDALInstallerTest {

    @Test
    @STTM("SNAP-3567")
    public void testCopyDistribution() {
        try {
            final GDALVersion gdalVersion = GDALVersion.getInternalVersion();
            assertEquals(GDALVersionTest.getExpectedGDALVersionLocation(gdalVersion), gdalVersion.getNativeLibrariesFolderPath());
            GDALInstaller.copyDistribution(gdalVersion);
            assertTrue(Files.exists(AbstractGDALTest.getExpectedNativeLibrariesRootFolderPath()));
        } catch (IOException e) {
            fail("Error on testCopyDistribution(): " + e.getMessage());
        }
    }
}
