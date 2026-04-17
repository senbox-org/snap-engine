package org.esa.snap.speclib.util.noise;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.*;


public class SpectralNoiseReducerTest {


    private static final double DOUBLE_ERR = 1.0e-10;


    @Test
    @STTM("SNAP-4173")
    public void test_ApplyConvolutionComputesWeightedAverage() {
        double[] spectrum = {10.0, 20.0, 30.0};
        boolean[] validMask = {true, true, true};
        double[] kernel = {0.25, 0.5, 0.25};
        double[] result = new double[3];

        SpectralNoiseReducer.applyConvolution(spectrum, validMask, kernel, result);

        assertArrayEquals(new double[]{12.5, 20.0, 27.5}, result, DOUBLE_ERR);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ApplyConvolutionUsesClampAtEdges() {
        double[] spectrum = {10.0, 20.0, 30.0};
        boolean[] validMask = {true, true, true};
        double[] kernel = {0.25, 0.5, 0.25};
        double[] result = new double[3];

        SpectralNoiseReducer.applyConvolution(spectrum, validMask, kernel, result);

        assertEquals(12.5, result[0], DOUBLE_ERR);
        assertEquals(27.5, result[2], DOUBLE_ERR);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ApplyConvolutionSetsNaNForInvalidCenterSample() {
        double[] spectrum = {10.0, 20.0, 30.0};
        boolean[] validMask = {true, false, true};
        double[] kernel = {0.25, 0.5, 0.25};
        double[] result = new double[3];

        SpectralNoiseReducer.applyConvolution(spectrum, validMask, kernel, result);

        assertEquals(10.0, result[0], DOUBLE_ERR);
        assertTrue(Double.isNaN(result[1]));
        assertEquals(30.0, result[2], DOUBLE_ERR);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ApplyConvolutionSkipsInvalidNeighborSamplesAndRenormalizes() {
        double[] spectrum = {10.0, 20.0, 30.0};
        boolean[] validMask = {true, true, false};
        double[] kernel = {0.25, 0.5, 0.25};
        double[] result = new double[3];

        SpectralNoiseReducer.applyConvolution(spectrum, validMask, kernel, result);

        assertEquals(0.25 * 10.0 + 0.5 * 10.0 + 0.25 * 20.0, result[0], DOUBLE_ERR);
        assertEquals((0.25 * 10.0 + 0.5 * 20.0) / 0.75, result[1], DOUBLE_ERR);
        assertTrue(Double.isNaN(result[2]));
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ApplyConvolutionFallsBackToOriginalSpectrumWhenWeightSumIsZero() {
        double[] spectrum = {10.0, 20.0, 30.0};
        boolean[] validMask = {true, true, true};
        double[] kernel = {0.0, 0.0, 0.0};
        double[] result = new double[3];

        SpectralNoiseReducer.applyConvolution(spectrum, validMask, kernel, result);

        assertArrayEquals(spectrum, result, DOUBLE_ERR);
    }
}