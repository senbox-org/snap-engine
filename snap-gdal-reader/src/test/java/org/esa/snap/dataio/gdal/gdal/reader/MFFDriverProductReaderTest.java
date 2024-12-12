package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.MFFDriverProductReaderPlugIn;
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
public class MFFDriverProductReaderTest extends AbstractTestDriverProductReader {

    public MFFDriverProductReaderTest() {
    }

    @Test
    public void testMFFReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("MFF-driver.hdr").toFile();

            MFFDriverProductReaderPlugIn readerPlugin = new MFFDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("Vexcel MFF Raster", finalProduct.getProductType());
            assertEquals(64, finalProduct.getSceneRasterWidth());
            assertEquals(64, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(20, band.getDataType());
            assertEquals(4096, band.getNumDataElems());

            float bandValue = band.getSampleFloat(62, 41);
            assertEquals(117.0f, bandValue, 0);

            bandValue = band.getSampleFloat(54, 44);
            assertEquals(203.0f, bandValue, 0);

            bandValue = band.getSampleFloat(30, 30);
            assertEquals(211.0f, bandValue, 0);

            bandValue = band.getSampleFloat(27, 29);
            assertEquals(224.0f, bandValue, 0);

            bandValue = band.getSampleFloat(15, 33);
            assertEquals(133.0f, bandValue, 0);
        }
    }

    @Test
    public void testMFFReadProductSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("MFF-driver.hdr").toFile();

            Rectangle subsetRegion = new Rectangle(10, 20, 50, 40);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            MFFDriverProductReaderPlugIn readerPlugin = new MFFDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNull(finalProduct.getSceneGeoCoding());

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(0,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("Vexcel MFF Raster", finalProduct.getProductType());
            assertEquals(50, finalProduct.getSceneRasterWidth());
            assertEquals(40, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(20, band.getDataType());
            assertEquals(2000, band.getNumDataElems());

            float bandValue = band.getSampleFloat(12, 24);
            assertEquals(168.0f, bandValue, 0);

            bandValue = band.getSampleFloat(17, 18);
            assertEquals(117.0f, bandValue, 0);

            bandValue = band.getSampleFloat(33, 20);
            assertEquals(212.0f, bandValue, 0);

            bandValue = band.getSampleFloat(33, 33);
            assertEquals(83.0f, bandValue, 0);

            bandValue = band.getSampleFloat(44, 37);
            assertEquals(159.0f, bandValue, 0);
        }
    }
}
