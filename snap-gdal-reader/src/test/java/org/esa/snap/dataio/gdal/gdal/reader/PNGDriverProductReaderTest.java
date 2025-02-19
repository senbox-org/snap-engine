package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.PNGDriverProductReaderPlugIn;
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
public class PNGDriverProductReaderTest extends AbstractTestDriverProductReader {

    public PNGDriverProductReaderTest() {
    }

    @Test
    public void testPNGReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("PNG-driver.png").toFile();

            PNGDriverProductReaderPlugIn readerPlugin = new PNGDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(4, finalProduct.getBands().length);
            assertEquals("Portable Network Graphics", finalProduct.getProductType());
            assertEquals(1920, finalProduct.getSceneRasterWidth());
            assertEquals(1200, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_3");
            assertEquals(20, band.getDataType());
            assertEquals(2304000, band.getNumDataElems());

            float bandValue = band.getSampleFloat(620, 410);
            assertEquals(219.0f, bandValue, 0);

            bandValue = band.getSampleFloat(543, 444);
            assertEquals(213.0f, bandValue, 0);

            bandValue = band.getSampleFloat(300, 300);
            assertEquals(79.0f, bandValue, 0);

            bandValue = band.getSampleFloat(270, 290);
            assertEquals(74.0f, bandValue, 0);

            bandValue = band.getSampleFloat(158, 335);
            assertEquals(75.0f, bandValue, 0);
        }
    }

    @Test
    public void testPNGReadProductSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("PNG-driver.png").toFile();

            Rectangle subsetRegion = new Rectangle(480, 108, 1165, 1056);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1", "band_2", "band_4"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            PNGDriverProductReaderPlugIn readerPlugin = new PNGDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNull(finalProduct.getSceneGeoCoding());

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(0,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(3, finalProduct.getBands().length);
            assertEquals("Portable Network Graphics", finalProduct.getProductType());
            assertEquals(1165, finalProduct.getSceneRasterWidth());
            assertEquals(1056, finalProduct.getSceneRasterHeight());

            Band band_1 = finalProduct.getBand("band_1");
            assertEquals(20, band_1.getDataType());
            assertEquals(1230240, band_1.getNumDataElems());

            float bandValue = band_1.getSampleFloat(135, 159);
            assertEquals(216.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(509, 239);
            assertEquals(13.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(544, 733);
            assertEquals(170.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(1073, 629);
            assertEquals(74.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(1114, 428);
            assertEquals(169.0f, bandValue, 0);

            Band band_2 = finalProduct.getBand("band_2");
            assertEquals(20, band_2.getDataType());
            assertEquals(1230240, band_2.getNumDataElems());

            bandValue = band_2.getSampleFloat(135, 159);
            assertEquals(220.0f, bandValue, 0);

            bandValue = band_2.getSampleFloat(509, 239);
            assertEquals(14.0f, bandValue, 0);

            bandValue = band_2.getSampleFloat(544, 733);
            assertEquals(179.0f, bandValue, 0);

            bandValue = band_2.getSampleFloat(1073, 629);
            assertEquals(93.0f, bandValue, 0);

            bandValue = band_2.getSampleFloat(1114, 428);
            assertEquals(32.0f, bandValue, 0);

            Band band_4 = finalProduct.getBand("band_4");
            assertEquals(20, band_4.getDataType());
            assertEquals(1230240, band_4.getNumDataElems());

            bandValue = band_4.getSampleFloat(135, 159);
            assertEquals(255.0, bandValue, 0);

            bandValue = band_4.getSampleFloat(509, 239);
            assertEquals(255.0, bandValue, 0);

            bandValue = band_4.getSampleFloat(544, 733);
            assertEquals(255.0, bandValue, 0);

            bandValue = band_4.getSampleFloat(1073, 629);
            assertEquals(255.0, bandValue, 0);

            bandValue = band_4.getSampleFloat(1114, 428);
            assertEquals(255.0, bandValue, 0);
        }
    }
}
