package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

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
        assertEquals(1.0912129731611722, algo.phaseRaylMin(1.0, 1.0, 1.0), 1e-8);
        assertEquals(0.7978394662881223, algo.phaseRaylMin(45.0, 40.0, 30.0), 1e-8);
        assertEquals(0.8303653103338895, algo.phaseRaylMin(60.0, 45.0, 40.0), 1e-8);
    }

    @Test
    public void testPhaseRaylMinWithZero() throws Exception {
        assertEquals(0.9569723939515613, algo.phaseRaylMin(0.0, 45.0, 40.0), 1e-8);
        assertEquals(1.5, algo.phaseRaylMin(0.0, 0.0, 30.0), 1e-8);
        assertEquals(1.5, algo.phaseRaylMin(0.0, 0.0, 0.0), 1e-8);
    }

    @Test
    public void testGetCosScatterAngleWithZero() throws Exception {
        assertEquals(-0.15425144988758405, algo.cosScatterAngle(0, 30, 30), 1e-8);
        assertEquals(-1.0, algo.cosScatterAngle(0, 0, 30), 1e-8);
        assertEquals(-1.0, algo.cosScatterAngle(0, 0, 0), 1e-8);
    }

    @Test
    public void testPressureAtElevation() throws Exception {
        double[] pressureAtSurface = algo.pressureAtSurface(new double[]{1.0, 2.0}, new double[]{0.0, 1.0});
        assertEquals(2, pressureAtSurface.length);
        assertEquals(1.0, pressureAtSurface[0], 1e-8);
        assertEquals(1.999750015624349, pressureAtSurface[1], 1e-8);
    }

    @Test
    public void testRayleighThickness() throws Exception {
        double[] taurPoZs = algo.getTaurPoZ(new double[]{2, 9, 15}, 2.0);
        assertEquals(3, taurPoZs.length);
        assertEquals(0.003076923076923077, taurPoZs[0], 1e-8);
        assertEquals(0.013846153846153847, taurPoZs[1], 1e-8);
        assertEquals(0.023076923076923078, taurPoZs[2], 1e-8);
    }

    @Test
    public void testGetTaur() throws Exception {
        double[] doubles = new double[]{412.5, 442.5, 490, 510, 560, 620, 665, 681.25, 708.75, 753.75, 778.75, 865, 885};
        double[] taurs = algo.getTaur(doubles);
        double[] taursExp = new double[]{0.3552985127, 0.2670335786, 0.1763712746, 0.1498827898, 0.1024524651, 0.0677183447, 0.0509237429, 0.046160331, 0.0392966673, 0.0305916887, 0.0267891122, 0.0174738524, 0.0159223057};
        assertArrayEquals(taursExp, taurs, 0);
    }

    @Test
    public void testGetRefl_Ray() throws Exception {
        double[] reflRaly = algo.getReflRaly(new double[2], 10.90, 23.9, 23.9);

    }
}