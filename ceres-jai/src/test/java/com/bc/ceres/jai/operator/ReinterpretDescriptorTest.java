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

import static com.bc.ceres.jai.operator.ReinterpretDescriptor.AWT;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.EXPONENTIAL;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.INTERPRET_BYTE_SIGNED;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.INTERPRET_INT_UNSIGNED;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.LINEAR;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.LOGARITHMIC;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.create;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.getTargetDataType;
import static java.awt.image.DataBuffer.TYPE_BYTE;
import static java.awt.image.DataBuffer.TYPE_DOUBLE;
import static java.awt.image.DataBuffer.TYPE_FLOAT;
import static java.awt.image.DataBuffer.TYPE_INT;
import static java.awt.image.DataBuffer.TYPE_SHORT;
import static java.awt.image.DataBuffer.TYPE_UNDEFINED;
import static java.awt.image.DataBuffer.TYPE_USHORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.awt.image.DataBufferFloat;
import java.awt.image.RenderedImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import org.junit.Before;
import org.junit.Test;

public class ReinterpretDescriptorTest {

    private RenderedOp sourceImage;

    @Before
    public void setup() {
        sourceImage = ConstantDescriptor.create(5f, 5f, new Short[]{7}, null);
    }

    @Test
    public void testLinearRescaleUShort() {
        final RenderedImage targetImage = create(sourceImage, 17.0, 11.0, LINEAR, AWT, null);
        assertNotNull(targetImage);
        assertEquals(5, targetImage.getWidth());
        assertEquals(5, targetImage.getHeight());
        assertEquals(TYPE_FLOAT, targetImage.getSampleModel().getDataType());

        final float[] targetData = ((DataBufferFloat) targetImage.getData().getDataBuffer()).getData();
        for (int i = 0; i < targetData.length; i++) {
            assertEquals("i = " + i, 130.0, targetData[i], 0.0);
        }
    }

    @Test
    public void testTargetImageRenderingIsSameAsSourceImageRendering() {
        final RenderedOp targetImage = create(sourceImage, 1.0, 0.0, LINEAR, AWT, null);
        assertSame(sourceImage.getRendering(), targetImage.getRendering());
    }

