package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.BTDriverProductReaderPlugIn;
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
public class BTDriverProductReaderTest extends AbstractTestDriverProductReader {

    public BTDriverProductReaderTest() {
    }

    @Test
    public void testBTReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("BT-driver.bt").toFile();

            BTDriverProductReaderPlugIn readerPlugin = new BTDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("VTP .bt (Binary Terrain) 1.3 Format", finalProduct.getProductType());
            assertEquals(768, finalProduct.getSceneRasterWidth());
            assertEquals(512, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(12, band.getDataType());
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
    public void testBTReadProductSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("BT-driver.bt").toFile();

            Rectangle subsetRegion = new Rectangle(200, 100, 400, 300);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            BTDriverProductReaderPlugIn readerPlugin = new BTDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNull(finalProduct.getSceneGeoCoding());

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(0,finalProduct.getMaskGroup().getNodeNames().length);

            assertEquals(1, finalProduct.getBands().length);
            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(0,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals("VTP .bt (Binary Terrain) 1.3 Format", finalProduct.getProductType());
            assertEquals(400, finalProduct.getSceneRasterWidth());
            assertEquals(300, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(12, band.getDataType());
            assertEquals(120000, band.getNumDataElems());

            float bandValue = band.getSampleFloat(109, 25);
            assertEquals(219.0f, bandValue, 0);

            bandValue = band.getSampleFloat(170, 205);
            assertEquals(67.0f, bandValue, 0);

            bandValue = band.getSampleFloat(171, 263);
            assertEquals(19.0f, bandValue, 0);

            bandValue = band.getSampleFloat(227, 233);
            assertEquals(21.0f, bandValue, 0);

            bandValue = band.getSampleFloat(288, 104);
            assertEquals(83.0f, bandValue, 0);

            bandValue = band.getSampleFloat(379, 201);
            assertEquals(43.0f, bandValue, 0);
        }
    }
}