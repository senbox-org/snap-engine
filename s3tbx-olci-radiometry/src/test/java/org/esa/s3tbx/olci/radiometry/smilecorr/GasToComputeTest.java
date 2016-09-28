package org.esa.s3tbx.olci.radiometry.smilecorr;

import org.esa.s3tbx.olci.radiometry.gasabsorption.GasToCompute;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author muhammad.bc.
 */
public class GasToComputeTest {
    @Test
    public void testCheckTheBandsToCompute() throws Exception {

        assertArrayEquals(new String[]{"NO2"}, GasToCompute.valueOf("gaseous_absorp_01").getGasBandToCompute());
        assertArrayEquals(new String[]{"NO2"}, GasToCompute.valueOf("gaseous_absorp_02").getGasBandToCompute());
        assertArrayEquals(new String[]{"NO2", "H2O", "O3"}, GasToCompute.valueOf("gaseous_absorp_03").getGasBandToCompute());
        assertArrayEquals(new String[]{"O3"}, GasToCompute.valueOf("gaseous_absorp_04").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O", "O3"}, GasToCompute.valueOf("gaseous_absorp_05").getGasBandToCompute());
        assertArrayEquals(new String[]{"O3"}, GasToCompute.valueOf("gaseous_absorp_06").getGasBandToCompute());
        assertArrayEquals(new String[]{"O3"}, GasToCompute.valueOf("gaseous_absorp_07").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O", "O3"}, GasToCompute.valueOf("gaseous_absorp_08").getGasBandToCompute());
        assertArrayEquals(new String[]{"O3"}, GasToCompute.valueOf("gaseous_absorp_09").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O", "O3"}, GasToCompute.valueOf("gaseous_absorp_10").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O", "O3"}, GasToCompute.valueOf("gaseous_absorp_11").getGasBandToCompute());
        assertArrayEquals(new String[]{"O3"}, GasToCompute.valueOf("gaseous_absorp_12").getGasBandToCompute());
        assertArrayEquals(new String[]{"O2", "O3"}, GasToCompute.valueOf("gaseous_absorp_13").getGasBandToCompute());
        assertArrayEquals(new String[]{"O2", "O3"}, GasToCompute.valueOf("gaseous_absorp_14").getGasBandToCompute());
        assertArrayEquals(new String[]{"O2", "O3"}, GasToCompute.valueOf("gaseous_absorp_15").getGasBandToCompute());
        assertArrayEquals(new String[]{"O2", "O3", "H2O"}, GasToCompute.valueOf("gaseous_absorp_16").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O"}, GasToCompute.valueOf("gaseous_absorp_17").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O"}, GasToCompute.valueOf("gaseous_absorp_18").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O"}, GasToCompute.valueOf("gaseous_absorp_19").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O"}, GasToCompute.valueOf("gaseous_absorp_20").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O"}, GasToCompute.valueOf("gaseous_absorp_21").getGasBandToCompute());

    }
}