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

package org.esa.s3tbx.idepix.algorithms.landsat8;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.*;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class Landsat8ClassificationOpTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetHistogramBinAt3PercentOfMaximum() {
        // todo: continue
        Band b = createTestBand(ProductData.TYPE_FLOAT32, 100, 100);
        final Stx stx = new StxFactory().create(b, ProgressMonitor.NULL);
        final double at3PercentOfMaximum = Landsat8Utils.getHistogramBinAtNPercentOfMaximum(stx, 3.0);
        System.out.println("at3PercentOfMaximum = " + at3PercentOfMaximum);
//        assertEquals(6.0, at3PercentOfMaximum);
    }

    private Band createTestBand(int type, int w, int h) {
        final double mean = (w * h - 1.0) / 2.0;
        return createTestBand(type, w, h, mean);
    }

    private Band createTestBand(int type, int w, int h, double offset) {
        final Product product = createTestProduct(w, h);
        final Band band = new VirtualBand("V", type, w, h, "(Y-0.5) * " + w + " + (X-0.5) - " + offset);
        product.addBand(band);
        return band;
    }

    private Product createTestProduct(int w, int h) {
        return new Product("F", "F", w, h);
    }

}
