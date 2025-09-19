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

package org.esa.snap.core.datamodel;

import com.bc.ceres.jai.operator.ReinterpretDescriptor;
import com.bc.ceres.multilevel.MultiLevelModel;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 * @author Norman Fomferra
 */
public class RasterDataNodeTest {

    /**
     * Tests use of Product.getNumResolutionsMax in RasterDataNode.getMultiLevelMode
     */
    @Test
    public void testGetMultiLevelModel() throws Exception {
        MultiLevelModel mlm1, mlm2;
        final Product p = new Product("P", "T", 10960, 10960);

        final Band b1 = p.addBand("B1", "0"); // Virtual band image --> source image set
        final Band b2 = p.addBand("B2", ProductData.TYPE_FLOAT32); // Normal band image --> source image NOT set

        mlm1 = b1.getMultiLevelModel();
        mlm2 = b2.getMultiLevelModel();
        assertEquals(0, p.getNumResolutionsMax());
        assertEquals(7, mlm1.getLevelCount());
        assertEquals(7, mlm2.getLevelCount());

        p.setNumResolutionsMax(3);

        b1.getSourceImage();

        mlm1 = b1.getMultiLevelModel();
        mlm2 = b2.getMultiLevelModel();
        assertEquals(3, p.getNumResolutionsMax());
        assertEquals(3, mlm1.getLevelCount());
        assertEquals(3, mlm2.getLevelCount());
    }

    @Test
    public void testImageToModelTransformCannotDetermine() throws Exception {
        Band band = new Band("B", ProductData.TYPE_FLOAT32, 4, 2);
        assertEquals(new AffineTransform(), band.getImageToModelTransform());
    }

    @Test
    public void testImageToModelTransformSetterGetter() throws Exception {
        Band band = new Band("B", ProductData.TYPE_FLOAT32, 4, 2);
        band.setImageToModelTransform(AffineTransform.getScaleInstance(.6, .3));
        assertEquals(AffineTransform.getScaleInstance(.6, .3), band.getImageToModelTransform());
    }

    @Test
    public void testImageToModelTransformIsIdentity() throws Exception {
        Product product = new Product("N", "T", 4, 2);
        Band band = new Band("B", ProductData.TYPE_FLOAT32, 4, 2);
        product.addBand(band);
        assertEquals(new AffineTransform(), band.getImageToModelTransform());
    }

    @Test
    public void testImageToModelTransformIsNewInstance() throws Exception {
        Product product = new Product("N", "T", 4, 2);
        Band band = new Band("B", ProductData.TYPE_FLOAT32, 4, 2);
        product.addBand(band);
        AffineTransform scaleInstance = AffineTransform.getScaleInstance(2.0, 2.0);
        band.setImageToModelTransform(scaleInstance);
        assertEquals(scaleInstance, band.getImageToModelTransform());
        assertNotSame(scaleInstance, band.getImageToModelTransform());
        assertNotSame(band.getImageToModelTransform(), band.getImageToModelTransform());
        scaleInstance.rotate(0.1, 0.2);
        assertNotEquals(scaleInstance, band.getImageToModelTransform());
    }

    @Test(expected = IllegalStateException.class)
    public void testImageToModelTransformIsRuledBySourceImage() throws Exception {
        Band band = new Band("B", ProductData.TYPE_FLOAT32, 4, 2);
        band.setSourceImage(ConstantDescriptor.create(4f, 2f, new Float[]{0f}, null));
        assertEquals(new AffineTransform(), band.getImageToModelTransform());
        band.setImageToModelTransform(AffineTransform.getScaleInstance(.6, .3));
        assertEquals(new AffineTransform(), band.getImageToModelTransform());
    }

