package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.PNMDriverProductReaderPlugIn;
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
public class PNMDriverProductReaderTest extends AbstractTestDriverProductReader {

    public PNMDriverProductReaderTest() {
    }

    @Test
    public void testPNMReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("PNM-driver.pnm").toFile();

            PNMDriverProductReaderPlugIn readerPlugin = new PNMDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(3, finalProduct.getBands().length);
            assertEquals("Portable Pixmap Format", finalProduct.getProductType());
            assertEquals(768, finalProduct.getSceneRasterWidth());
            assertEquals(512, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(20, band.getDataType());
            assertEquals(393216, band.getNumDataElems());

            float bandValue = band.getSampleFloat(620, 410);
            assertEquals(49, bandValue, 0);

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
    public void testPNMReadProductSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("PNM-driver.pnm").toFile();

            Rectangle subsetRegion = new Rectangle(200, 100, 500, 400);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1", "band_3"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            PNMDriverProductReaderPlugIn readerPlugin = new PNMDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNull(finalProduct.getSceneGeoCoding());

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(0,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(2, finalProduct.getBands().length);
            assertEquals("Portable Pixmap Format", finalProduct.getProductType());
            assertEquals(500, finalProduct.getSceneRasterWidth());
            assertEquals(400, finalProduct.getSceneRasterHeight());

            Band band_1 = finalProduct.getBand("band_1");
            assertEquals(20, band_1.getDataType());
            assertEquals(200000, band_1.getNumDataElems());

            float bandValue = band_1.getSampleFloat(62, 88);
            assertEquals(118.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(162, 119);
            assertEquals(32.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(431, 297);
            assertEquals(92.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(225, 217);
            assertEquals(153.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(306, 51);
            assertEquals(0.0f, bandValue, 0);

            Band band_3 = finalProduct.getBand("band_3");
            assertEquals(20, band_3.getDataType());
            assertEquals(200000, band_3.getNumDataElems());

            band_3.getSampleFloat(5, 10);
            assertEquals(0.0f, bandValue, 0);

            bandValue = band_3.getSampleFloat(162, 119);
            assertEquals(235.0f, bandValue, 0);

            bandValue = band_3.getSampleFloat(431, 297);
            assertEquals(62.0f, bandValue, 0);

            bandValue = band_3.getSampleFloat(225, 217);
            assertEquals(89.0f, bandValue, 0);

            bandValue = band_3.getSampleFloat(306, 51);
            assertEquals(225.0f, bandValue, 0);
        }
    }
}
