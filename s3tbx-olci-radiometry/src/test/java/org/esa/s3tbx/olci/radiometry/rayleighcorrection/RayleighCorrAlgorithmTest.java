package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import java.util.stream.DoubleStream;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileUtils;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrAlgorithmTest {

    private RayleighCorrAlgorithm algo;

    @Before
    public void setUp() throws Exception {
        algo = new RayleighCorrAlgorithm();
    }

    @Test
    public void testConvertToRadian() throws Exception {
        double[] convertDegreesToRadians = SmileUtils.convertDegreesToRadians(new double[]{50, 30, -1});
        assertEquals(0.872664626, convertDegreesToRadians[0], 1e-8);
        assertEquals(0.5235987756, convertDegreesToRadians[1], 1e-8);
        assertEquals(-0.017453292519943295, convertDegreesToRadians[2], 1e-8);

    }

    @Test
    public void testCalculateRayleighCrossSection() throws Exception {
        double[] sectionSigma = algo.getCrossSection(new double[]{1., 2.});
        assertEquals(2, sectionSigma.length);
        assertEquals(1.004158010817489E-9, sectionSigma[0], 1e-8);
        assertEquals(3.9154039638717356E-12, sectionSigma[1], 1e-8);
    }

    @Test
    public void testCheckIn() throws Exception {
        double[] n = new double[]{2.840904951095581,
                17.638418197631836,
                28.7684268951416,
                36.189727783203125,
                43.61144256591797,
                51.033390045166016,
                58.45547866821289,
                65.87765502929688,
                69.58876037597656,
                73.29988098144531,
                77.0110092163086,
                80.7221450805664};

        boolean b = DoubleStream.of(n).anyMatch(x -> x < 2.82);
        assertFalse(b);
        boolean c = DoubleStream.of(n).anyMatch(x -> x > 2.82 && x < 80.7221450805664);
        assertTrue(c);
    }


}