    @Test
    public void testGetPixelString_Byte() {
        final Product product = new Product("X", "Y", 2, 1);
        Band node = product.addBand("name", ProductData.TYPE_INT8);
        node.setSynthetic(true);
        node.setNoDataValue(0);
        node.setNoDataValueUsed(true);
        final byte[] data = new byte[product.getSceneRasterWidth() * product.getSceneRasterHeight()];
        Arrays.fill(data, (byte) 1);
        data[0] = 0; // no data
        node.setData(ProductData.createInstance(data));

        assertEquals("NaN", node.getPixelString(0, 0));
        assertEquals("1", node.getPixelString(1, 0));
    }

    @Test
    public void testGetPixelString() throws IOException {
        Locale.setDefault(Locale.UK);

        final Product product = new Product("X", "Y", 2, 1);
        Band b1 = product.addBand("b1", "X < 1 ? NaN : 1.0", ProductData.TYPE_FLOAT32);
        Band b2 = product.addBand("b2", "X < 1 ? NaN : 2.0", ProductData.TYPE_FLOAT64);
        Band b3 = product.addBand("b3", "X < 1 ? 0 : 3", ProductData.TYPE_UINT16);
        Band b4 = product.addBand("b4", "X < 1 ? 0 : 4", ProductData.TYPE_INT8);
        b1.loadRasterData();
        b2.loadRasterData();
        b3.loadRasterData();
        b4.loadRasterData();

        b3.setNoDataValue(0);
        b3.setNoDataValueUsed(true);

        b4.setNoDataValue(0);
        b4.setNoDataValueUsed(true);

        assertEquals("NaN", b1.getPixelString(0, 0));
        assertEquals("1.0", b1.getPixelString(1, 0));

        assertEquals("NaN", b2.getPixelString(0, 0));
        assertEquals("2.0", b2.getPixelString(1, 0));

        assertEquals("NaN", b3.getPixelString(0, 0));
        assertEquals("3", b3.getPixelString(1, 0));

        assertEquals("NaN", b4.getPixelString(0, 0));
        assertEquals("4", b4.getPixelString(1, 0));
    }

    @Test
    public void testGetGeophysicalDataType() {
        final Product product = new Product("TestProduct", "TestType", 10, 10);

        // Test with different data types without scaling
        Band int8Band = product.addBand("int8", ProductData.TYPE_INT8);
        Band int16Band = product.addBand("int16", ProductData.TYPE_INT16);
        Band int32Band = product.addBand("int32", ProductData.TYPE_INT32);
        Band uint8Band = product.addBand("uint8", ProductData.TYPE_UINT8);
        Band uint16Band = product.addBand("uint16", ProductData.TYPE_UINT16);
        Band uint32Band = product.addBand("uint32", ProductData.TYPE_UINT32);
        Band float32Band = product.addBand("float32", ProductData.TYPE_FLOAT32);
        Band float64Band = product.addBand("float64", ProductData.TYPE_FLOAT64);

        // Without scaling, geophysical data type should match original data type
        // todo: Maybe a bug, why is INT8 converted to INT16
        assertEquals(ProductData.TYPE_INT16, int8Band.getGeophysicalDataType());
        assertEquals(ProductData.TYPE_INT16, int16Band.getGeophysicalDataType());
        assertEquals(ProductData.TYPE_INT32, int32Band.getGeophysicalDataType());
        assertEquals(ProductData.TYPE_UINT8, uint8Band.getGeophysicalDataType());
        assertEquals(ProductData.TYPE_UINT16, uint16Band.getGeophysicalDataType());
        assertEquals(ProductData.TYPE_FLOAT64, uint32Band.getGeophysicalDataType());
        assertEquals(ProductData.TYPE_FLOAT32, float32Band.getGeophysicalDataType());
        assertEquals(ProductData.TYPE_FLOAT64, float64Band.getGeophysicalDataType());

        Band scaledInt8Band = product.addBand("scaledInt8", ProductData.TYPE_INT8);
        scaledInt8Band.setScalingFactor(0.0006);
        scaledInt8Band.setScalingOffset(-1.0);
        assertEquals(ProductData.TYPE_FLOAT32, scaledInt8Band.getGeophysicalDataType());

        Band scaledUInt8Band = product.addBand("scaledUInt8", ProductData.TYPE_UINT8);
        scaledUInt8Band.setScalingFactor(0.0006);
        scaledUInt8Band.setScalingOffset(-1.0);
        assertEquals(ProductData.TYPE_FLOAT32, scaledUInt8Band.getGeophysicalDataType());

        Band scaledInt16Band = product.addBand("scaledInt16", ProductData.TYPE_INT16);
        scaledInt16Band.setScalingFactor(0.0006);
        scaledInt16Band.setScalingOffset(-1.0);
        assertEquals(ProductData.TYPE_FLOAT32, scaledInt16Band.getGeophysicalDataType());

        // Test with log10 scaling
        Band scaledUInt16Band = product.addBand("logScaledUint16", ProductData.TYPE_UINT16);
        scaledUInt16Band.setLog10Scaled(true);
        assertEquals(ProductData.TYPE_FLOAT32, scaledUInt16Band.getGeophysicalDataType());

        Band scaledInt32 = product.addBand("scaledInt32", ProductData.TYPE_INT32);
        scaledInt32.setScalingFactor(0.0006);
        scaledInt32.setScalingOffset(-1.0);
        assertEquals(ProductData.TYPE_FLOAT64, scaledInt32.getGeophysicalDataType());

        Band scaledUint32 = product.addBand("scaledUint32", ProductData.TYPE_UINT32);
        scaledUint32.setScalingFactor(0.0006);
        scaledUint32.setScalingOffset(-1.0);
        assertEquals(ProductData.TYPE_FLOAT64, scaledUint32.getGeophysicalDataType());

        Band scaledFloat32Band = product.addBand("logScaledFloat32", ProductData.TYPE_FLOAT32);
        scaledFloat32Band.setLog10Scaled(true);
        assertEquals(ProductData.TYPE_FLOAT32, scaledFloat32Band.getGeophysicalDataType());

        Band scaledFloat64Band = product.addBand("logScaledFloat64", ProductData.TYPE_FLOAT64);
        scaledFloat64Band.setLog10Scaled(true);
        assertEquals(ProductData.TYPE_FLOAT64, scaledFloat64Band.getGeophysicalDataType());
    }

