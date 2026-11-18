package org.esa.snap.speclib.util.noise;

public class SpectralNoiseReducer {


    private SpectralNoiseReducer() {}


    public static void applyConvolution(double[] spectrum, boolean[] validMask, double[] kernel, double[] result) {
        applyConvolution(spectrum, validMask, kernel, result, true, true);
    }

    public static void applyConvolution(double[] spectrum, boolean[] validMask, double[] kernel, double[] result,
                                        boolean renormalizeMissingSamples, boolean clampOutsideBounds) {
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
                final int sourceIndex = clampOutsideBounds
                        ? Math.max(0, Math.min(center + k, bandCount - 1))
                        : center + k;
                if (sourceIndex < 0 || sourceIndex >= bandCount || !validMask[sourceIndex]) {
                    continue;
                }

                final double weight = kernel[k + half];
                weightedSum += weight * spectrum[sourceIndex];
                weightSum += weight;
            }

            result[center] = renormalizeMissingSamples
                    ? (weightSum > 0.0 ? weightedSum / weightSum : spectrum[center])
                    : weightedSum;
        }
    }
}
