package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.PCIDSKDriverProductReaderPlugIn;
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
public class PCIDSKDriverProductReaderTest extends AbstractTestDriverProductReader {

    public PCIDSKDriverProductReaderTest() {
    }

    @Test
    public void testPCIDSKReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("PCIDSK-driver.pix").toFile();

            PCIDSKDriverProductReaderPlugIn readerPlugin = new PCIDSKDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("PCIDSK Database File", finalProduct.getProductType());
            assertEquals(1024, finalProduct.getSceneRasterWidth());
            assertEquals(1024, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(20, band.getDataType());
            assertEquals(1048576, band.getNumDataElems());

            float bandValue = band.getSampleFloat(32, 11);
            assertEquals(121.0f, bandValue, 0);

            bandValue = band.getSampleFloat(33, 32);
            assertEquals(118.0f, bandValue, 0);

            bandValue = band.getSampleFloat(30, 30);
            assertEquals(123.0f, bandValue, 0);

            bandValue = band.getSampleFloat(27, 29);
            assertEquals(141.0f, bandValue, 0);

            bandValue = band.getSampleFloat(15, 33);
            assertEquals(147.0f, bandValue, 0);
        }
    }

    @Test
    public void testPCIDSKReadProductSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("PCIDSK-driver.pix").toFile();

            Rectangle subsetRegion = new Rectangle(480, 260, 500, 400);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            PCIDSKDriverProductReaderPlugIn readerPlugin = new PCIDSKDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNull(finalProduct.getSceneGeoCoding());

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(0,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("PCIDSK Database File", finalProduct.getProductType());
            assertEquals(500, finalProduct.getSceneRasterWidth());
            assertEquals(400, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(20, band.getDataType());
            assertEquals(200000, band.getNumDataElems());

            float bandValue = band.getSampleFloat(91, 44);
            assertEquals(205.0f, bandValue, 0);

            bandValue = band.getSampleFloat(249, 64);
            assertEquals(43.0f, bandValue, 0);

            bandValue = band.getSampleFloat(418, 51);
            assertEquals(17.0f, bandValue, 0);

            bandValue = band.getSampleFloat(441, 200);
            assertEquals(60.0f, bandValue, 0);

            bandValue = band.getSampleFloat(104, 321);
            assertEquals(24.0f, bandValue, 0);
        }
    }
}
