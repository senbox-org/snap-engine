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

package com.bc.ceres.jai.operator;

import com.bc.ceres.jai.ExpressionCompilerConfig;
import com.bc.ceres.jai.opimage.ExpressionCRIF;
import com.bc.ceres.jai.opimage.ExpressionOpImage_1;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static java.lang.Math.sqrt;

public class ExpressionDescriptorTest {

    private static final int W = 4;
    private static final int H = 5;

    private static final byte S1 = (byte) 211;
    private static final short S2 = (short) -2198;
    private static final short S3 = (short) 23753;
    private static final int S4 = 83457;
    private static final float S5 = 5.1f;
    private static final double S6 = 6.4;
    private static final double N3 = -3.6;
    private static final float N2 = 2.7f;
    private static final int N1 = 32;


    private static final String JAI_CORE_NAME = "jai-core";
    private static String jaiCoreLibPath;
    private static final String JAI_CODEC_NAME = "jai-codec";
    private static String jaiCodecLibPath;

    @BeforeClass
    public static void setUpClass() {
        String classPath = System.getProperty("java.class.path");
        String[] split = classPath.split(File.pathSeparator);
        for (String s : split) {
            if (s.contains(JAI_CORE_NAME)) {
                jaiCoreLibPath = s;
            }
            if (s.contains(JAI_CODEC_NAME)) {
                jaiCodecLibPath = s;
            }
        }

        String msg = String.format("Couldn't find %s and/or %s library on classpath", JAI_CORE_NAME, JAI_CODEC_NAME);
        Assume.assumeTrue(msg, jaiCoreLibPath != null || jaiCodecLibPath != null);
    }


    @Test
    public void testDescriptor() {
        ParameterBlockJAI params = createParameterBlock(DataBuffer.TYPE_DOUBLE,
                                                        "S1 * S2 / S3 % S4 + S5 - S6");
        RenderedOp op = JAI.create("Expression", params, null);
        testDestinationImage(op, S1 * S2 / S3 % S4 + S5 - S6);

        params = createParameterBlock(DataBuffer.TYPE_INT,
                                      "S1 * S2 / S3 - S4");
        op = JAI.create("Expression", params, null);
        testDestinationImage(op, S1 * S2 / S3 - S4);

        params = createParameterBlock(DataBuffer.TYPE_FLOAT,
                                      "sqrt(S5)");
        op = JAI.create("Expression", params, null);
        testDestinationImage(op, (float) sqrt(S5));
    }

    @Test
    public void testCRIF() {
        ParameterBlockJAI params = createParameterBlock(DataBuffer.TYPE_DOUBLE,
                                                        "S1 * S2 / S3 % S4 + S5 - S6");
        ExpressionCRIF crif = new ExpressionCRIF();
        RenderedImage image = crif.create(params, null);
        testDestinationImage(image, S1 * S2 / S3 % S4 + S5 - S6);
    }

    /**
     * Tests the ExpressionOpImage code that has been created using the testCreate() method.
     */
    @Test
    public void testGeneratedExpressionOpImage() {
        HashMap<String, RenderedImage> sourceMap = createSourceMap();
        Vector<RenderedImage> sources = new Vector<RenderedImage>();
        sources.add(sourceMap.get("S1"));
        sources.add(sourceMap.get("S2"));
        sources.add(sourceMap.get("S3"));
        sources.add(sourceMap.get("S4"));
        sources.add(sourceMap.get("S5"));
        sources.add(sourceMap.get("S6"));
        OpImage opImage = new ExpressionOpImage_1(sources, null, createImageLayout());
        testDestinationImage(opImage, S1 * S2 / S3 % S4 + S5 - S6);
    }

    static ExpressionCompilerConfig createExpressionCompilerConfig() {
        File outputDir = new File("./target/test-classes");
        File[] classPath = {
                new File(jaiCoreLibPath),
                new File(jaiCodecLibPath),
                new File("./target/classes"),
                new File("./target/test-classes")
        };

        return new ExpressionCompilerConfig(outputDir, classPath);
    }

    private static ParameterBlockJAI createParameterBlock(int dataType, String expression) {
        ParameterBlockJAI args = new ParameterBlockJAI("Expression");
        args.addSource(createSourceMap());
        args.setParameter("dataType", dataType);
        args.setParameter("expression", expression);
        args.setParameter("compilerConfig", createExpressionCompilerConfig());
        return args;
    }

    public static HashMap<String, RenderedImage> createSourceMap() {
        HashMap<String, RenderedImage> map = new HashMap<String, RenderedImage>();
        map.put("S1", createSourceImage(new Byte[]{S1}));
        map.put("S2", createSourceImage(new Short[]{S2}));
        map.put("S3", createSourceImage(new Short[]{S3}));
        map.put("S4", createSourceImage(new Integer[]{S4}));
        map.put("S5", createSourceImage(new Float[]{S5}));
        map.put("S6", createSourceImage(new Double[]{S6}));
        return map;
    }

    static HashMap<String, Number> createConstantMap() {
        HashMap<String, Number> map = new HashMap<String, Number>();
        map.put("N1", N1);
        map.put("N2", N2);
        map.put("N3", N3);
        return map;
    }

    static HashMap<String, Map<String, Integer>> createMaskMap() {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("F1", 0x01);
        map.put("F2", 0x40);
        HashMap<String, Map<String, Integer>> map2 = new HashMap<String, Map<String, Integer>>();
        map2.put("S4", map);
        return map2;
    }

    private static void testDestinationImage(RenderedImage image, double expectedSample) {
        Assert.assertNotNull(image);
        Assert.assertEquals(W, image.getWidth());
        Assert.assertEquals(H, image.getHeight());
        Raster data = image.getData();
        Assert.assertEquals(expectedSample, data.getSampleDouble(0, 0, 0), 1e-10);
        Assert.assertEquals(expectedSample, data.getSampleDouble(1, 1, 0), 1e-10);
        Assert.assertEquals(expectedSample, data.getSampleDouble(2, 2, 0), 1e-10);
        Assert.assertEquals(expectedSample, data.getSampleDouble(3, 3, 0), 1e-10);
    }

    private static void testDestinationImage(RenderedImage image, int expectedSample) {
        Assert.assertNotNull(image);
        Assert.assertEquals(W, image.getWidth());
        Assert.assertEquals(H, image.getHeight());
        Raster data = image.getData();
        Assert.assertEquals(expectedSample, data.getSample(0, 0, 0));
        Assert.assertEquals(expectedSample, data.getSample(1, 1, 0));
        Assert.assertEquals(expectedSample, data.getSample(2, 2, 0));
        Assert.assertEquals(expectedSample, data.getSample(3, 3, 0));
    }

    static RenderedOp createSourceImage(Number[] v) {
        return ConstantDescriptor.create((float) W, (float) H, v, null);
    }

    static ImageLayout createImageLayout() {
        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setWidth(W);
        imageLayout.setHeight(H);
        imageLayout.setSampleModel(new PixelInterleavedSampleModel(DataBuffer.TYPE_DOUBLE, W, H, 1, W, new int[]{0}));
        return imageLayout;
    }


}