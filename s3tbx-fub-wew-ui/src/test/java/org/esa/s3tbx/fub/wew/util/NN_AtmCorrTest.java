package org.esa.s3tbx.fub.wew.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author muhammad.bc.
 */
public class NN_AtmCorrTest {
    final float[][] out = new float[12][1];
    final int width = 1;
    final int[] mask = new int[width];
    final float[] a = new float[width];
    final float[][] in = new float[18][1];


    @Test
    public void testNN_AtmCorrGetNumInputNode() throws Exception {
        int numNodesInput = -1;
        int numNodesOutput = 1;

        int numInputNode = NN_AtmCorr.compute(in, numNodesInput, out, numNodesOutput, width, mask, 0, a);
        assertEquals(18, numInputNode);
    }

    @Test
    public void testNN_AtmCorrGetNumOutputNode() throws Exception {
        int numNodesInput = 1;
        int numNodesOutput = -1;
        int numOutputNum = NN_AtmCorr.compute(in, numNodesInput, out, numNodesOutput, width, mask, 0, a);
        assertEquals(12, numOutputNum);
    }

    @Test
    public void testNN_AtmCorrCheckOutputNode() throws Exception {
        int numNodesInput = 18;
        int numNodesOutput = 10;
        int checkOutputNode = NN_AtmCorr.compute(in, numNodesInput, out, numNodesOutput, width, mask, 0, a);
        assertEquals(-2, checkOutputNode);
    }

    @Test
    public void testNN_AtmCorrCheckInputNode() throws Exception {
        int numNodesInput = 11;
        int numNodesOutput = 12;
        int checkOutputNode = NN_AtmCorr.compute(in, numNodesInput, out, numNodesOutput, width, mask, 0, a);
        assertEquals(-1, checkOutputNode);
    }


    @Test
    public void testNN_AtmCorrCompute() throws Exception {
        final float[][] input = new float[][]{
                {0.05943133f}, {0.05067047f}, {0.041214053f}, {0.037428323f}, {0.030080993f},
                {0.024526045f}, {0.023756435f}, {0.022254849f}, {0.021630857f}, {0.021160515f},
                {0.019966979f}, {0.019658221f}, {11.66836f}, {1023.05f}, {0.7145359f}, {-0.385183f},
                {-0.385695f}, {0.83837545f}};

        float[][] exOutput = new float[][]{
                {0.0064101876f}, {0.006221302f}, {0.0067794686f}, {0.0054770103f},
                {0.003863283f}, {0.0011327977f}, {7.242046E-4f}, {1.9468556E-4f},
                {1.1322831f}, {1.0241352f}, {0.9826809f}, {0.93816406f}};

        final int compute = NN_AtmCorr.compute(input, 18, out, 12, width, mask, 0, a);

        assertEquals(0, compute);
        assertArrayEquals(exOutput, out);
    }

}