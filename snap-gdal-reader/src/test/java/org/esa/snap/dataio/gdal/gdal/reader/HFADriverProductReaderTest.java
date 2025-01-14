package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.HFADriverProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
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
public class HFADriverProductReaderTest extends AbstractTestDriverProductReader {

    public HFADriverProductReaderTest() {
    }

    @Test
    public void testHFAReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("HFA-driver.img").toFile();

            HFADriverProductReaderPlugIn readerPlugin = new HFADriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(3, finalProduct.getBands().length);
            assertEquals("Erdas Imagine Images", finalProduct.getProductType());
            assertEquals(768, finalProduct.getSceneRasterWidth());
            assertEquals(512, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("Layer_2");
            assertEquals(20, band.getDataType());
            assertEquals(393216, band.getNumDataElems());

            float bandValue = band.getSampleFloat(620, 410);
            assertEquals(53.0f, bandValue, 0);

            bandValue = band.getSampleFloat(543, 444);
            assertEquals(109.0f, bandValue, 0);

            bandValue = band.getSampleFloat(321, 379);
            assertEquals(60.0f, bandValue, 0);

            bandValue = band.getSampleFloat(276, 299);
            assertEquals(170.0f, bandValue, 0);

            bandValue = band.getSampleFloat(158, 335);
            assertEquals(116.0f, bandValue, 0);
        }
    }

    @Test
    public void testHFAReadProductSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("HFA-driver.img").toFile();

            Rectangle subsetRegion = new Rectangle(200, 100, 500, 400);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "Layer_1","Layer_3"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            HFADriverProductReaderPlugIn readerPlugin = new HFADriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNull(finalProduct.getSceneGeoCoding());

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(0,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(2, finalProduct.getBands().length);
            assertEquals("Erdas Imagine Images", finalProduct.getProductType());
            assertEquals(500, finalProduct.getSceneRasterWidth());
            assertEquals(400, finalProduct.getSceneRasterHeight());

            Band bandLayer3 = finalProduct.getBand("Layer_3");
            assertEquals(20, bandLayer3.getDataType());
            assertEquals(200000, bandLayer3.getNumDataElems());

            float bandValue = bandLayer3.getSampleFloat(62, 88);
            assertEquals(71.0f, bandValue, 0);

            bandValue = bandLayer3.getSampleFloat(162, 119);
            assertEquals(235.0f, bandValue, 0);

            bandValue = bandLayer3.getSampleFloat(431, 297);
            assertEquals(62.0f, bandValue, 0);

            bandValue = bandLayer3.getSampleFloat(225, 217);
            assertEquals(89.0f, bandValue, 0);

            bandValue = bandLayer3.getSampleFloat(306, 51);
            assertEquals(225.0f, bandValue, 0);

            bandValue = bandLayer3.getSampleFloat(388, 299);
            assertEquals(21.0f, bandValue, 0);

            Band bandLayer1 = finalProduct.getBand("Layer_1");
            assertEquals(20, bandLayer1.getDataType());
            assertEquals(200000, bandLayer1.getNumDataElems());

            bandValue = bandLayer1.getSampleFloat(306, 51);
            assertEquals(0.0f, bandValue, 0);

            bandValue = bandLayer1.getSampleFloat(162, 119);
            assertEquals(32.0f, bandValue, 0);

            bandValue = bandLayer1.getSampleFloat(431, 297);
            assertEquals(92.0f, bandValue, 0);

            bandValue = bandLayer1.getSampleFloat(225, 217);
            assertEquals(153.0f, bandValue, 0);

            bandValue = bandLayer1.getSampleFloat(95, 320);
            assertEquals(4.0f, bandValue, 0);

            bandValue = bandLayer1.getSampleFloat(388, 299);
            assertEquals(46.0f, bandValue, 0);
        }
    }
}
