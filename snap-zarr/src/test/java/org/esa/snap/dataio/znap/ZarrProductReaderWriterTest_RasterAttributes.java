/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 */

package org.esa.snap.dataio.znap;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class ZarrProductReaderWriterTest_RasterAttributes {

    private Band source;
    private Band target;
    private HashMap<String, Object> attributes;
    private ZarrProductWriter writer;
    private ZarrProductReader reader;

    @Before
    public void setUp() throws Exception {
        source = new Band("band", ProductData.TYPE_FLOAT64, 10, 10);
        target = new Band("band", ProductData.TYPE_FLOAT64, 10, 10);
        attributes = new HashMap<>();

        final Product sourceProduct = new Product("name", "type");
        sourceProduct.addBand(source);
        sourceProduct.addBand("ancil", "4");
        final Product targetProduct = new Product("name", "type");
        targetProduct.addBand(target);
        targetProduct.addBand("ancil", "5");
        writer = new ZarrProductWriter(new ZarrProductWriterPlugIn());
        reader = new ZarrProductReader(new ZarrProductReaderPlugIn());
    }

    @Test
    public void rasterDescription() {
        // preparation
        source.setDescription("some extended description");
        assertThat(target.getDescription()).isNull();
        // execution
        transferToTarget();
        // verification
        assertThat(attributes.size()).isEqualTo(1);
        assertThat(attributes.keySet().iterator().next()).isEqualTo("long_name");
        assertThat(target.getDescription()).isEqualTo("some extended description");
    }

    @Test
    public void unit() {
        //preparation
        source.setUnit("An example unit");
        assertThat(target.getUnit()).isNull();
        //execution
        transferToTarget();
        //verification
        assertThat(attributes.size()).isEqualTo(1);
        assertThat(attributes.keySet().iterator().next()).isEqualTo("units");
        assertThat(target.getUnit()).isEqualTo("An example unit");
    }

    @Test
    public void unsignedAttribute() {
        //preparation
        source = new Band("unsigned band", ProductData.TYPE_UINT8, 10, 10);
        //execution
        transferToTarget();
        //verification
        assertThat(attributes.size()).isEqualTo(1);
        assertThat(attributes.keySet().iterator().next()).isEqualTo("_Unsigned");
    }

    @Test
    public void validPixelExpression() {
        //preparation
        source.setValidPixelExpression("example expression");
        assertThat(target.getValidPixelExpression()).isNull();
        //execution
        transferToTarget();
        //verification
        assertThat(attributes.size()).isEqualTo(1);
        assertThat(attributes.keySet().iterator().next()).isEqualTo("valid_pixel_expression");
        assertThat(target.getValidPixelExpression()).isEqualTo("example expression");
    }

    @Test
    public void scalingFactor() {
        //preparation
        source.setScalingFactor(321.3);
        assertThat(target.getScalingFactor()).isEqualTo(1.0);
        //execution
        transferToTarget();
        //verification
        assertThat(attributes.size()).isEqualTo(1);
        assertThat(attributes.keySet().iterator().next()).isEqualTo("scale_factor");
        assertThat(target.getScalingFactor()).isEqualTo(321.3);
    }

    @Test
    public void scalingOffset() {
        //preparation
        source.setScalingOffset(221.3);
        assertThat(target.getScalingOffset()).isEqualTo(0.0);
        //execution
        transferToTarget();
        //verification
        assertThat(attributes.size()).isEqualTo(1);
        assertThat(attributes.keySet().iterator().next()).isEqualTo("add_offset");
        assertThat(target.getScalingOffset()).isEqualTo(221.3);
    }

    @Test
    public void noDataValueIfLog10ScaledIsSet() {
        //preparation
        source.setLog10Scaled(true);
        source.setNoDataValue(5.0);
        assertThat(target.getNoDataValue()).isEqualTo(0.0);
        //execution
        transferToTarget();
        //verification
        assertThat(attributes.size()).isEqualTo(1);
        assertThat(attributes.keySet().iterator().next()).isEqualTo("_FillValue");
        assertThat(target.getNoDataValue()).isEqualTo(1.0E5);
    }

    @Test
    public void noDataValueInCaseOfUnsignedIntegerData() {
        // in this case standard source and target can not be used
        source = new Band("u int", ProductData.TYPE_UINT8, 10,10);
        target = new Band("u int", ProductData.TYPE_UINT8, 10,10);

        //preparation
        source.setNoDataValue(232);  // biger than Byte.MAX_VALUE (127) but not in case of unsigned Byte
        assertThat(target.getNoDataValue()).isEqualTo(0.0);
        //execution
        transferToTarget();
        //verification
        assertThat(attributes.size()).isEqualTo(2);
        assertThat(attributes.keySet().toArray()).isEqualTo(new String[]{"_FillValue", "_Unsigned"});
        assertThat(target.getNoDataValue()).isEqualTo(232.0);
    }

    @Test
    public void noDataValueInCaseOfSignedIntegerData() {
        // in this case standard source and target can not be used
        source = new Band("u int", ProductData.TYPE_INT8, 10,10);
        target = new Band("u int", ProductData.TYPE_INT8, 10,10);

        //preparation
        source.setNoDataValue(-24);
        assertThat(target.getNoDataValue()).isEqualTo(0.0);
        //execution
        transferToTarget();
        //verification
        assertThat(attributes.size()).isEqualTo(1);
        assertThat(attributes.keySet().iterator().next()).isEqualTo("_FillValue");
        assertThat(target.getNoDataValue()).isEqualTo(-24.0);
    }

    @Test
    public void noDataValueInCaseOfFloat32Data() {
        // in this case standard source and target can not be used
        source = new Band("u int", ProductData.TYPE_FLOAT32, 10,10);
        target = new Band("u int", ProductData.TYPE_FLOAT32, 10,10);

        //preparation
        source.setNoDataValue(1256.523);
        assertThat(target.getNoDataValue()).isEqualTo(0.0);
        //execution
        transferToTarget();
        //verification
        assertThat(attributes.size()).isEqualTo(1);
        assertThat(attributes.keySet().iterator().next()).isEqualTo("_FillValue");
        assertThat(attributes.get("_FillValue")).isEqualTo(1256.523F);
        assertThat(((Double)target.getNoDataValue()).floatValue()).isEqualTo(1256.523F);
    }

    @Test
    public void noDataValue() {
        //preparation
        source.setNoDataValue(893756.7899);
        assertThat(target.getNoDataValue()).isEqualTo(0.0);
        //execution
        transferToTarget();
        //verification
        assertThat(attributes.size()).isEqualTo(1);
        assertThat(attributes.keySet().iterator().next()).isEqualTo("_FillValue");
        assertThat(target.getNoDataValue()).isEqualTo(893756.7899);
    }

    @Test
    public void noDataValueUsed() {
        //preparation
        source.setNoDataValueUsed(true);
        assertThat(target.isNoDataValueUsed()).isEqualTo(false);
        //execution
        transferToTarget();
        //verification
        assertThat(attributes.size()).isEqualTo(1);
        assertThat(attributes.keySet().iterator().next()).isEqualTo("no_data_value_used");
        assertThat(target.isNoDataValueUsed()).isTrue();
    }

    @Test
    public void transferAncillaryRelations() {
        //preparation
        source.setAncillaryRelations("abc", "def");
        assertThat(target.getAncillaryRelations()).isEmpty();
        //execution
        transferToTarget();
        //verification
        assertThat(target.getAncillaryRelations()).containsExactly("abc", "def");
    }

    @Test
    public void transferAnc() {
        //preparation
        final Band ancil = source.getProduct().getBand("ancil");
        source.addAncillaryVariable(ancil);
        assertThat(source.getAncillaryVariables()).containsExactly(ancil);
        assertThat(source.getAncillaryRelations()).isEmpty();
        assertThat(target.getAncillaryVariables()).isEmpty();
        assertThat(target.getAncillaryRelations()).isEmpty();
        //execution
        transferToTarget();
        //verification
        assertThat(target.getAncillaryVariables())
                .isNotEmpty()
                .containsExactly(target.getProduct().getBand("ancil"));
        assertThat(target.getAncillaryRelations()).isEmpty();
    }

    private void transferToTarget() {
        writer.collectRasterAttributes(source, attributes);
        reader.applyRasterAttributes(attributes, target);
    }
}