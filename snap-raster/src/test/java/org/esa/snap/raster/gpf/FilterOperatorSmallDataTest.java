/*
 * Copyright (c) 2022.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 *
 */

package org.esa.snap.raster.gpf;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.junit.Before;
import org.junit.Test;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Marco Peters
 */
public class FilterOperatorSmallDataTest {
    private static final int width = 5;
    private static final int height = 5;
    private static final short[] sourceDataB1 = new short[]{
            0, 1, 2, 3, 4,
            9, 0, 1, 2, 3,
            8, 9, 0, 1, 2,
            7, 8, 9, 0, 1,
            6, 7, 8, 9, 0,
    };
    private static final float[] sourceDataB2 = new float[]{
            Float.NaN, 1, 2, 3, 4,
            9, Float.NaN, 1, 2, 3,
            8, 9, Float.NaN, 1, 2,
            7, 8, 9, Float.NaN, 1,
            6, 7, 8, 9, Float.NaN,
    };
    private static Product testProduct;

    @Before
    public void setUp() {
        testProduct = new Product("P1", "T", width, height);
        final Band b1 = testProduct.addBand("B1", ProductData.TYPE_INT16);
        BufferedImage b1Image = createOneBandedUShortImage(5, 5, sourceDataB1);
        b1.setSourceImage(b1Image);
        final Band b2 = testProduct.addBand("B2", ProductData.TYPE_FLOAT32);
        BufferedImage b2Image = createOneBandedFloatImage(5, 5, sourceDataB2);
        b2.setSourceImage(b2Image);

    }

    @Test
    public void test_Smoothing_ArithMean() throws IOException {
        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sourceBands", "B1,B2");
        parameters.put("selectedFilterName", "Arithmetic 3x3 Mean");
        final Product target = GPF.createProduct("Image-Filter", parameters, testProduct);
        final Band b1 = target.getBand("B1");
        final float[] b1Pixels = b1.readPixels(0, 0, width, height, new float[width * height]);
        float[] expectedB1Data = new float[]{
                2.2222223f, 1.7777779f, 1.6666667f, 2.6666667f, 3.3333335f,
                4.8888893f, 3.3333335f, 2.1111112f, 2.0000000f, 2.6666667f,
                7.222223f, 5.666667f, 3.3333335f, 2.1111112f, 1.6666667f,
                7.333334f, 6.8888893f, 5.666667f, 3.3333335f, 1.7777778f,
                6.666667f, 7.3333335f, 7.2222223f, 4.8888893f, 2.2222223f};
        assertArrayEquals(expectedB1Data, b1Pixels, 1.0e-6f);

        final Band b2 = target.getBand("B2");
        final float[] b2Pixels = b2.readPixels(0, 0, width, height, new float[width * height]);
        float[] expectedB2Data = new float[]{
                Float.NaN, Float.NaN, Float.NaN, 2.6666667f, 3.3333335f,
                Float.NaN, Float.NaN, Float.NaN, Float.NaN, 2.6666667f,
                Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                7.333334f, Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                6.666667f, 7.3333335f, Float.NaN, Float.NaN, Float.NaN
        };
        assertArrayEquals(expectedB2Data, b2Pixels, 1.0e-6f);
    }

    @Test
    public void test_GradientDetection_SobelNorth() throws IOException {
        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sourceBands", "B1,B2");
        parameters.put("selectedFilterName", "Sobel North");
        final Product target = GPF.createProduct("Image-Filter", parameters, testProduct);
        final Band b1 = target.getBand("B1");
        final float[] b1Pixels = b1.readPixels(0, 0, width, height, new float[width * height]);
        float[] expectedB1Data = new float[]{
                -26.0f, -6.0f, 4.0f, 4.0f, 4.0f,
                -32.0f, -22.0f, -2.0f, 8.0f, 8.0f,
                -2.0f, -22.0f, -22.0f, -2.0f, 8.0f,
                8.0f, -2.0f, -22.0f, -22.0f, -2.0f,
                4.0f, 4.0f, -6.0f, -16.0f, -6.0f
        };
        assertArrayEquals(expectedB1Data, b1Pixels, 1.0e-6f);

        final Band b2 = target.getBand("B2");
        final float[] b2Pixels = b2.readPixels(0, 0, width, height, new float[width * height]);
        float[] expectedB2Data = new float[]{
                Float.NaN, Float.NaN, Float.NaN, 4.0f, 4.0f,
                Float.NaN, Float.NaN, Float.NaN, Float.NaN, 8.0f,
                Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                8.0f, Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                4.0f, 4.0f, Float.NaN, Float.NaN, Float.NaN
        };
        assertArrayEquals(expectedB2Data, b2Pixels, 1.0e-6f);
    }

