/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.dataio.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import com.sun.media.jai.codec.ByteArraySeekableStream;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.MapGeoCoding;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.dataop.maptransf.LambertConformalConicDescriptor;
import org.esa.snap.core.dataop.maptransf.MapInfo;
import org.esa.snap.core.dataop.maptransf.MapProjection;
import org.esa.snap.core.dataop.maptransf.MapProjectionRegistry;
import org.esa.snap.core.dataop.maptransf.MapTransform;
import org.esa.snap.core.dataop.maptransf.MapTransformDescriptor;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.io.FileUtils;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.datum.DefaultGeodeticDatum;
import org.junit.Before;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"InstanceVariableMayNotBeInitialized"})
//@RunWith(LongTestRunner.class)
public class GeoTiffWriteReadTest {
    private static final String WGS_84 = "EPSG:4326";
    private static final String WGS_72 = "EPSG:4322";
    private static final String WGS_84_UTM_ZONE_28S = "EPSG:32728";
    private static final String NEW_ZEALAND_TRANSVERSE_MERCATOR_2000 = "EPSG:2193";
    private static final String WGS84_ARCTIC_POLAR_STEREOGRAPHIC = "EPSG:3995";
    private static final String LAMBERT_CONIC_CONFORMAL_1SP = "EPSG:9801";
    private static final String ALBERS_CONIC_EQUAL_AREA = "Albers_Conic_Equal_Area";

    private Product outProduct;
    private ByteArrayOutputStream outputStream;
    private GeoTiffProductReader reader;
    private File location;

    @Before
    public void setup() {
        reader = (GeoTiffProductReader) new GeoTiffProductReaderPlugIn().createReaderInstance();
        outputStream = new ByteArrayOutputStream();
        location = new File("memory.tif");
        final int width = 14;
        final int height = 14;
        outProduct = new Product("P", "T", width, height);
        final Band bandInt16 = outProduct.addBand("int16", ProductData.TYPE_INT16);
        bandInt16.setDataElems(createShortData(getProductSize(), 23));
        ImageManager.getInstance().getSourceImage(bandInt16, 0);
    }

    @Test
    public void testWriteReadBeamMetadata() throws IOException {
        final Band expectedBand = outProduct.getBand("int16");
        expectedBand.setDescription("Danger");
        expectedBand.setUnit("Voltage");
        expectedBand.setScalingFactor(0.7);
        expectedBand.setScalingOffset(100);
        expectedBand.setLog10Scaled(true);
        expectedBand.setNoDataValue(12.5);
        expectedBand.setNoDataValueUsed(true);

        final Product inProduct = writeReadProduct();

        assertEquals(outProduct.getName(), inProduct.getName());
        assertEquals(outProduct.getProductType(), inProduct.getProductType());
        assertEquals(outProduct.getNumBands(), inProduct.getNumBands());

        final Band actualBand = inProduct.getBandAt(0);
        assertEquals(expectedBand.getName(), actualBand.getName());
        assertEquals(expectedBand.getDescription(), actualBand.getDescription());
        assertEquals(expectedBand.getUnit(), actualBand.getUnit());
        assertEquals(expectedBand.getDataType(), actualBand.getDataType());
        assertEquals(expectedBand.getScalingFactor(), actualBand.getScalingFactor(), 1.0e-6);
        assertEquals(expectedBand.getScalingOffset(), actualBand.getScalingOffset(), 1.0e-6);
        assertEquals(expectedBand.isLog10Scaled(), actualBand.isLog10Scaled());
        assertEquals(expectedBand.getNoDataValue(), actualBand.getNoDataValue(), 1.0e-6);
        assertEquals(expectedBand.isNoDataValueUsed(), actualBand.isNoDataValueUsed());
    }

    @Test
    public void testWriteReadVirtualBandIsNotExcludedInProduct() throws IOException {
        final VirtualBand virtualBand = new VirtualBand("VB", ProductData.TYPE_FLOAT32,
                                                        outProduct.getSceneRasterWidth(),
                                                        outProduct.getSceneRasterHeight(), "X * Y");
        outProduct.addBand(virtualBand);
        final Product inProduct = writeReadProduct();

        assertEquals(2, inProduct.getNumBands());
        assertNotNull(inProduct.getBand("VB"));
    }

