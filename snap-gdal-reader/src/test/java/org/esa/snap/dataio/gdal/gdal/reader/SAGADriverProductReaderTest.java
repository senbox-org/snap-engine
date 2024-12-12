package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.SAGADriverProductReaderPlugIn;
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
public class SAGADriverProductReaderTest extends AbstractTestDriverProductReader {

    public SAGADriverProductReaderTest() {
    }

    @Test
    public void testSAGAReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("SAGA-driver.sdat").toFile();

            SAGADriverProductReaderPlugIn readerPlugin = new SAGADriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("SAGA GIS Binary Grid", finalProduct.getProductType());
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
    public void testSAGAReadProductSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("SAGA-driver.sdat").toFile();

            Rectangle subsetRegion = new Rectangle(200, 100, 700, 500);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            SAGADriverProductReaderPlugIn readerPlugin = new SAGADriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNull(finalProduct.getSceneGeoCoding());

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(0,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("SAGA GIS Binary Grid", finalProduct.getProductType());
            assertEquals(700, finalProduct.getSceneRasterWidth());
            assertEquals(500, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(20, band.getDataType());
            assertEquals(350000, band.getNumDataElems());

            float bandValue = band.getSampleFloat(32, 11);
            assertEquals(204.0f, bandValue, 0);

            bandValue = band.getSampleFloat(33, 32);
            assertEquals(156.0f, bandValue, 0);

            bandValue = band.getSampleFloat(30, 30);
            assertEquals(179.0f, bandValue, 0);

            bandValue = band.getSampleFloat(27, 29);
            assertEquals(163.0f, bandValue, 0);

            bandValue = band.getSampleFloat(15, 33);
            assertEquals(186.0f, bandValue, 0);
        }
    }
}
