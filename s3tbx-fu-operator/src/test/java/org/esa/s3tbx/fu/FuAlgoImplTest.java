package org.esa.s3tbx.fu;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FuAlgoImplTest {

    private final double[] xFactor = new double[]{2.957, 10.861, 3.744, 3.750, 34.687, 41.853, 7.619, 0.844, 0.189};
    private final double[] yFactor = new double[]{0.112, 1.711, 5.672, 23.263, 48.791, 23.949, 2.944, 0.307, 0.068};
    private final double[] zFactor = new double[]{14.354, 58.356, 28.227, 4.022, 0.618, 0.026, 0.000, 0.000, 0.000};
    private final double[] polyHue = new double[]{-12.0506, 88.9325, -244.6960, 305.2361, -164.6960, 28.5255};

    private FuAlgoImpl fuAlgoImpl;

    @Before
    public void setUp() throws Exception {
        fuAlgoImpl = new FuAlgoImpl();
        fuAlgoImpl.setX3Factors(xFactor);
        fuAlgoImpl.setY3Factors(yFactor);
        fuAlgoImpl.setZ3Factors(zFactor);
        fuAlgoImpl.setPolyCoeffs(polyHue);
    }

    @Test
    public void testGetTristimulusValue() throws Exception {
        final double[] spectrum = new double[]{0.222, 0.219, 0.277, 0.219, 0.235, 0.260, 0.281, 0.287, 0.273};
        assertEquals(26.36134, fuAlgoImpl.getTristimulusValue(spectrum, xFactor), 1e-8);
        assertEquals(25.691876, fuAlgoImpl.getTristimulusValue(spectrum, yFactor), 1e-8);
        assertEquals(24.818239000000005, fuAlgoImpl.getTristimulusValue(spectrum, zFactor), 1e-8);
    }


    //Simulates sample found in Sample_From_Hans.xlsx
    @Test
    public void testTristimulusHueAndFUForMERIS() throws Exception {
        final double[] constPolyHue = {-12.0506, 88.9325, -244.6960, 305.2361, -164.6960, 28.5255};
        final double[] spectrum = new double[]{0.00798, 0.01494, 0.027, 0.03116, 0.04357, 0.0281, 0.01942, 0.01863, 0.01472};

        final double tristimulusValueX = fuAlgoImpl.getTristimulusValue(spectrum, xFactor);
        final double tristimulusValueY = fuAlgoImpl.getTristimulusValue(spectrum, yFactor);
        final double tristimulusValueZ = fuAlgoImpl.getTristimulusValue(spectrum, zFactor);

        assertEquals(3.25764687, tristimulusValueX, 1e-8);
        assertEquals(3.7671588, tristimulusValueY, 1e-8);
        assertEquals(1.90149494, tristimulusValueZ, 1e-8);


        final double denominator = tristimulusValueX + tristimulusValueY + tristimulusValueZ;
        final double chrX = tristimulusValueX / denominator;
        final double chrY = tristimulusValueY / denominator;

        assertEquals(0.36494926760034363, chrX, 1e-8);
        assertEquals(0.4220291209753466, chrY, 1e-8);


        final double hue = fuAlgoImpl.getHue(chrX, chrY);
        final double hue100 = (hue / 100);

        assertEquals(70.38108395830288, hue, 1e-8);
        assertEquals(0.7038108395830288, hue100, 1e-8);


        double polyHue = fuAlgoImpl.getPolyCorr(hue100, constPolyHue);
        double hueMerisPCorr = hue + polyHue;
        int fuValue = FuAlgoImpl.getFuValue(hueMerisPCorr);

        assertEquals(-1.759288220916794, polyHue, 1e-8);
        assertEquals(68.6217957373861, hueMerisPCorr, 1e-8);
        assertEquals(11, fuValue);
    }

    @Test
    public void testGetFumeValue() {
        assertEquals(0, FuAlgoImpl.getFuValue(233));
        assertEquals(1, FuAlgoImpl.getFuValue(232));
        assertEquals(2, FuAlgoImpl.getFuValue(227));
        assertEquals(3, FuAlgoImpl.getFuValue(210.4));
        assertEquals(4, FuAlgoImpl.getFuValue(200.89));
        assertEquals(5, FuAlgoImpl.getFuValue(180.8));
        assertEquals(6, FuAlgoImpl.getFuValue(150));
        assertEquals(7, FuAlgoImpl.getFuValue(131));
        assertEquals(8, FuAlgoImpl.getFuValue(105));
        assertEquals(9, FuAlgoImpl.getFuValue(93.9));
        assertEquals(10, FuAlgoImpl.getFuValue(80.9));
        assertEquals(11, FuAlgoImpl.getFuValue(73.4));
        assertEquals(12, FuAlgoImpl.getFuValue(62.89));
        assertEquals(13, FuAlgoImpl.getFuValue(56.8));
        assertEquals(14, FuAlgoImpl.getFuValue(51));
        assertEquals(15, FuAlgoImpl.getFuValue(46.89));
        assertEquals(16, FuAlgoImpl.getFuValue(39.90));
        assertEquals(17, FuAlgoImpl.getFuValue(35.09));
        assertEquals(18, FuAlgoImpl.getFuValue(30.987));
        assertEquals(19, FuAlgoImpl.getFuValue(27.987));
        assertEquals(20, FuAlgoImpl.getFuValue(23.987));
        assertEquals(21, FuAlgoImpl.getFuValue(20.987));
        assertEquals(21, FuAlgoImpl.getFuValue(10));
        assertEquals(21, FuAlgoImpl.getFuValue(20));
    }

    @Test
    public void testGetHueValue() throws Exception {
        assertEquals(225.0, fuAlgoImpl.getHue(0.0, 0.0), 1e-8);
        assertEquals(50.52753979724931, fuAlgoImpl.getHue(5, 6), 1e-8);
        assertEquals(37.4570178393783, fuAlgoImpl.getHue(40.1, 30.8), 1e-8);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckNumberOfBands() throws Exception {
        fuAlgoImpl.getTristimulusValue(new double[4], xFactor);
    }


    @Test
    public void testSummarizationPolyHueMeris() throws Exception {
        final double[] constPolyHueForMeris = new double[]{-12.0506, 88.9325, -244.6960, 305.2361, -164.6960, 28.5255};
        assertEquals(1.2515000000000285, fuAlgoImpl.getPolyCorr(1.0, constPolyHueForMeris), 1e-8);
        assertEquals(-1.7544999999999717, fuAlgoImpl.getPolyCorr(2.5, constPolyHueForMeris), 1e-8);
    }
}