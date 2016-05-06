package org.esa.s3tbx.olci.radiometry.smilecorr;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author muhammad.bc.
 */
public class GasToComputeTest {
    @Test
    public void testCheckTheBandsToCompute() throws Exception {
        GasToCompute bandToCompute = GasToCompute.valueOf("Oa01_radiance");
        String[] gasToCompute = bandToCompute.getGasBandToCompute();
        assertArrayEquals(new String[]{"NO2"}, gasToCompute);
    }
}