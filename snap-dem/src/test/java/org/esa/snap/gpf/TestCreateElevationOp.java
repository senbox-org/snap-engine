/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.dem.gpf.AddElevationOp;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Unit test for CreateElevationOp.
 */
public class TestCreateElevationOp {

    private final static File inputASAR_WSM = new File(TestUtils.TEST_SAR_UNITTESTS +
            "ASAR/subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim");

    private final static OperatorSpi spi = new AddElevationOp.Spi();

    private final static double[] expectedValues = {
            1526.6146240234375,
            1521.0814208984375,
            1532.55810546875,
            1552.160888671875,
            1524.3275146484375,
            1518.3543701171875,
            1532.048095703125,
            1555.7523193359375
    };

    @Before
    public void setup() throws Exception {
        try {
            assumeTrue(inputASAR_WSM + " not found.", inputASAR_WSM.exists());
        } catch (Exception e) {
            TestUtils.skipTest(this, e.getMessage());
            throw e;
        }
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessing() throws Exception {
        final Product sourceProduct = TestUtils.readSourceProduct(inputASAR_WSM);

        final AddElevationOp op = (AddElevationOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        op.doExecute(ProgressMonitor.NULL);
        TestUtils.verifyProduct(targetProduct, true, true, true);

        final Band elevBand = targetProduct.getBand("elevation");
        assertNotNull(elevBand);
        assertEquals(-32768.0, elevBand.getNoDataValue(), 1e-8);

        final double[] demValues = new double[8];
        elevBand.readPixels(0, 0, 4, 2, demValues, ProgressMonitor.NULL);

        assertArrayEquals(expectedValues, demValues, 0.0);

        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        //TestUtils.attributeEquals(abs, AbstractMetadata.DEM, "SRTM");
    }
}
