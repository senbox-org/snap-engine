package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.KRODriverProductReaderPlugIn;
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
public class KRODriverProductReaderTest extends AbstractTestDriverProductReader {

    public KRODriverProductReaderTest() {
    }

    @Test
    public void testKROReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("KRO-driver.kro").toFile();

            KRODriverProductReaderPlugIn readerPlugin = new KRODriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(3, finalProduct.getBands().length);
            assertEquals("KOLOR Raw", finalProduct.getProductType());
            assertEquals(768, finalProduct.getSceneRasterWidth());
            assertEquals(512, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_2");
            assertEquals(20, band.getDataType());
            assertEquals(393216, band.getNumDataElems());

            float bandValue = band.getSampleFloat(620, 410);
            assertEquals(53.0f, bandValue, 0);

            bandValue = band.getSampleFloat(543, 444);
            assertEquals(109.0f, bandValue, 0);

            bandValue = band.getSampleFloat(300, 300);
            assertEquals(196.0f, bandValue, 0);

            bandValue = band.getSampleFloat(270, 290);
            assertEquals(109.0f, bandValue, 0);

            bandValue = band.getSampleFloat(158, 335);
            assertEquals(116.0f, bandValue, 0);
        }
    }

    @Test
    public void testKROReadProductSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("KRO-driver.kro").toFile();

            Rectangle subsetRegion = new Rectangle(200, 100, 500, 400);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1", "band_3", "mask_band_3"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            KRODriverProductReaderPlugIn readerPlugin = new KRODriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNull(finalProduct.getSceneGeoCoding());

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(1,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(2, finalProduct.getBands().length);
            assertEquals("KOLOR Raw", finalProduct.getProductType());
            assertEquals(500, finalProduct.getSceneRasterWidth());
            assertEquals(400, finalProduct.getSceneRasterHeight());

            Mask mask = finalProduct.getMaskGroup().get("mask_band_3");
            assertEquals(20, mask.getDataType());
            assertEquals(200000, mask.getNumDataElems());

            Band band_1 = finalProduct.getBand("band_1");
            assertEquals(20, band_1.getDataType());
            assertEquals(200000, band_1.getNumDataElems());

            float bandValue = band_1.getSampleFloat(160, 53);
            assertEquals(0.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(212, 104);
            assertEquals(128.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(81, 325);
            assertEquals(2.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(293, 255);
            assertEquals(179.0f, bandValue, 0);

            bandValue = band_1.getSampleFloat(489, 111);
            assertEquals(120.0f, bandValue, 0);

            Band band_3 = finalProduct.getBand("band_3");
            assertEquals(20, band_3.getDataType());
            assertEquals(200000, band_3.getNumDataElems());

            bandValue = band_3.getSampleFloat(160, 53);
            assertEquals(227.0f, bandValue, 0);

            bandValue = band_3.getSampleFloat(212, 104);
            assertEquals(251.0f, bandValue, 0);

            bandValue = band_3.getSampleFloat(81, 325);
            assertEquals(3.0f, bandValue, 0);

            bandValue = band_3.getSampleFloat(293, 255);
            assertEquals(102.0f, bandValue, 0);

            bandValue = band_3.getSampleFloat(489, 111);
            assertEquals(232.0f, bandValue, 0);
        }
    }
}
