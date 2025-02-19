package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.BMPDriverProductReaderPlugIn;
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
public class BMPDriverProductReaderTest extends AbstractTestDriverProductReader {

    public BMPDriverProductReaderTest() {
    }

    @Test
    public void testBMPReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("BMP-driver.bmp").toFile();

            BMPDriverProductReaderPlugIn readerPlugin = new BMPDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("MS Windows Device Independent Bitmap", finalProduct.getProductType());
            assertEquals(20, finalProduct.getSceneRasterWidth());
            assertEquals(30, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(20, band.getDataType());
            assertEquals(600, band.getNumDataElems());

            float bandValue = band.getSampleFloat(10, 10);
            assertEquals(211.0f, bandValue, 0);

            bandValue = band.getSampleFloat(13, 24);
            assertEquals(238.0f, bandValue, 0);

            bandValue = band.getSampleFloat(6, 23);
            assertEquals(211.0f, bandValue, 0);

            bandValue = band.getSampleFloat(17, 29);
            assertEquals(86.0f, bandValue, 0);

            bandValue = band.getSampleFloat(15, 21);
            assertEquals(180.0f, bandValue, 0);
        }
    }

    @Test
    public void testBMPReadProductSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("BMP-driver.bmp").toFile();

            Rectangle subsetRegion = new Rectangle(15, 5, 5, 16);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            BMPDriverProductReaderPlugIn readerPlugin = new BMPDriverProductReaderPlugIn();

            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNull(finalProduct.getSceneGeoCoding());

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(0,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("MS Windows Device Independent Bitmap", finalProduct.getProductType());
            assertEquals(5, finalProduct.getSceneRasterWidth());
            assertEquals(16, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(20, band.getDataType());
            assertEquals(80, band.getNumDataElems());

            float bandValue = band.getSampleFloat(0, 0);
            assertEquals(116.0f, bandValue, 0);

            bandValue = band.getSampleFloat(4, 10);
            assertEquals(64.0f, bandValue, 0);

            bandValue = band.getSampleFloat(2, 13);
            assertEquals(122.0f, bandValue, 0);

            bandValue = band.getSampleFloat(3, 3);
            assertEquals(179.0f, bandValue, 0);

            bandValue = band.getSampleFloat(2, 7);
            assertEquals(2.0f, bandValue, 0);
        }
    }
}
