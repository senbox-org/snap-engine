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

import org.esa.snap.TestHelper;
import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ATT_NAME_ORIGINAL_RASTER_DATA_NODE_ORDER;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.cast;

public class ZarrProductWriterTest_collectOriginalRasterDataNodeOrder {

    private Product product;

    @Before
    public void setUp() throws Exception {
        product = new Product("testProduct", "type", 5, 7);
        product.addBand("There", "42");
        product.addMask("truly", Mask.RangeType.INSTANCE);
        addTiePointGrid("data");
        product.addBand("is", "112");
        product.addMask("correct", Mask.RangeType.INSTANCE);
        addTiePointGrid("node");
        product.addBand("only", "116117");
        product.addMask("raster", Mask.RangeType.INSTANCE);
        addTiePointGrid("order");
        product.addBand("one", "110");
    }

    @Test
    public void testThatProductAttributesContainAttribute_OriginalRasterDataNodeOrder() throws IllegalAccessException, ProductIOException {
        //preparation
        final ZarrProductWriter writer = new ZarrProductWriter(new ZarrProductWriterPlugIn());
        TestHelper.setPrivateFieldObject(writer, "_sourceProduct", product);

        //execution
        final Map<String, Object> attributes = writer.collectProductAttributes();

        //verification
        assertThat(attributes).containsKey(ZnapConstantsAndUtils.ATT_NAME_ORIGINAL_RASTER_DATA_NODE_ORDER);
        final Object value = attributes.get(ATT_NAME_ORIGINAL_RASTER_DATA_NODE_ORDER);
        assertThat(value).isNotNull().isInstanceOf(List.class);
        final List<String> list = cast(value);
        assertThat(list).containsExactly("There","is","only","one","truly","correct","raster","data","node","order");
    }

    private void addTiePointGrid(String name) {
        product.addTiePointGrid(new TiePointGrid(name, 2, 3, 1, 1, 2, 3));
    }
}