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

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class BowtiePixelGeoCodingTest {

    @Test
    public void testTransferGeoCoding() throws URISyntaxException, IOException {
        Product product = ProductIO.readProduct(new File(getClass().getResource("bowtiepixelgeocoding_test_product.L2_sub").toURI()));
        assertTrue(product.getSceneGeoCoding() instanceof BowtiePixelGeoCoding);

        Product targetProduct = new Product("name", "type", product.getSceneRasterWidth(), product.getSceneRasterHeight());

        assertNull(targetProduct.getSceneGeoCoding());
        ProductUtils.copyGeoCoding(product, targetProduct);

        assertNotNull(targetProduct.getSceneGeoCoding());
        assertTrue(targetProduct.getSceneGeoCoding() instanceof BowtiePixelGeoCoding);
    }


    @Test
    public void testLatAndLonAreCorrectlySubsetted() throws URISyntaxException, IOException {
        Product product = ProductIO.readProduct(new File(getClass().getResource("bowtiepixelgeocoding_test_product.L2_sub").toURI()));
        GeoCoding sourcceGeoCoding = product.getSceneGeoCoding();
        assertTrue(sourcceGeoCoding instanceof BowtiePixelGeoCoding);

        ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setRegion(50, 50, 10, 10);
        subsetDef.addNodeName("chlor_a");
        Product targetProduct = product.createSubset(subsetDef, "subset", "");

        GeoCoding targetGeoCoding = targetProduct.getSceneGeoCoding();
        assertNotNull(targetGeoCoding);
        assertTrue(targetGeoCoding instanceof BowtiePixelGeoCoding);
        assertTrue(targetProduct.containsBand("latitude"));
        assertTrue(targetProduct.containsBand("longitude"));

        PixelPos sourcePixelPos = new PixelPos(50.5, 50.5);
        GeoPos expected = sourcceGeoCoding.getGeoPos(sourcePixelPos, new GeoPos());
        PixelPos targetPixelPos = new PixelPos(0.5, 0.5);
        GeoPos actual = targetGeoCoding.getGeoPos(targetPixelPos, new GeoPos());
        assertEquals(expected.getLat(), actual.getLat(), 1.0e-6);
        assertEquals(expected.getLon(), actual.getLon(), 1.0e-6);

    }
}
