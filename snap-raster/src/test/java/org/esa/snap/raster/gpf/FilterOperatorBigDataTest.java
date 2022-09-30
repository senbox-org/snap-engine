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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Objects;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Marco Peters
 */
public class FilterOperatorBigDataTest {
    private static Product testProduct;
    private static BufferedImage image;
    private static int width;
    private static int height;
    private static Product tempProduct;
    private int[] expectedData;

    @BeforeClass
    public static void beforeClass() throws Exception {
        image = ImageIO.read(Objects.requireNonNull(FilterOperatorBigDataTest.class.getResourceAsStream("SNAP_visual.png")));
        width = image.getWidth();
        height = image.getHeight();
    }

    @Before
    public void setUp() throws IOException {
        testProduct = new Product("P1", "T", width, height);
        final Band b1 = testProduct.addBand("B1", ProductData.TYPE_INT16);
        expectedData = image.getData().getSamples(0, 0, width, height, 0, new int[width * height]);
        b1.setSourceImage(createOneBandedIntImage(width, height, expectedData));
    }

    @Test
    public void test_Smoothing_ArithMean() throws IOException, URISyntaxException {
        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sourceBands", "B1");
        parameters.put("selectedFilterName", "Arithmetic 3x3 Mean");
        final Product target = GPF.createProduct("Image-Filter", parameters, testProduct);
        final Band band = target.getBand("B1");
        final int[] samples = band.readPixels(0, 0, width, height, new int[width * height]);
//        writeExpectedDataToFile(samples, file));
        int[] expectedSamples = readExpectedDataFromFile(new File(getClass().getResource("SNAP_visual_Ari3x3Mean_exp.bin").toURI()));
        assertArrayEquals(expectedSamples, samples);
    }

    private void writeExpectedDataToFile(int[] data, File file) throws IOException {
        try (DataOutputStream os = new DataOutputStream(new DeflaterOutputStream(Files.newOutputStream(file.toPath())))) {
            os.writeInt(data.length);
            for (int v : data) {
                os.writeInt(v);
            }
        }
    }

    private int[] readExpectedDataFromFile(File file) throws IOException {
        try (DataInputStream is = new DataInputStream(new InflaterInputStream(Files.newInputStream(file.toPath())))) {
            final int length = is.readInt();
            int[] data = new int[length];
            for (int i = 0; i < data.length; i++) {
                data[i] = is.readInt();
            }
            return data;
        }
    }


    static BufferedImage createOneBandedUShortImage(int w, int h, short[] data) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
        DataBufferUShort buffer = (DataBufferUShort) image.getRaster().getDataBuffer();
        System.arraycopy(data, 0, buffer.getData(), 0, w * h);
        return image;
    }

    static BufferedImage createOneBandedIntImage(int w, int h, int[] data) {
        SampleModel sampleModel = new BandedSampleModel(DataBuffer.TYPE_INT, w, h, 1);
        DataBufferInt buffer = new DataBufferInt(data, w * h);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, buffer, null);
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorModel colorModel = new ComponentColorModel(colorSpace, false, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_INT);
        return new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);
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