    @Test
    public void test_Laplacian_Laplace_3x3() throws IOException {
        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sourceBands", "B1,B2");
        parameters.put("selectedFilterName", "Laplace 3x3");
        final Product target = GPF.createProduct("Image-Filter", parameters, testProduct);
        final Band b1 = target.getBand("B1");
        final float[] b1Pixels = b1.readPixels(0, 0, width, height, new float[width * height]);
        float[] expectedB1Data = new float[]{
                -10.0f, 1.0f, 1.0f, 1.0f, 2.0f,
                19.0f, -20.0f, 0.0f, 0.0f, 1.0f,
                -1.0f, 20.0f, -20.0f, 0.0f, 1.0f,
                -1.0f, 0.0f, 20.0f, -20.0f, 1.0f,
                -2.0f, -1.0f, -1.0f, 19.0f, -10.0f
        };
        assertArrayEquals(expectedB1Data, b1Pixels, 1.0e-6f);

        final Band b2 = target.getBand("B2");
        final float[] b2Pixels = b2.readPixels(0, 0, width, height, new float[width * height]);
        float[] expectedB2Data = new float[]{
                Float.NaN, Float.NaN, Float.NaN, 1.0f, 2.0f,
                Float.NaN, Float.NaN, Float.NaN, Float.NaN, 1.0f,
                Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                -1.0f, Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                -2.0f, -1.0f, Float.NaN, Float.NaN, Float.NaN
        };
        assertArrayEquals(expectedB2Data, b2Pixels, 1.0e-6f);
    }

    @Test
    public void test_NonLinear_Mean_consideringNaNs() throws IOException {
        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sourceBands", "B1,B2");
        parameters.put("selectedFilterName", "Mean 3x3");
        final Product target = GPF.createProduct("Image-Filter", parameters, testProduct);
        final Band b1 = target.getBand("B1");
        final float[] pixels = b1.readPixels(0, 0, width, height, new float[width * height]);
        float[] expectedB1Data = new float[]{
                2.2222223f, 1.7777779f, 1.6666667f, 2.6666667f, 3.3333335f,
                4.8888893f, 3.3333335f, 2.1111112f, 2.0000000f, 2.6666667f,
                7.222223f, 5.666667f, 3.3333335f, 2.1111112f, 1.6666667f,
                7.333334f, 6.8888893f, 5.666667f, 3.3333335f, 1.7777778f,
                6.666667f, 7.3333335f, 7.2222223f, 4.8888893f, 2.2222223f};
        assertArrayEquals(expectedB1Data, pixels, 1.0e-6f);

        final Band b2 = target.getBand("B2");
        final float[] b2Pixels = b2.readPixels(0, 0, width, height, new float[width * height]);
        float[] expectedB2Data = new float[]{
                5.0f, 2.6666667f, 1.875f, 2.6666667f, 3.3333333f,
                7.3333335f, 5.0f, 2.7142856f, 2.25f, 2.6666667f,
                8.125f, 7.285714f, 5.0f, 2.7142856f, 1.875f,
                7.3333335f, 7.75f, 7.285714f, 5.0f, 2.6666667f,
                6.6666665f, 7.3333335f, 8.125f, 7.3333335f, 5.0f
        };
        assertArrayEquals(expectedB2Data, b2Pixels, 1.0e-6f);
    }

    @Test
    public void test_Morphology_Erosion() throws IOException {
        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sourceBands", "B1,B2");
        parameters.put("selectedFilterName", "Erosion 3x3");
        final Product target = GPF.createProduct("Image-Filter", parameters, testProduct);
        final Band b1 = target.getBand("B1");
        final float[] pixels = b1.readPixels(0, 0, width, height, new float[width * height]);
        float[] expectedB1Data = new float[]{
                0.0f, 0.0f, 0.0f, 1.0f, 2.0f,
                0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                6.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                6.0f, 6.0f, 0.0f, 0.0f, 0.0f};
        assertArrayEquals(expectedB1Data, pixels, 1.0e-6f);

        final Band b2 = target.getBand("B2");
        final float[] b2Pixels = b2.readPixels(0, 0, width, height, new float[width * height]);
        float[] expectedB2Data = new float[]{
                1.0f, 1.0f, 1.0f, 1.0f, 2.0f,
                1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                7.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                6.0f, 6.0f, 1.0f, 1.0f, 1.0f,
                6.0f, 6.0f, 7.0f, 1.0f, 1.0f,
        };
        assertArrayEquals(expectedB2Data, b2Pixels, 1.0e-6f);
    }

    @STTM("SNAP-3996")
    @Test
    public void test_band_unit() throws IOException {

        final Product sourceProduct = new Product("P1", "T", width, height);
        final Band srcBand = sourceProduct.addBand("B1", ProductData.TYPE_INT16);
        BufferedImage srcBandImage = createOneBandedUShortImage(5, 5, sourceDataB1);
        srcBand.setSourceImage(srcBandImage);
        srcBand.setUnit(Unit.AMPLITUDE);

        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sourceBands", "B1");
        parameters.put("selectedFilterName", "Mean 3x3");
        final Product targetProduct = GPF.createProduct("Image-Filter", parameters, sourceProduct);
        final Band targetBand = targetProduct.getBand("B1");
        final String unit = targetBand.getUnit();
        assertEquals(unit, Unit.AMPLITUDE);
    }


    static BufferedImage createOneBandedUShortImage(int w, int h, short[] data) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
        DataBufferUShort buffer = (DataBufferUShort) image.getRaster().getDataBuffer();
        System.arraycopy(data, 0, buffer.getData(), 0, w * h);
        return image;
    }

    static BufferedImage createOneBandedFloatImage(int w, int h, float[] data) {
        SampleModel sampleModel = new BandedSampleModel(DataBuffer.TYPE_FLOAT, w, h, 1);
        DataBufferFloat buffer = new DataBufferFloat(data, w * h);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, buffer, null);
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorModel colorModel = new ComponentColorModel(colorSpace, false, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_FLOAT);
        return new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);
    }
}