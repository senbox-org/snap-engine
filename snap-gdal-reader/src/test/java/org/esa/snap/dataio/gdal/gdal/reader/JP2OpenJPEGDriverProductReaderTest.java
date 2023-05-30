package org.esa.snap.dataio.gdal.gdal.reader;

import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.dataio.gdal.reader.plugins.JP2OpenJPEGDriverProductReaderPlugIn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Jean Coravu
 */
public class JP2OpenJPEGDriverProductReaderTest extends AbstractTestDriverProductReader {

    public JP2OpenJPEGDriverProductReaderTest() {
    }
// disable the JP2 test due to JP2 driver no longer used
//    @Test
//    public void testReadProduct() throws IOException {
//        if (GDALInstallInfo.INSTANCE.isPresent()) {
//            File productFile = this.gdalTestsFolderPath.resolve("JP2OpenJPEG-driver.jp2").toFile();
//
//            GDALProductReader reader = buildProductReader();
//            Product product = reader.readProductNodes(productFile, null);
//            assertNotNull(product.getFileLocation());
//            assertNotNull(product.getName());
//            assertNotNull(product.getPreferredTileSize());
//            assertNotNull(product.getProductReader());
//            assertEquals(product.getProductReader(), reader);
//            assertEquals("GDAL", product.getProductType());
//            assertEquals(343, product.getSceneRasterWidth());
//            assertEquals(343, product.getSceneRasterHeight());
//
//            GeoCoding geoCoding = product.getSceneGeoCoding();
//            assertNotNull(geoCoding);
//            CoordinateReferenceSystem coordinateReferenceSystem = geoCoding.getGeoCRS();
//            assertNotNull(coordinateReferenceSystem);
//            assertNotNull(coordinateReferenceSystem.getName());
//            assertEquals("WGS_1984", coordinateReferenceSystem.getName().getCode());
//
//            assertEquals(3, product.getMaskGroup().getNodeCount());
//
//            assertEquals(3, product.getBands().length);
//
//            Band band = product.getBand("band_1");
//            assertEquals(20, band.getDataType());
//            assertEquals(117649, band.getNumDataElems());
//            assertEquals("band_1", band.getName());
//            assertEquals(343, band.getRasterWidth());
//            assertEquals(343, band.getRasterHeight());
//
//            assertEquals(0, band.getSampleInt(0, 0));
//            assertEquals(9, band.getSampleInt(310, 210));
//            assertEquals(12, band.getSampleInt(333, 320));
//            assertEquals(0, band.getSampleInt(234, 165));
//            assertEquals(18, band.getSampleInt(320, 110));
//            assertEquals(10, band.getSampleInt(300, 300));
//            assertEquals(9, band.getSampleInt(277, 298));
//            assertEquals(7, band.getSampleInt(297, 338));
//            assertEquals(8, band.getSampleInt(256, 178));
//            assertEquals(13, band.getSampleInt(342, 342));
//        }
//    }

    // disable the JP2 test due to JP2 driver no longer used
//    @Test
//    public void testReadProductPixelSubset() throws IOException {
//        if (GDALInstallInfo.INSTANCE.isPresent()) {
//            File productFile = this.gdalTestsFolderPath.resolve("JP2OpenJPEG-driver.jp2").toFile();
//
//            ProductSubsetDef subsetDef = new ProductSubsetDef();
//            subsetDef.setNodeNames(new String[] { "band_1", "band_2" } );
//            subsetDef.setSubsetRegion(new PixelSubsetRegion(new Rectangle(123, 100, 210, 200), 0));
//            subsetDef.setSubSampling(1, 1);
//
//            GDALProductReader reader = buildProductReader();
//            Product product = reader.readProductNodes(productFile, subsetDef);
//            assertNotNull(product.getFileLocation());
//            assertNotNull(product.getName());
//            assertNotNull(product.getPreferredTileSize());
//            assertNotNull(product.getProductReader());
//            assertEquals(product.getProductReader(), reader);
//            assertEquals("GDAL", product.getProductType());
//            assertEquals(210, product.getSceneRasterWidth());
//            assertEquals(200, product.getSceneRasterHeight());
//
//            GeoCoding geoCoding = product.getSceneGeoCoding();
//            assertNotNull(geoCoding);
//            CoordinateReferenceSystem coordinateReferenceSystem = geoCoding.getGeoCRS();
//            assertNotNull(coordinateReferenceSystem);
//            assertNotNull(coordinateReferenceSystem.getName());
//            assertEquals("WGS_1984", coordinateReferenceSystem.getName().getCode());
//
//            assertEquals(0, product.getMaskGroup().getNodeCount());
//
//            assertEquals(2, product.getBands().length);
//
//            Band band = product.getBand("band_2");
//            assertEquals(20, band.getDataType());
//            assertEquals(42000, band.getNumDataElems());
//            assertEquals("band_2", band.getName());
//            assertEquals(210, band.getRasterWidth());
//            assertEquals(200, band.getRasterHeight());
//
//            assertEquals(0, band.getSampleInt(0, 0));
//            assertEquals(0, band.getSampleInt(110, 110));
//            assertEquals(0, band.getSampleInt(200, 200));
//            assertEquals(15, band.getSampleInt(198, 165));
//            assertEquals(11, band.getSampleInt(120, 198));
//            assertEquals(0, band.getSampleInt(50, 50));
//            assertEquals(0, band.getSampleInt(100, 100));
//            assertEquals(12, band.getSampleInt(200, 169));
//            assertEquals(11, band.getSampleInt(156, 187));
//            assertEquals(11, band.getSampleInt(209, 199));
//        }
//    }

