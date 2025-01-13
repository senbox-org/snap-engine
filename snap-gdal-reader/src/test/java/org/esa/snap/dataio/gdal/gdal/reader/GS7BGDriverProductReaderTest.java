package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.GS7BGDriverProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Jean Coravu
 */
public class GS7BGDriverProductReaderTest extends AbstractTestDriverProductReader {

    public GS7BGDriverProductReaderTest() {
    }

    @Test
    public void testGS7BGReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("GS7BG-driver.grd").toFile();

            GS7BGDriverProductReaderPlugIn readerPlugin = new GS7BGDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("Golden Software 7 Binary Grid", finalProduct.getProductType());
            assertEquals(768, finalProduct.getSceneRasterWidth());
            assertEquals(512, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(31, band.getDataType());
            assertEquals(393216, band.getNumDataElems());

            float bandValue = band.getSampleFloat(620, 410);
            assertEquals(49.0f, bandValue, 0);

            bandValue = band.getSampleFloat(543, 444);
            assertEquals(90.0f, bandValue, 0);

            bandValue = band.getSampleFloat(300, 300);
            assertEquals(200.0f, bandValue, 0);

            bandValue = band.getSampleFloat(270, 290);
            assertEquals(122.0f, bandValue, 0);

            bandValue = band.getSampleFloat(158, 335);
            assertEquals(125.0f, bandValue, 0);
        }
    }

    @Test
    public void testGS7BGReadProductSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("GS7BG-driver.grd").toFile();

            Rectangle subsetRegion = new Rectangle(240, 110, 500, 300);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1", "nodata_band_1"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            GS7BGDriverProductReaderPlugIn readerPlugin = new GS7BGDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNull(finalProduct.getSceneGeoCoding());

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(1,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("Golden Software 7 Binary Grid", finalProduct.getProductType());
            assertEquals(500, finalProduct.getSceneRasterWidth());
            assertEquals(300, finalProduct.getSceneRasterHeight());

            Mask mask = finalProduct.getMaskGroup().get("nodata_band_1");
            assertEquals(20, mask.getDataType());
            assertEquals(150000, mask.getNumDataElems());

            Band band = finalProduct.getBand("band_1");
            assertEquals(31, band.getDataType());
            assertEquals(150000, band.getNumDataElems());

            float bandValue = band.getSampleFloat(121, 50);
            assertEquals(1.701410009187828E38, bandValue, 0.0f); // the no data value

            bandValue = band.getSampleFloat(170, 114);
            assertEquals(64.0f, bandValue, 0.0f);

            bandValue = band.getSampleFloat(199, 103);
            assertEquals(203.0f, bandValue, 0.0f);

            bandValue = band.getSampleFloat(469, 89);
            assertEquals(165.0f, bandValue, 0.0f);

            bandValue = band.getSampleFloat(466, 140);
            assertEquals(90.0f, bandValue, 0.0f);

            bandValue = band.getSampleFloat(456, 295);
            assertEquals(37.0f, bandValue, 0.0f);
        }
    }
}
