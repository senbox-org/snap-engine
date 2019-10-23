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
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.dem.gpf.AddElevationOp;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Unit test for CreateElevationOp.
 */
public class TestCreateElevationOp {

    private static final String PROPERTY_NAME_S1_DATA_DIR = "s1tbx.tests.data.dir";
    private final static String sep = File.separator;
    private final static String input = System.getProperty(PROPERTY_NAME_S1_DATA_DIR,"/data/ssd/testData/s1tbx/");
    private final static String inputSAR = input + sep + "SAR" + sep;
    private final static File inputASAR_WSM = new File(inputSAR + "ASAR" + sep + "subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim");

    private final static OperatorSpi spi = new AddElevationOp.Spi();

    private static double[] expectedValues = {
            1528.408203125,
            1525.0687255859375,
            1524.87841796875,
            1541.1314697265625,
            1527.7576904296875,
            1521.7149658203125,
            1521.7635498046875,
            1542.481201171875
    };

    private File inputFile;

    @Before
    public void setup() {
        inputFile =  inputASAR_WSM;
        assumeTrue(inputFile.exists());
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessing() throws Exception {
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final AddElevationOp op = (AddElevationOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);

        final Band elevBand = targetProduct.getBand("elevation");
        assertNotNull(elevBand);
        assertEquals(-32768.0, elevBand.getNoDataValue(), 1e-8);

        final double[] demValues = new double[8];
        elevBand.readPixels(0, 0, 4, 2, demValues, ProgressMonitor.NULL);

        assertTrue(Arrays.equals(expectedValues, demValues));

        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        //TestUtils.attributeEquals(abs, AbstractMetadata.DEM, "SRTM");
    }
}
