package org.esa.s3tbx.olci.radiometry.smilecorr;

import org.esa.s3tbx.olci.radiometry.gaseousabsorption.GasToCompute;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author muhammad.bc.
 */
public class GasToComputeTest {
    @Test
    public void testCheckTheBandsToCompute() throws Exception {

        assertArrayEquals(new String[]{"NO2"}, GasToCompute.valueOf("Oa01_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"NO2"}, GasToCompute.valueOf("Oa02_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"NO2", "H2O", "O3"}, GasToCompute.valueOf("Oa03_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"O3"}, GasToCompute.valueOf("Oa04_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O", "O3"}, GasToCompute.valueOf("Oa05_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"O3"}, GasToCompute.valueOf("Oa06_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"O3"}, GasToCompute.valueOf("Oa07_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O", "O3"}, GasToCompute.valueOf("Oa08_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"O3"}, GasToCompute.valueOf("Oa09_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O", "O3"}, GasToCompute.valueOf("Oa10_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O", "O3"}, GasToCompute.valueOf("Oa11_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"O3"}, GasToCompute.valueOf("Oa12_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"O2", "O3"}, GasToCompute.valueOf("Oa13_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"O2", "O3"}, GasToCompute.valueOf("Oa14_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"O2", "O3"}, GasToCompute.valueOf("Oa15_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"O2", "O3", "H2O"}, GasToCompute.valueOf("Oa16_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O"}, GasToCompute.valueOf("Oa17_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O"}, GasToCompute.valueOf("Oa18_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O"}, GasToCompute.valueOf("Oa19_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O"}, GasToCompute.valueOf("Oa20_radiance").getGasBandToCompute());
        assertArrayEquals(new String[]{"H2O"}, GasToCompute.valueOf("Oa21_radiance").getGasBandToCompute());

    }
}