    @Test
    public void testGetInterpretationType() {
        final Product product = new Product("TestProduct", "TestType", 10, 10);

        Band int8Band = product.addBand("int8", ProductData.TYPE_INT8);
        Band int16Band = product.addBand("int16", ProductData.TYPE_INT16);
        Band int32Band = product.addBand("int32", ProductData.TYPE_INT32);
        Band uint8Band = product.addBand("uint8", ProductData.TYPE_UINT8);
        Band uint16Band = product.addBand("uint16", ProductData.TYPE_UINT16);
        Band uint32Band = product.addBand("uint32", ProductData.TYPE_UINT32);
        Band float32Band = product.addBand("float32", ProductData.TYPE_FLOAT32);
        Band float64Band = product.addBand("float64", ProductData.TYPE_FLOAT64);

        // todo: BYTE_SIGNED is AWT DataBuffer default for int8
        assertEquals(ReinterpretDescriptor.INTERPRET_BYTE_SIGNED, int8Band.getInterpretationType());
        // todo: should uint8 be INTERPRET_INT_UNSIGNED?
        assertEquals(ReinterpretDescriptor.AWT, uint8Band.getInterpretationType());
        assertEquals(ReinterpretDescriptor.AWT, int16Band.getInterpretationType());
        // todo: shouldn't unint16 be INTERPRET_INT_UNSIGNED
        assertEquals(ReinterpretDescriptor.AWT, uint16Band.getInterpretationType());
        assertEquals(ReinterpretDescriptor.AWT, int32Band.getInterpretationType());
        assertEquals(ReinterpretDescriptor.INTERPRET_INT_UNSIGNED, uint32Band.getInterpretationType());
        assertEquals(ReinterpretDescriptor.AWT, float32Band.getInterpretationType());
        assertEquals(ReinterpretDescriptor.AWT, float64Band.getInterpretationType());
    }
}