    @Test
    public void testGetTargetDataType() {
        // Test without rescaling (factor=1.0, offset=0.0, LINEAR scaling)
        assertEquals(TYPE_BYTE, getTargetDataType(TYPE_BYTE, 1.0, 0.0, LINEAR, AWT));
        assertEquals(TYPE_USHORT, getTargetDataType(TYPE_USHORT, 1.0, 0.0, LINEAR, AWT));
        assertEquals(TYPE_SHORT, getTargetDataType(TYPE_SHORT, 1.0, 0.0, LINEAR, AWT));
        assertEquals(TYPE_INT, getTargetDataType(TYPE_INT, 1.0, 0.0, LINEAR, AWT));
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_FLOAT, 1.0, 0.0, LINEAR, AWT));
        assertEquals(TYPE_DOUBLE, getTargetDataType(TYPE_DOUBLE, 1.0, 0.0, LINEAR, AWT));

        // Test with rescaling (factor != 1.0)
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_BYTE, 2.0, 0.0, LINEAR, AWT));
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_USHORT, 2.0, 0.0, LINEAR, AWT));
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_SHORT, 2.0, 0.0, LINEAR, AWT));
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_FLOAT, 2.0, 0.0, LINEAR, AWT));
        assertEquals(TYPE_DOUBLE, getTargetDataType(TYPE_INT, 2.0, 0.0, LINEAR, AWT));
        assertEquals(TYPE_DOUBLE, getTargetDataType(TYPE_DOUBLE, 2.0, 0.0, LINEAR, AWT));

        // Test with rescaling (offset != 0.0)
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_BYTE, 1.0, 5.0, LINEAR, AWT));
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_USHORT, 1.0, 5.0, LINEAR, AWT));
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_SHORT, 1.0, 5.0, LINEAR, AWT));
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_FLOAT, 1.0, 5.0, LINEAR, AWT));
        assertEquals(TYPE_DOUBLE, getTargetDataType(TYPE_INT, 1.0, 5.0, LINEAR, AWT));
        assertEquals(TYPE_DOUBLE, getTargetDataType(TYPE_DOUBLE, 1.0, 5.0, LINEAR, AWT));

        // Test with non-LINEAR scaling type (EXPONENTIAL)
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_BYTE, 1.0, 0.0, EXPONENTIAL, AWT));
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_USHORT, 1.0, 0.0, EXPONENTIAL, AWT));
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_SHORT, 1.0, 0.0, EXPONENTIAL, AWT));
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_FLOAT, 1.0, 0.0, EXPONENTIAL, AWT));
        assertEquals(TYPE_DOUBLE, getTargetDataType(TYPE_INT, 1.0, 0.0, EXPONENTIAL, AWT));
        assertEquals(TYPE_DOUBLE, getTargetDataType(TYPE_DOUBLE, 1.0, 0.0, EXPONENTIAL, AWT));

        // Test with non-LINEAR scaling type (LOGARITHMIC)
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_BYTE, 1.0, 0.0, LOGARITHMIC, AWT));
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_USHORT, 1.0, 0.0, LOGARITHMIC, AWT));
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_SHORT, 1.0, 0.0, LOGARITHMIC, AWT));
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_FLOAT, 1.0, 0.0, LOGARITHMIC, AWT));
        assertEquals(TYPE_DOUBLE, getTargetDataType(TYPE_INT, 1.0, 0.0, LOGARITHMIC, AWT));
        assertEquals(TYPE_DOUBLE, getTargetDataType(TYPE_DOUBLE, 1.0, 0.0, LOGARITHMIC, AWT));

        // Test with INTERPRET_BYTE_SIGNED interpretation type
        assertEquals(TYPE_SHORT, getTargetDataType(TYPE_BYTE, 1.0, 0.0, LINEAR, INTERPRET_BYTE_SIGNED));
        assertEquals(TYPE_USHORT, getTargetDataType(TYPE_USHORT, 1.0, 0.0, LINEAR, INTERPRET_BYTE_SIGNED));
        assertEquals(TYPE_SHORT, getTargetDataType(TYPE_SHORT, 1.0, 0.0, LINEAR, INTERPRET_BYTE_SIGNED));
        assertEquals(TYPE_INT, getTargetDataType(TYPE_INT, 1.0, 0.0, LINEAR, INTERPRET_BYTE_SIGNED));

        // Test with INTERPRET_INT_UNSIGNED interpretation type
        assertEquals(TYPE_DOUBLE, getTargetDataType(TYPE_BYTE, 1.0, 0.0, LINEAR, INTERPRET_INT_UNSIGNED));
        // todo: why is TYPE_BYTE turned into TYPE_DOUBLE but TYPE_SHORT and TYPE_USHORT not?
        assertEquals(TYPE_USHORT, getTargetDataType(TYPE_USHORT, 1.0, 0.0, LINEAR, INTERPRET_INT_UNSIGNED));
        assertEquals(TYPE_SHORT, getTargetDataType(TYPE_SHORT, 1.0, 0.0, LINEAR, INTERPRET_INT_UNSIGNED));
        assertEquals(TYPE_DOUBLE, getTargetDataType(TYPE_INT, 1.0, 0.0, LINEAR, INTERPRET_INT_UNSIGNED));

        // Test with undefined source data type
        assertEquals(TYPE_UNDEFINED, getTargetDataType(TYPE_UNDEFINED, 2.0, 0.0, LINEAR, AWT));

        // Test edge cases - combination of rescaling and interpretation
        assertEquals(TYPE_FLOAT, getTargetDataType(TYPE_BYTE, 2.0, 0.0, LINEAR, INTERPRET_BYTE_SIGNED));
        assertEquals(TYPE_DOUBLE, getTargetDataType(TYPE_INT, 2.0, 0.0, LINEAR, INTERPRET_INT_UNSIGNED));
    }

}
