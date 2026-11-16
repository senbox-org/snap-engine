package org.esa.snap.dataio.gdal.gdal;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.dataio.gdal.reader.GDALMetadataInspector;
import org.esa.snap.engine_utilities.utils.TestUtil;
import org.esa.snap.runtime.LogUtils4Tests;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * @author Denisa Stefanescu
 */
public class GDALMetadataInspectorTest {

    public GDALMetadataInspectorTest() {
    }

    @BeforeClass
    public static void setup() throws Exception {
        LogUtils4Tests.initLogger();
        GDALLibraryInstaller.install();
    }

    @Test
    public void testGDALMetadataInspector() throws IOException {
        assumeTrue(TestUtil.testdataAvailable());

        if (GDALInstallInfo.INSTANCE.isPresent()) {
            final Path file = TestUtil.getTestFile("_gdal"+ IOHelper.SEPARATOR + "BMP-driver.bmp");
            assertNotNull(file);

            GDALMetadataInspector metadataInspector = new GDALMetadataInspector();
            MetadataInspector.Metadata metadata = metadataInspector.getMetadata(file);
            assertNotNull(metadata);
            assertEquals(20, metadata.getProductWidth());
            assertEquals(30, metadata.getProductHeight());

            assertNull(metadata.getGeoCoding());

            assertNotNull(metadata.getBandList());
            assertEquals(1, metadata.getBandList().size());
            assertTrue(metadata.getBandList().contains("band_1"));

            assertNotNull(metadata.getMaskList());
            assertEquals(1, metadata.getMaskList().size());
        }
    }
}
