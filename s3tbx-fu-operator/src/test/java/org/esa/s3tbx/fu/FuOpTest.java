/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.fu;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Color;
import java.util.HashMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Muhammad
 */

public class FuOpTest {

    private static FuOp.Spi operatorSpi;

    @BeforeClass
    public static void setUp() throws Exception {
        operatorSpi = new FuOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi);
    }

    @Test
    public void testFindInstrument() throws Exception {
        Product product = new Product("MER_RR__2P_how_dummy_it_is", "MER_RR__2P");
        assertEquals("MER_RR__2P", product.getProductType());
    }

    @Test
    public void testImageInfoColorPalette() throws Exception {
        ImageInfo indexInfo = FuOp.createImageInfo(new IndexCoding("Forel-Ule Scale"));
        ColorPaletteDef.Point[] points = indexInfo.getColorPaletteDef().getPoints();
        assertEquals(22, points.length);
        for (int i = 0; i < points.length; i++) {
            assertFUColor(i, points[i].getColor());
        }
    }

    @Test
    public void testAttachIndexCodingToBand() throws Exception {
        final Product product = new Product("test_product", "test", 20, 20);
        final Band fuBand = new Band("test_Band", ProductData.TYPE_INT8, 10, 10);
        product.addBand(fuBand);

        assertNull(fuBand.getSampleCoding());
        assertNull(fuBand.getIndexCoding());

        FuOp.attachIndexCoding(fuBand);
        assertNotNull(fuBand.getSampleCoding());
        assertNotNull(fuBand.getIndexCoding());
    }

    @Test
    public void testGetTheBandNamsWithCloseWavelength() throws Exception {
        Product product = new Product("dummy", "dummy", 10, 10);
        addBand(product, "a", 500);
        addBand(product, "a2", 504);
        addBand(product, "a3", 506.5f);
        addBand(product, "a4", 508);
        addBand(product, "b", 600);
        addBand(product, "c", 620);
        addBand(product, "d", 700);
        addBand(product, "e", 712);
        addBand(product, "f", 720);
        addBand(product, "g", 799);


        double[] centralWavelength = {506.0, 603.0, 620.0, 704.0, 720.0, 730.0, 797.0};
        String[] waveBand = FuOp.findWaveBand(product, centralWavelength, 5);
        final String[] expecteds = {"a3", "b", "c", "d", "f", "g"};
        assertArrayEquals(expecteds, waveBand);
    }


    @Test
    public void testGetTheBandNamsWithTheDifferentWavelength() throws Exception {
        Product product = new Product("dummy", "dummy", 10, 10);
        addBand(product, "a", 500);
        addBand(product, "b", 600);
        addBand(product, "c", 620);
        addBand(product, "d", 700);
        addBand(product, "e", 712);
        addBand(product, "f", 720);
        addBand(product, "g", 799);


        final double[] centralWavelength = {504.0, 602.0, 622.0, 703.0, 713.0, 723.0, 800.0};
        final String[] waveBand = FuOp.findWaveBand(product, centralWavelength, 5);
        final String[] expecteds = {"a", "b", "c", "d", "e", "f", "g"};
        assertArrayEquals(expecteds, waveBand);
    }


    @Test
    public void testGetTheBandCloseWavelength() throws Exception {
        Product product = new Product("dummy", "dummy", 10, 10);
        addBand(product, "a", 500);
        addBand(product, "a2", 502.5f);
        addBand(product, "b", 506);

        final double[] centralWavelength = {501, 501.5, 502.0, 505.0, 622.0, 703.0, 713.0, 723.0, 800.0};
        final String[] waveBand = FuOp.findWaveBand(product, centralWavelength, 5);
        final String[] expecteds = {"a", "a2", "a2", "b"};
        assertArrayEquals(expecteds, waveBand);
    }

    @Test
    public void testWithCoastColourLikeInput() throws Exception {
        Product radianceProduct = new Product("CoastColour_L2R", "cc-dummy", 1, 1);
        addBand(radianceProduct, "reflec_1", 412.691f, 0.0209);
        addBand(radianceProduct, "reflec_2", 442.559f, 0.0257);
        addBand(radianceProduct, "reflec_3", 489.882f, 0.0380);
        addBand(radianceProduct, "reflec_4", 509.819f, 0.0416);
        addBand(radianceProduct, "reflec_5", 559.694f, 0.0423);
        addBand(radianceProduct, "reflec_6", 619.601f, 0.0113);
        addBand(radianceProduct, "reflec_7", 664.573f, 0.0063);
        addBand(radianceProduct, "reflec_8", 680.821f, 0.0056);
        addBand(radianceProduct, "reflec_9", 708.329f, 0.0030);
        addBand(radianceProduct, "reflec_10", 753.371f, 8.572e-4);
        addBand(radianceProduct, "reflec_12", 778.409f, 9.388e-4);
        addBand(radianceProduct, "reflec_13", 864.876f, 3.678e-4);

        HashMap<String, Object> radianceParams = new HashMap<>();
        radianceParams.put("validExpression", "true");
        radianceParams.put("instrument", Instrument.MERIS);
        Product radianceResult = GPF.createProduct("FuClassification", radianceParams, radianceProduct);

        int radianceFuValue = radianceResult.getBand("FU").getSampleInt(0, 0);
        float radianceHueValue = radianceResult.getBand("hue_angle").getSampleFloat(0, 0);

        Product irradianceProduct = new Product("CoastColour_L2R", "cc-dummy", 1, 1);

        addBand(irradianceProduct, "reflec_1", 412.691f, 0.0209 * Math.PI);
        addBand(irradianceProduct, "reflec_2", 442.559f, 0.0257 * Math.PI);
        addBand(irradianceProduct, "reflec_3", 489.882f, 0.0380 * Math.PI);
        addBand(irradianceProduct, "reflec_4", 509.819f, 0.0416 * Math.PI);
        addBand(irradianceProduct, "reflec_5", 559.694f, 0.0423 * Math.PI);
        addBand(irradianceProduct, "reflec_6", 619.601f, 0.0113 * Math.PI);
        addBand(irradianceProduct, "reflec_7", 664.573f, 0.0063 * Math.PI);
        addBand(irradianceProduct, "reflec_8", 680.821f, 0.0056 * Math.PI);
        addBand(irradianceProduct, "reflec_9", 708.329f, 0.0030 * Math.PI);
        addBand(irradianceProduct, "reflec_10", 753.371f, 8.572e-4 * Math.PI);
        addBand(irradianceProduct, "reflec_12", 778.409f, 9.388e-4 * Math.PI);
        addBand(irradianceProduct, "reflec_13", 864.876f, 3.678e-4 * Math.PI);


        HashMap<String, Object> irradianceParams = new HashMap<>();
        irradianceParams.put("validExpression", "true");
        irradianceParams.put("instrument", Instrument.MERIS);
        irradianceParams.put("inputIsIrradianceReflectance", true);
        Product irradianceResult = GPF.createProduct("FuClassification", irradianceParams, irradianceProduct);
        int irradianceFuValue = irradianceResult.getBand("FU").getSampleInt(0, 0);
        float irradianceHueValue = irradianceResult.getBand("hue_angle").getSampleFloat(0, 0);

        assertEquals(irradianceFuValue, radianceFuValue, 1.0e-6);
        assertEquals(irradianceHueValue, radianceHueValue, 1.0e-6);
    }

    @Test
    public void testFuValueColor() throws Exception {

        assertFUColor(0, new Color(0, 0, 0));
        assertFUColor(1, new Color(33, 88, 188));
        assertFUColor(2, new Color(49, 109, 197));
        assertFUColor(3, new Color(50, 124, 187));
        assertFUColor(4, new Color(75, 128, 160));
        assertFUColor(5, new Color(86, 143, 150));
        assertFUColor(6, new Color(109, 146, 152));
        assertFUColor(7, new Color(105, 140, 134));
        assertFUColor(8, new Color(117, 158, 114));
        assertFUColor(9, new Color(123, 166, 84));
        assertFUColor(10, new Color(125, 174, 56));
        assertFUColor(11, new Color(149, 182, 69));
        assertFUColor(12, new Color(148, 182, 96));
        assertFUColor(13, new Color(165, 188, 118));
        assertFUColor(14, new Color(170, 184, 109));
        assertFUColor(15, new Color(173, 181, 95));
        assertFUColor(16, new Color(168, 169, 101));
        assertFUColor(17, new Color(174, 159, 92));
        assertFUColor(18, new Color(179, 160, 83));
        assertFUColor(19, new Color(175, 138, 68));
        assertFUColor(20, new Color(164, 105, 5));
        assertFUColor(21, new Color(161, 77, 4));

    }

    @Test
    public void testMerisSourceProduct() throws Exception {
        Product radianceProduct = new Product("CoastColour_L2R", "cc-dummy", 1, 1);
        //  SNAP_MERIS.xlsx
        addBand(radianceProduct, "reflec_1", 412.691f, 0.00981);
        addBand(radianceProduct, "reflec_2", 442.559f, 0.011);
        addBand(radianceProduct, "reflec_3", 489.882f, 0.01296);
        addBand(radianceProduct, "reflec_4", 509.819f, 0.01311);
        addBand(radianceProduct, "reflec_5", 559.694f, 0.01193);
        addBand(radianceProduct, "reflec_6", 619.601f, 0.00298);
        addBand(radianceProduct, "reflec_7", 664.573f, 0.0016);
        addBand(radianceProduct, "reflec_8", 680.821f, 0.0014);
        addBand(radianceProduct, "reflec_9", 708.329f, 0.00081);
        HashMap<String, Object> radianceParams = new HashMap<>();
        radianceParams.put("validExpression", "true");
        radianceParams.put("instrument", Instrument.MERIS);
        Product radianceResult = GPF.createProduct("FuClassification", radianceParams, radianceProduct);

        int radianceFuValue = radianceResult.getBand("FU").getSampleInt(0, 0);
        float radianceHueValue = radianceResult.getBand("hue_angle").getSampleFloat(0, 0);
        assertEquals(5, radianceFuValue);
        assertEquals(171.02552795410156, radianceHueValue, 1e-8);
    }


    @Test
    public void testModisSourceProduct() throws Exception {
        Product radianceProduct = new Product("Modis FU_Hue_Value", "dummy", 1, 1);
        // SNAP_MODIS.xlsx
        addBand(radianceProduct, "reflec_1", 412, 0.00242);
        addBand(radianceProduct, "reflec_2", 443, 0.0031);
        addBand(radianceProduct, "reflec_3", 469, 0.0);
        addBand(radianceProduct, "reflec_4", 488, 0.00345);
        addBand(radianceProduct, "reflec_5", 531, 0.0039);
        addBand(radianceProduct, "reflec_6", 547, 0.0);
        addBand(radianceProduct, "reflec_7", 555, 0.00358);
        addBand(radianceProduct, "reflec_8", 645, 0.0);
        addBand(radianceProduct, "reflec_9", 667, 0.00059);
        addBand(radianceProduct, "reflec_10", 678, 0.00063);


        HashMap<String, Object> radianceParams = new HashMap<>();
        radianceParams.put("instrument", Instrument.MODIS);
        Product radianceResult = GPF.createProduct("FuClassification", radianceParams, radianceProduct);

        int radianceFuValue = radianceResult.getBand("FU").getSampleInt(0, 0);
        float radianceHueValue = radianceResult.getBand("hue_angle").getSampleFloat(0, 0);
        assertEquals(6, radianceFuValue);
        assertEquals(162.87364196777344, radianceHueValue, 1e-8);

    }

    @Test
    public void testOLCISourceProduct() throws Exception {
        Product radianceProduct = new Product("OLCI FU_Hue_Value ", "cc-dummy", 1, 1);
        //  SNAP_OLCI.xlsx
        addBand(radianceProduct, "reflec_1", 400.0f, 0.04376);
        addBand(radianceProduct, "reflec_2", 412.5f, 0.02783);
        addBand(radianceProduct, "reflec_3", 442.5f, 0.02534);
        addBand(radianceProduct, "reflec_4", 490.0f, 0.0208);
        addBand(radianceProduct, "reflec_5", 510.0f, 0.01462);
        addBand(radianceProduct, "reflec_6", 560.0f, 0.00549);
        addBand(radianceProduct, "reflec_7", 620.0f, 0.00041);
        addBand(radianceProduct, "reflec_8", 665.0f, 0.00161);
        addBand(radianceProduct, "reflec_9", 673.75f, 0.00164);
        addBand(radianceProduct, "reflec_10", 681.25f, 0.00179);
        addBand(radianceProduct, "reflec_11", 708.75f, 0.00153);

        HashMap<String, Object> radianceParams = new HashMap<>();
        radianceParams.put("instrument", Instrument.OLCI);
        Product radianceResult = GPF.createProduct("FuClassification", radianceParams, radianceProduct);

        int radianceFuValue = radianceResult.getBand("FU").getSampleInt(0, 0);
        float radianceHueValue = radianceResult.getBand("hue_angle").getSampleFloat(0, 0);
        assertEquals(2, radianceFuValue);
        assertEquals(221.7655029296875, radianceHueValue, 1e-8);

    }


    @Test
    public void testSeaWiFSSourceProduct() throws Exception {
        Product radianceProduct = new Product("SeaWIFS FU_Hue_Value", "cc-dummy", 1, 1);
        //  SNAP_SEAWIFS.xlsx
        addBand(radianceProduct, "reflec_1", 412, 0.00011);
        addBand(radianceProduct, "reflec_2", 443, 0.00074);
        addBand(radianceProduct, "reflec_3", 490, 0.00125);
        addBand(radianceProduct, "reflec_4", 510, 0.00159);
        addBand(radianceProduct, "reflec_5", 555, 0.00178);
        addBand(radianceProduct, "reflec_6", 670, 0.00034);

        HashMap<String, Object> radianceParams = new HashMap<>();
        radianceParams.put("instrument", Instrument.SEAWIFS);
        Product radianceResult = GPF.createProduct("FuClassification", radianceParams, radianceProduct);

        int radianceFuValue = radianceResult.getBand("FU").getSampleInt(0, 0);
        float radianceHueValue = radianceResult.getBand("hue_angle").getSampleFloat(0, 0);
        assertEquals(8, radianceFuValue);
        assertEquals(100.09454345703125, radianceHueValue, 1e-8);

    }

    private static Band addBand(Product product, String bandName, float wavelength) {
        Band band = new Band(bandName, ProductData.TYPE_FLOAT64, 10, 10);
        band.setSpectralWavelength(wavelength);
        product.addBand(band);
        return band;
    }

    private static void addBand(Product product, String bandName, float wavelength, double value) {
        Band band = addBand(product, bandName, wavelength);
        band.setSourceImage(ConstantDescriptor.create((float) band.getRasterWidth(), (float) band.getRasterHeight(),
                new Double[]{value}, null));
    }

    private void assertFUColor(int fuValue, Color expectedColor) {
        Color fuColor = FuOp.FU_COLORS[fuValue];
        assertEquals(expectedColor.getRed(), fuColor.getRed());
        assertEquals(expectedColor.getGreen(), fuColor.getGreen());
        assertEquals(expectedColor.getBlue(), fuColor.getBlue());
    }
}
