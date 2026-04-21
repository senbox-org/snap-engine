package org.esa.snap.speclib.util.noise;

public class SpectralNoiseReducer {


    private SpectralNoiseReducer() {}


    public static void applyConvolution(double[] spectrum, boolean[] validMask, double[] kernel, double[] result) {
        final int bandCount = spectrum.length;
        final int half = kernel.length / 2;

        for (int center = 0; center < bandCount; center++) {
            if (!validMask[center]) {
                result[center] = Double.NaN;
                continue;
            }

            double weightedSum = 0.0;
            double weightSum = 0.0;

            for (int k = -half; k <= half; k++) {
                final int sourceIndex = clamp(center + k, 0, bandCount - 1);
                if (!validMask[sourceIndex]) {
                    continue;
                }

                final double weight = kernel[k + half];
                weightedSum += weight * spectrum[sourceIndex];
                weightSum += weight;
            }

            result[center] = weightSum > 0.0 ? weightedSum / weightSum : spectrum[center];
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