    @Test
    public void testWriteReadVirtualBandIsExcludedInImageFile() throws IOException {
        final VirtualBand virtualBand = new VirtualBand("VB", ProductData.TYPE_FLOAT32,
                                                        outProduct.getSceneRasterWidth(),
                                                        outProduct.getSceneRasterHeight(), "X * Y");
        outProduct.addBand(virtualBand);
        final GeoTiffProductWriter writer = (GeoTiffProductWriter) new GeoTiffProductWriterPlugIn().createWriterInstance();
        outProduct.setProductWriter(writer);
        writer.writeGeoTIFFProduct(new MemoryCacheImageOutputStream(outputStream), outProduct);
        final Band[] bands = outProduct.getBands();
        for (Band band : bands) {
            if (writer.shouldWrite(band)) {
                band.readRasterDataFully(ProgressMonitor.NULL);
                writer.writeBandRasterData(band,
                                           0, 0,
                                           band.getRasterWidth(), band.getRasterHeight(),
                                           band.getData(), ProgressMonitor.NULL);
            }
        }
        writer.flush();
        ByteArraySeekableStream inputStream = new ByteArraySeekableStream(outputStream.toByteArray());
        final MemoryCacheImageInputStream imageStream = new MemoryCacheImageInputStream(inputStream);
        Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageStream);
        ImageReader imageReader = null;
        if (imageReaders.hasNext()) {
            imageReader = imageReaders.next();
        }

        if (imageReader == null) {
            throw new IllegalStateException("No ImageReader found");
        }

        imageReader.setInput(imageStream);
        assertEquals(1, imageReader.getNumImages(true));

