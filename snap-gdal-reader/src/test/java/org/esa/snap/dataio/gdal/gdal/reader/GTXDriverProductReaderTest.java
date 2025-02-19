package org.esa.snap.dataio.gdal.gdal.reader;

import com.bc.ceres.binding.ConversionException;
import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.GTXDriverProductReaderPlugIn;
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
import static org.junit.Assert.assertTrue;

/**
 * @author Jean Coravu
 */
public class GTXDriverProductReaderTest extends AbstractTestDriverProductReader {

    public GTXDriverProductReaderTest() {
    }

    @Test
    public void testGTXReadProductNodes() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("GTX-driver.gtx").toFile();

            GTXDriverProductReaderPlugIn readerPlugin = new GTXDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNotNull(finalProduct.getSceneGeoCoding());
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("NOAA Vertical Datum .GTX", finalProduct.getProductType());
            assertEquals(20, finalProduct.getSceneRasterWidth());
            assertEquals(30, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBand("band_1");
            assertEquals(30, band.getDataType());
            assertEquals(600, band.getNumDataElems());

            float bandValue = band.getSampleFloat(10, 15);
            assertEquals(311.0f, bandValue, 0);

            bandValue = band.getSampleFloat(13, 18);
            assertEquals(374.0f, bandValue, 0);

            bandValue = band.getSampleFloat(11, 19);
            assertEquals(392.0f, bandValue, 0);

            bandValue = band.getSampleFloat(12, 28);
            assertEquals(573.0f, bandValue, 0);

            bandValue = band.getSampleFloat(15, 18);
            assertEquals(376.0f, bandValue, 0);
        }
    }

    @Test
    public void testGTXReadProductPixelSubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("GTX-driver.gtx").toFile();

            Rectangle subsetRegion = new Rectangle(5, 10, 10, 20);
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1"} );
            subsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            subsetDef.setSubSampling(1, 1);

            GTXDriverProductReaderPlugIn readerPlugin = new GTXDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNotNull(finalProduct.getSceneGeoCoding());
            GeoPos productOrigin = ProductUtils.getCenterGeoPos(finalProduct);
            assertEquals(0.09f, productOrigin.lat,2);
            assertEquals(0.10f, productOrigin.lon,2);

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(0,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("NOAA Vertical Datum .GTX", finalProduct.getProductType());
            assertEquals(10, finalProduct.getSceneRasterWidth());
            assertEquals(20, finalProduct.getSceneRasterHeight());
            assertEquals(0,finalProduct.getTiePointGridGroup().getNodeNames().length);

            Band band = finalProduct.getBand("band_1");
            assertEquals(30, band.getDataType());
            assertEquals(200, band.getNumDataElems());

            float bandValue = band.getSampleFloat(2, 2);
            assertEquals(248.0f, bandValue, 0);

            bandValue = band.getSampleFloat(6, 8);
            assertEquals(372.0f, bandValue, 0);

            bandValue = band.getSampleFloat(3, 14);
            assertEquals(489, bandValue, 0);

            bandValue = band.getSampleFloat(7, 15);
            assertEquals(513.0f, bandValue, 0);

            bandValue = band.getSampleFloat(9, 19);
            assertEquals(595.0f, bandValue, 0);
        }
    }

    @Test
    public void testGTXReadProductGeometrySubset() throws IOException {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("GTX-driver.gtx").toFile();
            try{
                JtsGeometryConverter converter = new JtsGeometryConverter();
                Geometry geometry = converter.parse("POLYGON ((0.0399999991059303 0.2000000029802322, 0.0500000007450581 0.2000000029802322," +
                                                            " 0.0599999986588955 0.2000000029802322, 0.0700000002980232 0.2000000029802322," +
                                                            " 0.0799999982118607 0.2000000029802322, 0.0900000035762787 0.2000000029802322," +
                                                            " 0.1000000014901161 0.2000000029802322, 0.1099999994039536 0.2000000029802322," +
                                                            " 0.119999997317791 0.2000000029802322, 0.1299999952316284 0.2000000029802322," +
                                                            " 0.1400000005960464 0.2000000029802322, 0.1400000005960464 0.1899999976158142," +
                                                            " 0.1400000005960464 0.1800000071525574, 0.1400000005960464 0.1700000017881393," +
                                                            " 0.1400000005960464 0.1599999964237213, 0.1400000005960464 0.1500000059604645," +
                                                            " 0.1400000005960464 0.1400000005960464, 0.1400000005960464 0.1299999952316284," +
                                                            " 0.1400000005960464 0.119999997317791, 0.1400000005960464 0.1099999994039536," +
                                                            " 0.1400000005960464 0.1000000014901161, 0.1400000005960464 0.0900000035762787," +
                                                            " 0.1400000005960464 0.0799999982118607, 0.1400000005960464 0.0700000002980232," +
                                                            " 0.1400000005960464 0.0599999986588955, 0.1400000005960464 0.0500000007450581," +
                                                            " 0.1400000005960464 0.0399999991059303, 0.1400000005960464 0.0299999993294477," +
                                                            " 0.1400000005960464 0.0199999995529652, 0.1400000005960464 0.0099999997764826," +
                                                            " 0.1400000005960464 0, 0.1299999952316284 0, 0.119999997317791 0, 0.1099999994039536 0," +
                                                            " 0.1000000014901161 0, 0.0900000035762787 0, 0.0799999982118607 0, 0.0700000002980232 0," +
                                                            " 0.0599999986588955 0, 0.0500000007450581 0, 0.0399999991059303 0, 0.0399999991059303 0.0099999997764826," +
                                                            " 0.0399999991059303 0.0199999995529652, 0.0399999991059303 0.0299999993294477," +
                                                            " 0.0399999991059303 0.0399999991059303, 0.0399999991059303 0.0500000007450581," +
                                                            " 0.0399999991059303 0.0599999986588955, 0.0399999991059303 0.0700000002980232, 0.0399999991059303 0.0799999982118607," +
                                                            " 0.0399999991059303 0.0900000035762787, 0.0399999991059303 0.1000000014901161, 0.0399999991059303 0.1099999994039536," +
                                                            " 0.0399999991059303 0.119999997317791, 0.0399999991059303 0.1299999952316284, 0.0399999991059303 0.1400000005960464," +
                                                            " 0.0399999991059303 0.1500000059604645, 0.0399999991059303 0.1599999964237213, 0.0399999991059303 0.1700000017881393," +
                                                            " 0.0399999991059303 0.1800000071525574, 0.0399999991059303 0.1899999976158142, 0.0399999991059303 0.2000000029802322))");

            ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.setNodeNames(new String[] { "band_1"} );
            subsetDef.setSubsetRegion(new GeometrySubsetRegion(geometry, 0));
            subsetDef.setSubSampling(1, 1);

            GTXDriverProductReaderPlugIn readerPlugin = new GTXDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, subsetDef);

            assertNotNull(finalProduct.getSceneGeoCoding());
            GeoPos productOrigin = ProductUtils.getCenterGeoPos(finalProduct);
            assertEquals(0.09f, productOrigin.lat,2);
            assertEquals(0.10f, productOrigin.lon,2);

            assertNotNull(finalProduct.getMaskGroup());
            assertEquals(0,finalProduct.getMaskGroup().getNodeNames().length);
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("NOAA Vertical Datum .GTX", finalProduct.getProductType());
            assertEquals(11, finalProduct.getSceneRasterWidth());
            assertEquals(21, finalProduct.getSceneRasterHeight());
            assertEquals(0,finalProduct.getTiePointGridGroup().getNodeNames().length);

            Band band = finalProduct.getBand("band_1");
            assertEquals(30, band.getDataType());
            assertEquals(231, band.getNumDataElems());

            float bandValue = band.getSampleFloat(2, 2);
            assertEquals(227.0f, bandValue, 0);

            bandValue = band.getSampleFloat(6, 8);
            assertEquals(351.0f, bandValue, 0);

            bandValue = band.getSampleFloat(3, 14);
            assertEquals(468, bandValue, 0);

            bandValue = band.getSampleFloat(7, 15);
            assertEquals(492.0f, bandValue, 0);

            bandValue = band.getSampleFloat(9, 19);
            assertEquals(574.0f, bandValue, 0);
            } catch (ConversionException e) {
                e.printStackTrace();
                assertTrue(e.getMessage(), false);
            }
        }
    }
}
