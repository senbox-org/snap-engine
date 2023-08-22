/*
 * Copyright (C) 2021 SkyWatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.snap.landcover;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.esa.snap.landcover.gpf.AddLandCoverOp;
import com.bc.ceres.test.LongTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Unit test for AddLandCoverOp.
 */
@RunWith(LongTestRunner.class)
public class TestAddLandCoverOp {

    private final static OperatorSpi spi = new AddLandCoverOp.Spi();

    @Test
    public void testGLC2000() throws Exception {

        final double[] expectedValues = {
                14.0, 21.0, 4.0, 4.0, 14.0, 14.0, 4.0, 14.0
        };

        final LandCoverDataset dataset = new LandCoverDataset("GLC2000",
                "land_cover_GLC2000", 23.0, expectedValues);

        process(dataset);
    }

    private void process(final LandCoverDataset dataset) throws Exception {
        final Product srcProduct = TestUtils.createProduct("test", 10, 10);

        final AddLandCoverOp op = (AddLandCoverOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(srcProduct);
        op.setParameter("landCoverNames", new String[] {dataset.name});

        // get targetProduct: execute initialize()
        final Product trgProduct = op.getTargetProduct();
        op.doExecute(ProgressMonitor.NULL);
        TestUtils.verifyProduct(trgProduct, true, true, true);

        final Band landcoverBand = trgProduct.getBand(dataset.bandName);
        assertNotNull(landcoverBand);
        assertEquals("nodatavalue", dataset.nodatavalue, landcoverBand.getNoDataValue(), 1e-8);

        final double[] values = new double[8];
        landcoverBand.readPixels(0, 0, 4, 2, values, ProgressMonitor.NULL);

        assertArrayEquals("pixels", dataset.expected, values, 0.0);
    }

    private static class LandCoverDataset {
        String name;
        String bandName;
        double nodatavalue;
        double[] expected;

        public LandCoverDataset(final String name, final String bandName,
                                final double nodatavalue, final double[] expected) {
            this.name = name;
            this.bandName = bandName;
            this.nodatavalue = nodatavalue;
            this.expected = expected;
        }
    }
}
