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

package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrectionOpIITest {
    RayleighCorrectionOpII rayleighCorrectionOpII = new RayleighCorrectionOpII();

    @Test
    public void testGetBandIndex() throws Exception {
        assertEquals(8, rayleighCorrectionOpII.getSourceBandIndex("band_08"));
        assertEquals(-1, rayleighCorrectionOpII.getSourceBandIndex("band"));
        assertEquals(9, rayleighCorrectionOpII.getSourceBandIndex("09band"));
        assertEquals(5, rayleighCorrectionOpII.getSourceBandIndex("Bla05band"));
    }


    @Test
    public void testWaterVapor() throws Exception {
        Product product = new Product("dummy", "dummy");
        Band b1 = createBand("radiance_1", 1);
        Band b2 = createBand("radiance_2", 2);
        Band b3 = createBand("radiance_3", 3);
        Band b4 = createBand("radiance_4", 4);

        product.addBand(b1);
        product.addBand(b2);
        product.addBand(b3);
        product.addBand(b4);

        double[] allWavelengths = rayleighCorrectionOpII.getAllWavelengths(product, 3, "radiance_%d");
        assertArrayEquals(new double[]{1, 2, 3}, allWavelengths, 1e-8);

    }

    private Band createBand(String bandName, float waveLength) {
        Band b1 = new Band(bandName, ProductData.TYPE_INT8, 1, 1);
        b1.setSpectralWavelength(waveLength);
        return b1;
    }
}