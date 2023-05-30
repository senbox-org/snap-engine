package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.SGIDriverProductReaderPlugIn;
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
public class SGIDriverProductReaderTest extends AbstractTestDriverProductReader {

    public SGIDriverProductReaderTest() {
    }

    @Test
    public void testSGIReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("KRO-driver.kro").toFile();

            SGIDriverProductReaderPlugIn readerPlugin = new SGIDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(3, finalProduct.getBands().length);
            assertEquals("GDAL", finalProduct.getProductType());
            assertEquals(768, finalProduct.getSceneRasterWidth());
            assertEquals(512, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_3");
            assertEquals(20, band.getDataType());
            assertEquals(393216, band.getNumDataElems());

            float bandValue = band.getSampleFloat(620, 410);
            assertEquals(24.0f, bandValue, 0);

            bandValue = band.getSampleFloat(543, 444);
            assertEquals(57.0f, bandValue, 0);

            bandValue = band.getSampleFloat(300, 300);
            assertEquals(168.0f, bandValue, 0);

            bandValue = band.getSampleFloat(270, 290);
            assertEquals(78.0f, bandValue, 0);

            bandValue = band.getSampleFloat(158, 335);
            assertEquals(87.0f, bandValue, 0);
        }
    }

    @Test
    public void testSGIReadProductSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("KRO-driver.kro").toFile();

            Rectangle subsetRegion = new Rectangle(200, 100, 500, 400);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1", "band_3", "mask_band_2"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            SGIDriverProductReaderPlugIn readerPlugin = new SGIDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNull(finalProduct.getSceneGeoCoding());

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(1,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(2, finalProduct.getBands().length);
            assertEquals("GDAL", finalProduct.getProductType());
            assertEquals(500, finalProduct.getSceneRasterWidth());
            assertEquals(400, finalProduct.getSceneRasterHeight());

            Mask mask = finalProduct.getMaskGroup().get("mask_band_2");
            assertEquals(20, mask.getDataType());
            assertEquals(200000, mask.getNumDataElems());

            Band band_1 = finalProduct.getBand("band_1");
            assertEquals(20, band_1.getDataType());
            assertEquals(200000, band_1.getNumDataElems());

            float bandValue = band_1.getSampleFloat(67, 47);
            assertEquals(80.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(216, 219);
            assertEquals(202.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(382, 41);
            assertEquals(0.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(351, 297);
            assertEquals(161.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(465, 359);
            assertEquals(95.0f, bandValue, 0);

            Band band_3 = finalProduct.getBand("band_3");
            assertEquals(20, band_3.getDataType());
            assertEquals(200000, band_3.getNumDataElems());

            bandValue = band_3.getSampleFloat(67, 47);
            assertEquals(48.0f, bandValue, 0);

            bandValue = band_3.getSampleFloat(216, 219);
            assertEquals(155.0f, bandValue, 0);

            bandValue = band_3.getSampleFloat(382, 41);
            assertEquals(233.0f, bandValue, 0);

            bandValue = band_3.getSampleFloat(351, 297);
            assertEquals(128.0, bandValue, 0);

            bandValue = band_3.getSampleFloat(489, 140);
            assertEquals(239.0f, bandValue, 0);
        }
    }
}
