/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.operator;

import org.esa.s3tbx.olci.radiometry.gaseousabsorption.GaseousAbsorptionOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author muhammad.bc.
 */
public class GaseousAbsorptionOpTest {
    final GaseousAbsorptionOp.Spi operatorSpi = new GaseousAbsorptionOp.Spi();

    @Before
    public void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi);
    }

    @After
    public void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi);
    }

    @Test
    public void testGaseousOp() throws Exception {
        final URL resource = GaseousAbsorptionOpTest.class.getResource("test_product.dim");
        final Product sourceProduct = ProductIO.readProduct(resource.getPath());
        assertNotNull(sourceProduct);

        final HashMap<String, Object> parameters = new HashMap<>();
        final Product gaseousAbsorptionProduct = GPF.createProduct("OLCI.GaseousAsorption", parameters, sourceProduct);
        assertNotNull(gaseousAbsorptionProduct);

        float sampleFloatSourceProduct = sourceProduct.getBandAt(0).getSampleFloat(0, 0);
        float sampleFloatGas = gaseousAbsorptionProduct.getBandAt(0).getSampleFloat(0, 0);

        assertEquals(413.5063171386719, sampleFloatSourceProduct, 1e-8);
        assertEquals(1.3628956, sampleFloatGas, 1e-8);
    }
}