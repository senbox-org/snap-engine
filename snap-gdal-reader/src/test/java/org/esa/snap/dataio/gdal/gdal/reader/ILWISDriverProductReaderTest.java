package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.ILWISDriverProductReaderPlugIn;
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
public class ILWISDriverProductReaderTest extends AbstractTestDriverProductReader {

    public ILWISDriverProductReaderTest() {
    }

    @Test
    public void testILWISReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("ILWIS-driver.mpr").toFile();

            ILWISDriverProductReaderPlugIn readerPlugin = new ILWISDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("ILWIS Raster Map", finalProduct.getProductType());
            assertEquals(768, finalProduct.getSceneRasterWidth());
            assertEquals(512, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(12, band.getDataType());
            assertEquals(393216, band.getNumDataElems());

            float bandValue = band.getSampleFloat(234, 321);
            assertEquals(61.0f, bandValue, 0);

            bandValue = band.getSampleFloat(543, 444);
            assertEquals(109.0f, bandValue, 0);

            bandValue = band.getSampleFloat(300, 432);
            assertEquals(106.0f, bandValue, 0);

            bandValue = band.getSampleFloat(470, 291);
            assertEquals(114.0f, bandValue, 0);

            bandValue = band.getSampleFloat(458, 500);
            assertEquals(101.0f, bandValue, 0);
        }
    }

    @Test
    public void testILWISReadProductSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("ILWIS-driver.mpr").toFile();

            Rectangle subsetRegion = new Rectangle(200, 100, 500, 400);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            ILWISDriverProductReaderPlugIn readerPlugin = new ILWISDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNull(finalProduct.getSceneGeoCoding());

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(0,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("ILWIS Raster Map", finalProduct.getProductType());
            assertEquals(500, finalProduct.getSceneRasterWidth());
            assertEquals(400, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(12, band.getDataType());
            assertEquals(200000, band.getNumDataElems());

            float bandValue = band.getSampleFloat(105, 85);
            assertEquals(86.0f, bandValue, 0);

            bandValue = band.getSampleFloat(173, 285);
            assertEquals(26.0f, bandValue, 0);

            bandValue = band.getSampleFloat(304, 258);
            assertEquals(28.0f, bandValue, 0);

            bandValue = band.getSampleFloat(309, 192);
            assertEquals(123.0f, bandValue, 0);

            bandValue = band.getSampleFloat(493, 142);
            assertEquals(163.0f, bandValue, 0);
        }
    }
}
