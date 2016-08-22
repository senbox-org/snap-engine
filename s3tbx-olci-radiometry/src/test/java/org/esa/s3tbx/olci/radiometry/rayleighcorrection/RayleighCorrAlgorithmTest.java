package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileUtils;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertArrayEquals;
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
    public void testPhaseRaylMin() throws Exception {
        assertEquals(1.5, algo.phaseRaylMin(Math.toRadians(1.0), Math.toRadians(1.0), Math.toRadians(0)), 1e-8);
        assertEquals(1.41920746, algo.phaseRaylMin(Math.toRadians(45.0), Math.toRadians(30.0), Math.toRadians(20.0)), 1e-8);
        assertEquals(1.40039973, algo.phaseRaylMin(Math.toRadians(60.0), Math.toRadians(40.0), Math.toRadians(10.0)), 1e-8);
    }

    @Test
    public void testPhaseRaylMinWithZero() throws Exception {
        assertEquals(0.9569723939515613, algo.phaseRaylMin(0.0, 45.0, algo.getAzimuthDifference(40.0, 0.0)), 1e-8);
        assertEquals(1.5, algo.phaseRaylMin(0.0, 0.0, algo.getAzimuthDifference(30.0, 0.0)), 1e-8);
        assertEquals(1.5, algo.phaseRaylMin(0.0, 0.0, algo.getAzimuthDifference(0.0, 0.0)), 1e-8);
    }

    @Test
    public void testGetCosScatterAngleWithZero() throws Exception {
        assertEquals(-0.15425144988758405, algo.cosScatterAngle(0, 30, 30), 1e-8);
        assertEquals(-1.0, algo.cosScatterAngle(0, 0, 30), 1e-8);
        assertEquals(-1.0, algo.cosScatterAngle(0, 0, 0), 1e-8);
    }

    @Test
    public void testPressureAtElevation() throws Exception {
        double[] pressureAtSurface = algo.getPressureAtSurface(new double[]{1.0, 2.0}, new double[]{0.0, 1.0});
        assertEquals(2, pressureAtSurface.length);
        assertEquals(1.0, pressureAtSurface[0], 1e-8);
        assertEquals(1.999750015624349, pressureAtSurface[1], 1e-8);
    }

    @Test
    public void testRayleighThickness() throws Exception {
        double[] taurPoZs = algo.getRayleighOpticalThickness(new double[]{1000, 996, 1020}, 0.06);
        assertEquals(3, taurPoZs.length);
        assertEquals(0.05923000, taurPoZs[0], 1e-8);
        assertEquals(0.05899308, taurPoZs[1], 1e-8);
        assertEquals(0.06041461, taurPoZs[2], 1e-8);
    }

    @Test
    public void testGetTaur() throws Exception {
        double[] doubles = new double[]{412.5, 442.5, 490, 510, 560, 620, 665, 681.25, 708.75, 753.75, 778.75, 865, 885};
        double[] taurs = algo.getTaurStd(doubles);
        double[] taursExp = new double[]{0.3552985127, 0.2670335786, 0.1763712746, 0.1498827898, 0.1024524651, 0.0677183447, 0.0509237429, 0.046160331, 0.0392966673, 0.0305916887, 0.0267891122, 0.0174738524, 0.0159223057};
        assertArrayEquals(taursExp, taurs, 1e-8);
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
        double[] sectionSigma = algo.getCrossSectionSigma(new double[]{1., 2.});
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

    @Test
    public void spike() throws Exception {
        Pattern compile = Pattern.compile("(\\d+)");
        Matcher matcher = compile.matcher("solar_flux_band_01");
        matcher.find();
        String group = matcher.group(0);
        System.out.println("group = " + group);
        int i = Integer.parseInt(group);
        System.out.println("i = " + i);

    }
}