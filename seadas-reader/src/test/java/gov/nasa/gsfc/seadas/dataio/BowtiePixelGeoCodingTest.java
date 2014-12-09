/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package gov.nasa.gsfc.seadas.dataio;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.ProductUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class BowtiePixelGeoCodingTest {

    @Test
    public void testTransferGeoCoding() throws URISyntaxException, IOException {
        Product product = ProductIO.readProduct(new File(getClass().getResource("bowtiepixelgeocoding_test_product.L2_sub").toURI()));
        assertTrue(product.getGeoCoding() instanceof BowtiePixelGeoCoding);

        Product targetProduct = new Product("name", "type", product.getSceneRasterWidth(), product.getSceneRasterHeight());

        assertNull(targetProduct.getGeoCoding());
        ProductUtils.copyGeoCoding(product, targetProduct);

        assertNotNull(targetProduct.getGeoCoding());
        assertTrue(targetProduct.getGeoCoding() instanceof BowtiePixelGeoCoding);
    }
}
