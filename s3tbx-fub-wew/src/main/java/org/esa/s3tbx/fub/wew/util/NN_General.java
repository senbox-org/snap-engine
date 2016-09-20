package org.esa.s3tbx.fub.wew.util;

public class NN_General {

    // Input limits (min/max) from training data set
    public final static double[][] NODES_INPUT_SCALE_LIMITS = new double[][]{
                {+1.702400e-02, +8.136440e-02,},
                {+1.400690e-02, +7.919740e-02,},
                {+1.013800e-02, +8.273180e-02,},
                {+8.619290e-03, +8.449000e-02,},
                {+5.716580e-03, +8.982120e-02,},
                {+3.898480e-03, +8.450180e-02,},
                {+3.346080e-03, +8.031400e-02,},
                {+2.773580e-03, +7.758420e-02,},
                {+2.318270e-03, +6.796170e-02,},
                {+2.074470e-03, +6.784170e-02,},
                {+1.476870e-03, +6.428500e-02,},
                {+1.363570e-03, +6.350590e-02,},
                {+1.500000e+00, +7.226600e+00,},
                {+9.800000e+02, +1.040000e+03,},
                {+2.468300e-01, +9.999990e-01,},
                {-6.613120e-01, +6.613120e-01,},
                {-6.613120e-01, +6.613120e-01,},
                {+7.501110e-01, +1.000000e+00,},
    };

    // Input offset factors
    public final static double[] NODES_INPUT_SCALE_OFF = new double[]{
                +0.000000e+00, +0.000000e+00, +0.000000e+00, +0.000000e+00,
                +0.000000e+00, +0.000000e+00, +0.000000e+00, +0.000000e+00,
                +0.000000e+00, +0.000000e+00, +0.000000e+00, +0.000000e+00,
                +5.000000e-02, +5.000000e-02, +5.000000e-02, +5.000000e-02,
                +5.000000e-02, +5.000000e-02,
    };

    // Input scale flags
    public final static int[] NODES_INPUT_SCALE_FLAG = new int[]{
                +1, +1, +1, +1, +1, +1, +1, +1, +1, +1,
                +1, +1, +0, +0, +0, +0, +0, +0,
    };


    /*-------------------------------------------------------------------------*/

    public static int lrecall_run19_C2_080_nn(float[][] in, int ni, float[][] out, int no, int w, int mask[], int errmask, float[] a) {
        return NN_AtmCorr.compute(in, ni, out, no, w, mask, errmask, a);
    }

    public static int lrecall_run38_C2_040_nn(float[][] in, int ni, float[][] out, int no, int w, int mask[], int errmask, float[] a) {
        return NN_YellowSubstance.compute(in, ni, out, no, w, mask, errmask, a);
    }

    public static int lrecall_run39_C2_080_nn(float[][] in, int ni, float[][] out, int no, int w, int mask[], int errmask, float[] a) {
        return NN_TSM.compute(in, ni, out, no, w, mask, errmask, a);
    }

    public static int lrecall_run46_C2_100_nn(float[][] in, int ni, float[][] out, int no, int w, int mask[], int errmask, float[] a) {
        return NN_CHL.compute(in, ni, out, no, w, mask, errmask, a);
    }
}