        final ImageReadParam readParam = imageReader.getDefaultReadParam();
        RenderedImage image = imageReader.readAsRenderedImage(0, readParam);
        assertEquals(1, image.getSampleModel().getNumBands());
        inputStream.close();
    }

    @Test
    public void testWriteReadIndexCodingSingle8BitBand() throws IOException {
        outProduct.removeBand(outProduct.getBandAt(0));
        final Band bandUInt8 = outProduct.addBand("uint8", ProductData.TYPE_UINT8);
        bandUInt8.setDataElems(createByteData(getProductSize(), 23));
        ImageManager.getInstance().getSourceImage(bandUInt8, 0);

        setTiePointGeoCoding(outProduct);
        final IndexCoding indexCoding = new IndexCoding("color_map");
        indexCoding.addIndex("i1", 23, "");
        indexCoding.addIndex("i2", 24, "");
        indexCoding.addIndex("i3", 27, "");
        indexCoding.addIndex("i4", 30, "");
        outProduct.getBandAt(0).setSampleCoding(indexCoding);
        outProduct.getIndexCodingGroup().add(indexCoding);

        final Product inProduct = writeReadProduct();

        assertEquals(1, inProduct.getIndexCodingGroup().getNodeCount());
        final Band indexBand = inProduct.getBandAt(0);
        testIndexCoding(indexBand, 4);
    }

    @Test
    public void testReadProductWithHighestFlagSet() throws Exception {
        URL resource = getClass().getResource("C2RCC_max_flag_test.dim");
        final Product probeProduct = ProductIO.readProduct(new File(resource.toURI()));
        final File tiffFileToTest = File.createTempFile("C2RCC_FLag", ".tif");
        ProductIO.writeProduct(probeProduct, tiffFileToTest, "GeoTIFF", false);
        Product product = reader.readProductNodes(tiffFileToTest, null);
        final Band c2rcc_flags = product.getBand("c2rcc_flags");
        // Pixel at 47, 41; index = 3327
        final long pixelInt = c2rcc_flags.readPixels(47, 41, 1, 1, new int[1])[0];

        assertTrue(BitSetter.isFlagSet(pixelInt, 0));   // RTOSA_OOS
        assertFalse(BitSetter.isFlagSet(pixelInt, 1));  // RTOSA_OOR
        assertFalse(BitSetter.isFlagSet(pixelInt, 2));  // RHOW_OOR
        assertTrue(BitSetter.isFlagSet(pixelInt, 3));   // CLOUD_RISK
        assertFalse(BitSetter.isFlagSet(pixelInt, 4));  // IOP_OOR
        assertFalse(BitSetter.isFlagSet(pixelInt, 5));  // APIG_AT_MAX
        assertFalse(BitSetter.isFlagSet(pixelInt, 6));  // ADET_AT_MAX
        assertFalse(BitSetter.isFlagSet(pixelInt, 7));  // AGELB_AT_MAX
        assertFalse(BitSetter.isFlagSet(pixelInt, 8));  // BPART_AT_MAX
        assertFalse(BitSetter.isFlagSet(pixelInt, 9));  // BWIT_AT_MAX
        assertFalse(BitSetter.isFlagSet(pixelInt, 10)); // APIG_AT_MIN
        assertFalse(BitSetter.isFlagSet(pixelInt, 11)); // ADET_AT_MIN
        assertFalse(BitSetter.isFlagSet(pixelInt, 12)); // AGELB_AT_MIN
        assertFalse(BitSetter.isFlagSet(pixelInt, 13)); // BPART_AT_MIN
        assertFalse(BitSetter.isFlagSet(pixelInt, 14)); // BWIT_AT_MIN
        assertTrue(BitSetter.isFlagSet(pixelInt, 15));  // RHOW_OOS
        assertFalse(BitSetter.isFlagSet(pixelInt, 16)); // KD489_OOR
        assertFalse(BitSetter.isFlagSet(pixelInt, 17)); // KDmin_OOR
        assertFalse(BitSetter.isFlagSet(pixelInt, 18)); // KD489_AT_MAX
        assertFalse(BitSetter.isFlagSet(pixelInt, 19)); // KD489_AT_MIN
        assertTrue(BitSetter.isFlagSet(pixelInt, 31));  // VALID_PE

        final Band iop_apig = product.getBand("iop_apig");
        assertEquals(0.0051777f, iop_apig.readPixels(47, 41, 1, 1, new float[1])[0], 1.0e-6);

        final Band conc_chl = product.getBand("conc_chl");
        assertEquals(0.0880901, conc_chl.readPixels(47, 41, 1, 1, new float[1])[0], 1.0e-6);
    }

    @Test
    public void testWriteReadIndexCodingWith2BandsBand() throws IOException {
        final Band bandUInt8 = outProduct.addBand("uint8", ProductData.TYPE_UINT8);
        bandUInt8.setDataElems(createByteData(getProductSize(), 20));
        ImageManager.getInstance().getSourceImage(bandUInt8, 0);

        setTiePointGeoCoding(outProduct);
        final IndexCoding indexCoding = new IndexCoding("color_map");
        indexCoding.addIndex("i1", 23, "");
        indexCoding.addIndex("i2", 24, "");
        indexCoding.addIndex("i3", 27, "");
        indexCoding.addIndex("i4", 30, "");
        outProduct.getIndexCodingGroup().add(indexCoding);

        outProduct.getBandAt(0).setSampleCoding(indexCoding);
        outProduct.getBandAt(1).setSampleCoding(indexCoding);

        final Product inProduct = writeReadProduct();

        assertEquals(1, inProduct.getIndexCodingGroup().getNodeCount());
        testIndexCoding(inProduct.getBandAt(0), 4);
        testIndexCoding(inProduct.getBandAt(1), 4);
    }

    private void testIndexCoding(Band indexBand, final int expectedIndices) {
        assertTrue(indexBand.isIndexBand());
        assertEquals(expectedIndices, indexBand.getIndexCoding().getNumAttributes());
        final ColorPaletteDef paletteDef = indexBand.getImageInfo(ProgressMonitor.NULL).getColorPaletteDef();
        assertEquals(expectedIndices, paletteDef.getNumColors());
        final Color[] colors = paletteDef.getColors();
        assertNotSame(0, colors[0].getRed() | colors[0].getGreen() | colors[0].getBlue());
        assertNotSame(0, colors[1].getRed() | colors[1].getGreen() | colors[1].getBlue());
        assertNotSame(0, colors[2].getRed() | colors[2].getGreen() | colors[2].getBlue());
        assertNotSame(0, colors[3].getRed() | colors[3].getGreen() | colors[3].getBlue());
    }

    @Test
    public void testWriteReadUTMProjection() throws IOException, TransformException, FactoryException {
        setGeoCoding(outProduct, WGS_84_UTM_ZONE_28S);
        final Product inProduct = writeReadProduct();

        assertEquals(outProduct.getName(), inProduct.getName());
        assertEquals(outProduct.getProductType(), inProduct.getProductType());
        assertEquals(outProduct.getNumBands(), inProduct.getNumBands());
        assertEquals(outProduct.getBandAt(0).getName(), inProduct.getBandAt(0).getName());
        assertEquals(outProduct.getBandAt(0).getDataType(), inProduct.getBandAt(0).getDataType());
        assertEquals(outProduct.getBandAt(0).getScalingFactor(), inProduct.getBandAt(0).getScalingFactor(), 1.0e-6);
        assertEquals(outProduct.getBandAt(0).getScalingOffset(), inProduct.getBandAt(0).getScalingOffset(), 1.0e-6);
        assertNotNull(inProduct.getSceneGeoCoding());
        assertEquality(outProduct.getSceneGeoCoding(), inProduct.getSceneGeoCoding(), 2.0e-5f);
    }

    @Test
    public void testWriteReadLatLonGeocoding() throws IOException, TransformException, FactoryException {
        setGeoCoding(outProduct, WGS_84);
        final Product inProduct = writeReadProduct();

        assertEquals(outProduct.getName(), inProduct.getName());
        assertEquals(outProduct.getProductType(), inProduct.getProductType());
        assertEquals(outProduct.getNumBands(), inProduct.getNumBands());
        assertEquals(outProduct.getBandAt(0).getName(), inProduct.getBandAt(0).getName());
        assertEquals(outProduct.getBandAt(0).getDataType(), inProduct.getBandAt(0).getDataType());
        assertEquals(outProduct.getBandAt(0).getScalingFactor(), inProduct.getBandAt(0).getScalingFactor(), 1.0e-6);
        assertEquals(outProduct.getBandAt(0).getScalingOffset(), inProduct.getBandAt(0).getScalingOffset(), 1.0e-6);
        assertNotNull(inProduct.getSceneGeoCoding());
        assertEquality(outProduct.getSceneGeoCoding(), inProduct.getSceneGeoCoding(), 2.0e-5f);
    }

    @Test
    public void testWriteReadTiePointGeoCoding() throws IOException {
        setTiePointGeoCoding(outProduct);

        final Band band = outProduct.addBand("band_2", ProductData.TYPE_INT16);
        band.setDataElems(createShortData(getProductSize(), 23));

        performTest(2.0e-5f);
    }

    @Test
    public void testWriteReadTransverseMercator() throws IOException, TransformException, FactoryException {
        setGeoCoding(outProduct, NEW_ZEALAND_TRANSVERSE_MERCATOR_2000);

        performTest(2.0e-5f);
    }

    @Test
    public void testWriteReadLambertConformalConic() throws IOException, TransformException, FactoryException {
        setLambertConformalConicGeoCoding(outProduct);

        performTest(2.0e-5f);
    }


    @Test
    public void testWriteReadLambertConformalConic_MapGeoCoding() throws IOException {
        setLambertConformalConicGeoCoding_MapGeoCoding(outProduct);

        performTest(2.0e-4f);
    }

    @Test
    public void testWriteReadStereographic() throws IOException, TransformException, FactoryException {
        setGeoCoding(outProduct, WGS84_ARCTIC_POLAR_STEREOGRAPHIC);

        performTest(2.0e-5f);
    }

    @Test
    public void testWriteReadAlbersEqualArea() throws IOException, TransformException, FactoryException {
        setAlbersEqualAreaGeoCoding(outProduct);

        performTest(2.0e-5f);
    }

    private void performTest(float accuracy) throws IOException {
        final Product inProduct = writeReadProduct();

        assertEquals(outProduct.getName(), inProduct.getName());
        assertEquals(outProduct.getProductType(), inProduct.getProductType());
        assertEquals(outProduct.getNumBands(), inProduct.getNumBands());
        for (int i = 0; i < outProduct.getNumBands(); i++) {
            assertEquality(outProduct.getBandAt(i), inProduct.getBandAt(i));
        }
        assertNotNull(inProduct.getSceneGeoCoding());
        assertEquality(outProduct.getSceneGeoCoding(), inProduct.getSceneGeoCoding(), accuracy);
    }

    private int getProductSize() {
        final int w = outProduct.getSceneRasterWidth();
        final int h = outProduct.getSceneRasterHeight();
        return w * h;
    }

    private static void assertEquality(Band band1, Band band2) throws IOException {
        assertEquals(band1.getName(), band2.getName());
        assertEquals(band1.getDataType(), band2.getDataType());
        assertEquals(band1.getScalingFactor(), band2.getScalingFactor(), 1.0e-6);
        assertEquals(band1.getScalingOffset(), band2.getScalingOffset(), 1.0e-6);
        final int width = band1.getRasterWidth();
        final int height = band1.getRasterHeight();
        band2.readRasterDataFully(ProgressMonitor.NULL);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                assertEquals(band1.getPixelDouble(i, j), band2.getPixelDouble(i, j), 1.0e-13);
            }
        }
    }

    private void assertEquality(final GeoCoding gc1, final GeoCoding gc2, float accuracy) {
        assertNotNull(gc2);
        assertEquals(gc1.canGetGeoPos(), gc2.canGetGeoPos());
        assertEquals(gc1.canGetPixelPos(), gc2.canGetPixelPos());
        assertEquals(gc1.isCrossingMeridianAt180(), gc2.isCrossingMeridianAt180());

        if (gc1 instanceof CrsGeoCoding) {
            assertEquals(CrsGeoCoding.class, gc2.getClass());
            CRS.equalsIgnoreMetadata(gc1, gc2);
        } else if (gc1 instanceof TiePointGeoCoding) {
            assertEquals(TiePointGeoCoding.class, gc2.getClass());
        }

        final int width = outProduct.getSceneRasterWidth();
        final int height = outProduct.getSceneRasterHeight();
        GeoPos geoPos1 = null;
        GeoPos geoPos2 = null;
        final String msgPattern = "%s at [%d,%d] is not equal:";
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                final PixelPos pixelPos = new PixelPos(i, j);
                geoPos1 = gc1.getGeoPos(pixelPos, geoPos1);
                geoPos2 = gc2.getGeoPos(pixelPos, geoPos2);
                assertEquals(String.format(msgPattern, "Latitude", i, j), geoPos1.lat, geoPos2.lat, accuracy);
                assertEquals(String.format(msgPattern, "Longitude", i, j), geoPos1.lon, geoPos2.lon, accuracy);
            }
        }
    }

    private static short[] createShortData(final int size, final int offset) {
        final short[] shorts = new short[size];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) (i + offset);
        }
        return shorts;
    }

    private static byte[] createByteData(final int size, final int offset) {
        final byte[] bytes = new byte[size];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i + offset);
        }
        return bytes;
    }

    private static float[] createFloats(final int size, final float offset) {
        final float[] floats = new float[size];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = i * 6.3243f + offset;
        }
        return floats;
    }

    private static void setGeoCoding(Product product, String epsgCode) throws FactoryException, TransformException {
        final CoordinateReferenceSystem crs = CRS.decode(epsgCode, true);
        final Rectangle imageBounds = new Rectangle(product.getSceneRasterWidth(), product.getSceneRasterHeight());
        final AffineTransform imageToMap = new AffineTransform();
        imageToMap.translate(0.7, 0.8);
        imageToMap.scale(0.9, -0.8);
        imageToMap.translate(-0.5, -0.6);
        product.setSceneGeoCoding(new CrsGeoCoding(crs, imageBounds, imageToMap));
    }

    private static void setLambertConformalConicGeoCoding_MapGeoCoding(final Product product) {
        final MapTransformDescriptor descriptor = MapProjectionRegistry.getDescriptor(
                LambertConformalConicDescriptor.TYPE_ID);
        final double[] values = descriptor.getParameterDefaultValues();
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i] - 0.001;
        }
        final MapTransform transform = descriptor.createTransform(values);
        final MapProjection mapProjection = new MapProjection(descriptor.getTypeID(), transform);
        final MapInfo mapInfo = new MapInfo(mapProjection, .5f, .6f, .7f, .8f, .09f, .08f, Datum.WGS_84);
        mapInfo.setSceneWidth(product.getSceneRasterWidth());
        mapInfo.setSceneHeight(product.getSceneRasterHeight());
        product.setSceneGeoCoding(new MapGeoCoding(mapInfo));
    }

    private static void setLambertConformalConicGeoCoding(final Product product) throws FactoryException,
            TransformException {
        final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final ParameterValueGroup parameters = transformFactory.getDefaultParameters(LAMBERT_CONIC_CONFORMAL_1SP);
        final Ellipsoid ellipsoid = DefaultGeodeticDatum.WGS84.getEllipsoid();
        parameters.parameter("semi_major").setValue(ellipsoid.getSemiMajorAxis());
        parameters.parameter("semi_minor").setValue(ellipsoid.getSemiMinorAxis());
        parameters.parameter("central_meridian").setValue(0.0);
        parameters.parameter("latitude_of_origin").setValue(90.0);
        parameters.parameter("scale_factor").setValue(1.0);

        final MathTransform transform1 = transformFactory.createParameterizedTransform(parameters);
        final DefaultProjectedCRS crs = new DefaultProjectedCRS(parameters.getDescriptor().getName().getCode(),
                                                                (GeographicCRS) CRS.decode(WGS_72, true),
                                                                transform1,
                                                                DefaultCartesianCS.PROJECTED);
        final AffineTransform imageToMap = new AffineTransform();
        imageToMap.translate(0.7, 0.8);
        imageToMap.scale(0.9, -0.8);
        imageToMap.translate(-0.5, -0.6);
        final Rectangle imageBounds = new Rectangle(product.getSceneRasterWidth(), product.getSceneRasterHeight());
        product.setSceneGeoCoding(new CrsGeoCoding(crs, imageBounds, imageToMap));
    }

    private static void setAlbersEqualAreaGeoCoding(final Product product) throws FactoryException, TransformException {
        final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final ParameterValueGroup parameters = transformFactory.getDefaultParameters(ALBERS_CONIC_EQUAL_AREA);
        final org.opengis.referencing.datum.Ellipsoid ellipsoid = DefaultGeodeticDatum.WGS84.getEllipsoid();
        parameters.parameter("semi_major").setValue(ellipsoid.getSemiMajorAxis());
        parameters.parameter("semi_minor").setValue(ellipsoid.getSemiMinorAxis());
        parameters.parameter("latitude_of_origin").setValue(50.0);
        parameters.parameter("central_meridian").setValue(99.0);
        parameters.parameter("standard_parallel_1").setValue(56.0);
        parameters.parameter("false_easting").setValue(1000000.0);
        parameters.parameter("false_northing").setValue(0.0);

        final MathTransform transform1 = transformFactory.createParameterizedTransform(parameters);
        final DefaultProjectedCRS crs = new DefaultProjectedCRS(parameters.getDescriptor().getName().getCode(),
                                                                (GeographicCRS) CRS.decode(WGS_72, true),
                                                                transform1,
                                                                DefaultCartesianCS.PROJECTED);
        final AffineTransform imageToMap = new AffineTransform();
        imageToMap.translate(0.7, 0.8);
        imageToMap.scale(0.9, -0.8);
        imageToMap.translate(-0.5, -0.6);
        final Rectangle imageBounds = new Rectangle(product.getSceneRasterWidth(), product.getSceneRasterHeight());
        product.setSceneGeoCoding(new CrsGeoCoding(crs, imageBounds, imageToMap));

    }

    private static void setTiePointGeoCoding(final Product product) {
        final TiePointGrid latGrid = new TiePointGrid("lat", 3, 3, 0.5f, 0.5f, 5, 5, new float[]{
                85, 84, 83,
                75, 74, 73,
                65, 64, 63
        });
        final TiePointGrid lonGrid = new TiePointGrid("lon", 3, 3, 0.5f, 0.5f, 5, 5, new float[]{
                -15, -5, 5,
                -16, -6, 4,
                -17, -7, 3
        });
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
    }

    private Product writeReadProduct() throws IOException {
        final GeoTiffProductWriter writer = (GeoTiffProductWriter) new GeoTiffProductWriterPlugIn().createWriterInstance();
        outProduct.setProductWriter(writer);
        writer.writeGeoTIFFProduct(new MemoryCacheImageOutputStream(outputStream), outProduct);
        final Band[] bands = outProduct.getBands();
        for (Band band : bands) {
            if (writer.shouldWrite(band)) {
                band.readRasterDataFully(ProgressMonitor.NULL);
                writer.writeBandRasterData(band,
                                           0, 0,
                                           band.getRasterWidth(), band.getRasterHeight(),
                                           band.getData(), ProgressMonitor.NULL);
            }
        }
        writer.flush();

        ByteArraySeekableStream inputStream = new ByteArraySeekableStream(outputStream.toByteArray());
        MemoryCacheImageInputStream imageInputStream = new MemoryCacheImageInputStream(inputStream);
        GeoTiffImageReader geoTiffImageReader = new GeoTiffImageReader(imageInputStream);
        try {
            String defaultProductName = FileUtils.getFilenameWithoutExtension(location.toPath().getFileName().toString());
            Product product = reader.readProduct(geoTiffImageReader, defaultProductName);
            assertNotNull(product);
            assertNull(product.getFileLocation());
            assertNotNull(product.getName());
            assertNotNull(product.getProductReader());
            assertEquals(product.getProductReader(), reader);
            assertTrue(product.getNumBands() > 0);

            return product;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
