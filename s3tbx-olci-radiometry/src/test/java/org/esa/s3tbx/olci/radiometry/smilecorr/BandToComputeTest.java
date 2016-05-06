package org.esa.s3tbx.olci.radiometry.smilecorr;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author muhammad.bc.
 */
public class BandToComputeTest {
    @Test
    public void testCheckTheBandsToCompute() throws Exception {
        BandToCompute bandToCompute = BandToCompute.valueOf("Oa01_radiance");
        String[] gasToCompute = bandToCompute.getGasBandToCompute();
        assertArrayEquals(new String[]{"H2O", "NO3","O3"}, gasToCompute);
    }
}