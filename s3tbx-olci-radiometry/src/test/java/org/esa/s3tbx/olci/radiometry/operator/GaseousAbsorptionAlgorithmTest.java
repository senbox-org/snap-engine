package org.esa.s3tbx.olci.radiometry.operator;


import org.esa.s3tbx.olci.radiometry.smilecorr.GaseousAbsorptionAlgorithm;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;


/**
 * @author muhammad.bc.
 */
public class GaseousAbsorptionAlgorithmTest {

//    @Test
//    public void testCalExponential() {
//        GaseousAbsorptionAlgorithm gaseousAbsorptionAlgorithm = new GaseousAbsorptionAlgorithm();
//        assertEquals(3.3546262790251185E-4, gaseousAbsorptionAlgorithm.getTransmissionGas(2, 2, 2), 1e-8);
//        assertEquals(2.718281828459045, gaseousAbsorptionAlgorithm.getTransmissionGas(1, -1, 1), 1e-8);
//        assertEquals(1.0, gaseousAbsorptionAlgorithm.getTransmissionGas(2, 2, 0), 1e-8);
//        assertEquals(1.0, gaseousAbsorptionAlgorithm.getTransmissionGas(0, -2, 0), 1e-8);
//        assertEquals(2.572813378588326, gaseousAbsorptionAlgorithm.getTransmissionGas(0.9, -1, 1.05), 1e-8);
//
//    }

    @Test
    public void testGetGasToComputeForABand() throws Exception {
        GaseousAbsorptionAlgorithm gaseousAbsorptionAlgorithm = new GaseousAbsorptionAlgorithm();
        assertArrayEquals(new String[]{"NO2"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa01_radiance"));
        assertArrayEquals(new String[]{"NO2"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa02_radiance"));
        assertArrayEquals(new String[]{"NO2", "H2O", "O3"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa03_radiance"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa04_radiance"));
        assertArrayEquals(new String[]{"H2O", "O3"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa05_radiance"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa06_radiance"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa07_radiance"));
        assertArrayEquals(new String[]{"H2O", "O3"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa08_radiance"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa09_radiance"));
        assertArrayEquals(new String[]{"H2O", "O3"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa10_radiance"));
        assertArrayEquals(new String[]{"H2O", "O3"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa11_radiance"));
        assertArrayEquals(new String[]{"O3"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa12_radiance"));
        assertArrayEquals(new String[]{"O2", "O3"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa13_radiance"));
        assertArrayEquals(new String[]{"O2", "O3"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa14_radiance"));
        assertArrayEquals(new String[]{"O2", "O3"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa15_radiance"));
        assertArrayEquals(new String[]{"O2", "O3", "H2O"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa16_radiance"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa17_radiance"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa18_radiance"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa19_radiance"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa20_radiance"));
        assertArrayEquals(new String[]{"H2O"}, gaseousAbsorptionAlgorithm.gasToComputeForBand("Oa21_radiance"));

    }
}