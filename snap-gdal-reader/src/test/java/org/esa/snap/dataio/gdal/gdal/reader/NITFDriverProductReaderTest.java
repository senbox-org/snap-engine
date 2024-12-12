package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.NITFDriverProductReaderPlugIn;
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
public class NITFDriverProductReaderTest extends AbstractTestDriverProductReader {

    public NITFDriverProductReaderTest() {
    }

    @Test
    public void testNITFReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("NITF-driver.ntf").toFile();

            NITFDriverProductReaderPlugIn readerPlugin = new NITFDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("National Imagery Transmission Format (GDAL)", finalProduct.getProductType());
            assertEquals(64, finalProduct.getSceneRasterWidth());
            assertEquals(64, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(20, band.getDataType());
            assertEquals(4096, band.getNumDataElems());

            float bandValue = band.getSampleFloat(32, 11);
            assertEquals(108.0f, bandValue, 0);

            bandValue = band.getSampleFloat(33, 32);
            assertEquals(217.0f, bandValue, 0);

            bandValue = band.getSampleFloat(30, 30);
            assertEquals(211.0f, bandValue, 0);

            bandValue = band.getSampleFloat(27, 29);
            assertEquals(224.0f, bandValue, 0);

            bandValue = band.getSampleFloat(15, 33);
            assertEquals(133.0f, bandValue, 0);
        }
    }

    @Test
    public void testNITFReadProductSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("NITF-driver.ntf").toFile();

            Rectangle subsetRegion = new Rectangle(20, 10, 40, 50);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1", "mask_band_1"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            NITFDriverProductReaderPlugIn readerPlugin = new NITFDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNull(finalProduct.getSceneGeoCoding());

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(1,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("National Imagery Transmission Format (GDAL)", finalProduct.getProductType());
            assertEquals(40, finalProduct.getSceneRasterWidth());
            assertEquals(50, finalProduct.getSceneRasterHeight());

            Mask mask = finalProduct.getMaskGroup().get("mask_band_1");
            assertEquals(20, mask.getDataType());
            assertEquals(2000, mask.getNumDataElems());

            Band band = finalProduct.getBand("band_1");
            assertEquals(20, band.getDataType());
            assertEquals(2000, band.getNumDataElems());

            float bandValue = band.getSampleFloat(4, 14);
            assertEquals(234.0f, bandValue, 0);

            bandValue = band.getSampleFloat(9, 25);
            assertEquals(115.0f, bandValue, 0);

            bandValue = band.getSampleFloat(17, 30);
            assertEquals(80.0f, bandValue, 0);

            bandValue = band.getSampleFloat(32, 45);
            assertEquals(94.0f, bandValue, 0);

            bandValue = band.getSampleFloat(33, 4);
            assertEquals(112.0f, bandValue, 0);
        }
    }
}
