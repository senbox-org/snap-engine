package org.esa.snap.dataio.gdal.gdal.reader;

import com.bc.ceres.binding.ConversionException;
import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.RSTDriverProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.subset.GeometrySubsetRegion;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.converters.JtsGeometryConverter;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Jean Coravu
 */
public class RSTDriverProductReaderTest extends AbstractTestDriverProductReader {

    public RSTDriverProductReaderTest() {
    }

    @Test
    public void testRSTReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("RST-driver.rst").toFile();

            RSTDriverProductReaderPlugIn readerPlugin = new RSTDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNotNull(finalProduct.getSceneGeoCoding());
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("GDAL", finalProduct.getProductType());
            assertEquals(20, finalProduct.getSceneRasterWidth());
            assertEquals(30, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(20, band.getDataType());
            assertEquals(600, band.getNumDataElems());

            float bandValue = band.getSampleFloat(10, 20);
            assertEquals(155.0f, bandValue, 0);

            bandValue = band.getSampleFloat(12, 26);
            assertEquals(21.0f, bandValue, 0);

            bandValue = band.getSampleFloat(16, 16);
            assertEquals(81.0f, bandValue, 0);

            bandValue = band.getSampleFloat(7, 19);
            assertEquals(132.0f, bandValue, 0);

            bandValue = band.getSampleFloat(5, 13);
            assertEquals(10.0f, bandValue, 0);
        }
    }

    @Test
    public void testRSTReadProductPixelSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("RST-driver.rst").toFile();

            Rectangle subsetRegion = new Rectangle(5, 10, 15, 10);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            RSTDriverProductReaderPlugIn readerPlugin = new RSTDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNotNull(finalProduct.getSceneGeoCoding());
            GeoPos productOrigin = ProductUtils.getCenterGeoPos(finalProduct);
            assertEquals(15.00f, productOrigin.lat,2);
            assertEquals(12.50f, productOrigin.lon,2);

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(0,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("GDAL", finalProduct.getProductType());
            assertEquals(15, finalProduct.getSceneRasterWidth());
            assertEquals(10, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(20, band.getDataType());
            assertEquals(150, band.getNumDataElems());

            float bandValue = band.getSampleFloat(2, 0);
            assertEquals(208.0f, bandValue, 0);

            bandValue = band.getSampleFloat(6, 4);
            assertEquals(36.0f, bandValue, 0);

            bandValue = band.getSampleFloat(8, 2);
            assertEquals(254.0f, bandValue, 0);

            bandValue = band.getSampleFloat(6, 7);
            assertEquals(96.0f, bandValue, 0);

            bandValue = band.getSampleFloat(9, 7);
            assertEquals(99.0f, bandValue, 0);

            bandValue = band.getSampleFloat(11, 5);
            assertEquals(61.0f, bandValue, 0);

            bandValue = band.getSampleFloat(10, 7);
            assertEquals(100.0f, bandValue, 0);
        }
    }

    @Test
    public void testRSTReadProductGeometrySubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("RST-driver.rst").toFile();

           try {
               JtsGeometryConverter converter = new JtsGeometryConverter();
               Geometry geometry = converter.parse("POLYGON ((4.5 20.5, 5.5 20.5, 6.5 20.5, 7.5 20.5, 8.5 20.5, 9.5 20.5, 10.5 20.5," +
                                                           " 11.5 20.5, 12.5 20.5, 13.5 20.5, 14.5 20.5, 15.5 20.5, 16.5 20.5, 17.5 20.5," +
                                                           " 18.5 20.5, 19.5 20.5, 19.5 19.5, 19.5 18.5, 19.5 17.5, 19.5 16.5, 19.5 15.5," +
                                                           " 19.5 14.5, 19.5 13.5, 19.5 12.5, 19.5 11.5, 19.5 10.5, 18.5 10.5, 17.5 10.5," +
                                                           " 16.5 10.5, 15.5 10.5, 14.5 10.5, 13.5 10.5, 12.5 10.5, 11.5 10.5, 10.5 10.5," +
                                                           " 9.5 10.5, 8.5 10.5, 7.5 10.5, 6.5 10.5, 5.5 10.5, 4.5 10.5, 4.5 11.5, 4.5 12.5," +
                                                           " 4.5 13.5, 4.5 14.5, 4.5 15.5, 4.5 16.5, 4.5 17.5, 4.5 18.5, 4.5 19.5, 4.5 20.5))");
               ProductSubsetDef subsetDef = new ProductSubsetDef();
               subsetDef.setNodeNames(new String[]{"band_1"});
               subsetDef.setSubsetRegion(new GeometrySubsetRegion(geometry, 0));
               subsetDef.setSubSampling(1, 1);

               RSTDriverProductReaderPlugIn readerPlugin = new RSTDriverProductReaderPlugIn();
               GDALProductReader reader = (GDALProductReader) readerPlugin.createReaderInstance();
               Product finalProduct = reader.readProductNodes(file, subsetDef);

               assertNotNull(finalProduct.getSceneGeoCoding());
               GeoPos productOrigin = ProductUtils.getCenterGeoPos(finalProduct);
               assertEquals(15.00f, productOrigin.lat, 2);
               assertEquals(12.50f, productOrigin.lon, 2);

               assertNotNull(finalProduct.getMaskGroup());
               assertEquals(0, finalProduct.getMaskGroup().getNodeNames().length);
               assertEquals(1, finalProduct.getBands().length);
               assertEquals("GDAL", finalProduct.getProductType());
               assertEquals(16, finalProduct.getSceneRasterWidth());
               assertEquals(11, finalProduct.getSceneRasterHeight());

               Band band = finalProduct.getBand("band_1");
               assertEquals(20, band.getDataType());
               assertEquals(176, band.getNumDataElems());

               float bandValue = band.getSampleFloat(2, 0);
               assertEquals(187.0f, bandValue, 0);

               bandValue = band.getSampleFloat(6, 4);
               assertEquals(15.0f, bandValue, 0);

               bandValue = band.getSampleFloat(8, 2);
               assertEquals(233.0f, bandValue, 0);

               bandValue = band.getSampleFloat(6, 7);
               assertEquals(75.0f, bandValue, 0);

               bandValue = band.getSampleFloat(9, 7);
               assertEquals(78.0f, bandValue, 0);

               bandValue = band.getSampleFloat(11, 5);
               assertEquals(40.0f, bandValue, 0);

               bandValue = band.getSampleFloat(10, 7);
               assertEquals(79.0f, bandValue, 0);
           } catch (ConversionException e) {
               e.printStackTrace();
               assertTrue(e.getMessage(), false);
           }
        }
    }
}