    // disable the JP2 test due to JP2 driver no longer used
//    @Test
//    public void testReadProductGeometrySubset() throws IOException {
//        if (GDALInstallInfo.INSTANCE.isPresent()) {
//            File productFile = this.gdalTestsFolderPath.resolve("JP2OpenJPEG-driver.jp2").toFile();
//
//            try {
//                JtsGeometryConverter converter = new JtsGeometryConverter();
//                Geometry geometry = converter.parse("POLYGON ((6.029233932495117 44.82414245605469, 6.130279064178467 44.821414947509766," +
//                                                            " 6.23130989074707 44.818599700927734, 6.332325458526611 44.81569290161133," +
//                                                            " 6.43332576751709 44.81269836425781, 6.5343098640441895 44.80961227416992," +
//                                                            " 6.635277271270752 44.80643844604492, 6.736227989196777 44.80317687988281," +
//                                                            " 6.837161064147949 44.799827575683594, 6.877529621124268 44.7984619140625," +
//                                                            " 6.872718811035156 44.72660827636719, 6.867926120758057 44.65475082397461," +
//                                                            " 6.863151550292969 44.5828971862793, 6.858394622802734 44.51103973388672," +
//                                                            " 6.8536553382873535 44.439178466796875, 6.848933696746826 44.3673210144043," +
//                                                            " 6.844229698181152 44.29545974731445, 6.839542865753174 44.223594665527344," +
//                                                            " 6.799567699432373 44.22493362426758, 6.699617862701416 44.22821807861328," +
//                                                            " 6.599650859832764 44.23141860961914, 6.499667644500732 44.234527587890625," +
//                                                            " 6.3996686935424805 44.237552642822266, 6.299654483795166 44.24048614501953," +
//                                                            " 6.199625015258789 44.24333572387695, 6.099580764770508 44.246097564697266," +
//                                                            " 5.999522686004639 44.24877166748047, 6.003188610076904 44.32069778442383," +
//                                                            " 6.0068678855896 44.39262008666992, 6.010560512542725 44.464542388916016," +
//                                                            " 6.014267444610596 44.53646469116211, 6.017988204956055 44.6083869934082," +
//                                                            " 6.021722793579102 44.68030548095703, 6.025471210479736 44.75222396850586," +
//                                                            " 6.029233932495117 44.82414245605469))");
//
//                ProductSubsetDef subsetDef = new ProductSubsetDef();
//                subsetDef.setNodeNames(new String[]{"band_1", "band_2"});
//                subsetDef.setSubsetRegion(new GeometrySubsetRegion(geometry, 0));
//                subsetDef.setSubSampling(1, 1);
//
//                GDALProductReader reader = buildProductReader();
//                Product product = reader.readProductNodes(productFile, subsetDef);
//                assertNotNull(product.getFileLocation());
//                assertNotNull(product.getName());
//                assertNotNull(product.getPreferredTileSize());
//                assertNotNull(product.getProductReader());
//                assertEquals(product.getProductReader(), reader);
//                assertEquals("GDAL", product.getProductType());
//                assertEquals(212, product.getSceneRasterWidth());
//                assertEquals(202, product.getSceneRasterHeight());
//
//                GeoCoding geoCoding = product.getSceneGeoCoding();
//                assertNotNull(geoCoding);
//                CoordinateReferenceSystem coordinateReferenceSystem = geoCoding.getGeoCRS();
//                assertNotNull(coordinateReferenceSystem);
//                assertNotNull(coordinateReferenceSystem.getName());
//                assertEquals("WGS_1984", coordinateReferenceSystem.getName().getCode());
//
//                assertEquals(0, product.getMaskGroup().getNodeCount());
//
//                assertEquals(2, product.getBands().length);
//
//                Band band = product.getBand("band_2");
//                assertNotNull(band);
//
//                assertEquals(20, band.getDataType());
//                assertEquals(42824, band.getNumDataElems());
//                assertEquals("band_2", band.getName());
//                assertEquals(212, band.getRasterWidth());
//                assertEquals(202, band.getRasterHeight());
//
//                assertEquals(0, band.getSampleInt(0, 0));
//                assertEquals(0, band.getSampleInt(110, 110));
//                assertEquals(14, band.getSampleInt(200, 200));
//                assertEquals(15, band.getSampleInt(198, 165));
//                assertEquals(11, band.getSampleInt(120, 198));
//                assertEquals(0, band.getSampleInt(50, 50));
//                assertEquals(0, band.getSampleInt(100, 100));
//                assertEquals(12, band.getSampleInt(200, 169));
//                assertEquals(11, band.getSampleInt(156, 187));
//                assertEquals(11, band.getSampleInt(209, 199));
//            }catch (ConversionException e) {
//                e.printStackTrace();
//                assertTrue(e.getMessage(), false);
//            }
//        }
//    }

    private static GDALProductReader buildProductReader() {
        JP2OpenJPEGDriverProductReaderPlugIn readerPlugin = new JP2OpenJPEGDriverProductReaderPlugIn();
        return (GDALProductReader)readerPlugin.createReaderInstance();
    }
}
