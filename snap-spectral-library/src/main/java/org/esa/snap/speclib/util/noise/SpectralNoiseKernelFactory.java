package org.esa.snap.speclib.util.noise;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;

import java.util.Arrays;


public class SpectralNoiseKernelFactory {


    public static final String FILTER_SG = "Savitzky-Golay";
    public static final String FILTER_GAUSSIAN = "Gaussian";
    public static final String FILTER_BOX = "Box";
    public static final String FILTER_RICKER_WAVELET = "Ricker Wavelet";
    public static final String FILTER_TRAPEZOID = "Trapezoid";

    private final String filterType;
    private final int kernelSize;
    private final double gaussianSigma;
    private final int sgPolynomialOrder;
    private final double rickerWidth;
    private final double trapezoidWidth;
    private final double trapezoidSlope;


    public SpectralNoiseKernelFactory(String filterType, int kernelSize, double gaussianSigma, int sgPolynomialOrder) {
        this(filterType, kernelSize, gaussianSigma, sgPolynomialOrder, 1.0, 3.0, 1.0);
    }

    public SpectralNoiseKernelFactory(String filterType, int kernelSize, double gaussianSigma, int sgPolynomialOrder,
                                      double rickerWidth, double trapezoidWidth, double trapezoidSlope) {
        this.filterType = filterType;
        this.kernelSize = kernelSize;
        this.gaussianSigma = gaussianSigma;
        this.sgPolynomialOrder = sgPolynomialOrder;
        this.rickerWidth = rickerWidth;
        this.trapezoidWidth = trapezoidWidth;
        this.trapezoidSlope = trapezoidSlope;
    }


    public void ensureKernelSize(int numBands) {
        if (getActualKernelSize() > numBands) {
            throw new IllegalArgumentException("Kernel size must not be greater than the number of selected bands.");
        }
    }

    private int getActualKernelSize() {
        if (FILTER_RICKER_WAVELET.equals(filterType)) {
            return roundUpToOddInteger(8.0 * rickerWidth);
        }
        if (FILTER_TRAPEZOID.equals(filterType)) {
            return roundUpToOddInteger(trapezoidWidth + 2.0 / trapezoidSlope);
        }
        return kernelSize;
    }

    public void validateFilterParameters() {
        if (filterType == null) {
            throw new IllegalArgumentException("Filter type must not be null");
        }
        if (!FILTER_RICKER_WAVELET.equals(filterType) && !FILTER_TRAPEZOID.equals(filterType) && (kernelSize < 3 || kernelSize % 2 == 0)) {
            throw new IllegalArgumentException("Kernel size must be odd and >= 3.");
        }

        if (FILTER_GAUSSIAN.equals(filterType) && gaussianSigma <= 0.0) {
            throw new IllegalArgumentException("Gaussian sigma must be > 0.");
        }

        if (FILTER_SG.equals(filterType)) {
            if (sgPolynomialOrder < 0) {
                throw new IllegalArgumentException("Savitzky-Golay polynomial order must be >= 0.");
            }
            if (sgPolynomialOrder >= kernelSize) {
                throw new IllegalArgumentException("Savitzky-Golay polynomial order must be smaller than kernel size.");
            }
        }

        if (FILTER_RICKER_WAVELET.equals(filterType) && rickerWidth <= 0.0) {
            throw new IllegalArgumentException("Ricker wavelet width must be > 0.");
        }
        if (FILTER_TRAPEZOID.equals(filterType)) {
            if (trapezoidWidth <= 0.0) {
                throw new IllegalArgumentException("Trapezoid width must be > 0.");
            }
            if (trapezoidSlope <= 0.0) {
                throw new IllegalArgumentException("Trapezoid slope must be > 0.");
            }
        }
    }


    public double[] createKernel() {
        if (FILTER_BOX.equals(filterType)) {
            return createBoxKernel(kernelSize);
        }
        if (FILTER_GAUSSIAN.equals(filterType)) {
            return createGaussianKernel(kernelSize, gaussianSigma);
        }
        if (FILTER_SG.equals(filterType)) {
            try {
                return createSavitzkyGolayKernel(kernelSize, sgPolynomialOrder);
            } catch (SingularMatrixException ex) {
                throw new IllegalStateException("Failed to create Savitzky-Golay kernel.", ex);
            }
        }
        if (FILTER_RICKER_WAVELET.equals(filterType)) {
            return createRickerWaveletKernel(rickerWidth);
        }
        if (FILTER_TRAPEZOID.equals(filterType)) {
            return createTrapezoidKernel(trapezoidWidth, trapezoidSlope);
        }

        throw new IllegalArgumentException("Unsupported filter type: " + filterType);
    }

    public boolean usesZeroIntegralKernel() {
        return FILTER_RICKER_WAVELET.equals(filterType);
    }

    private static double[] createBoxKernel(int size) {
        final double[] kernel = new double[size];
        final double weight = 1.0 / size;
        Arrays.fill(kernel, weight);
        return kernel;
    }

    private static double[] createGaussianKernel(int size, double sigma) {
        final double[] kernel = new double[size];
        final int half = size / 2;

        double sum = 0.0;
        for (int ii = -half; ii <= half; ii++) {
            final double value = Math.exp(-(ii * ii) / (2.0 * sigma * sigma));
            kernel[ii + half] = value;
            sum += value;
        }

        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }

        return kernel;
    }


    private static double[] createRickerWaveletKernel(double width) {
        final int size = roundUpToOddInteger(8.0 * width);
        final int half = size / 2;
        final double[] kernel = new double[size];
        final double amplitude = 1.0 / (Math.sqrt(2.0 * Math.PI) * width * width * width);

        for (int i = -half; i <= half; i++) {
            final double normalized = i / width;
            kernel[i + half] = amplitude * (1.0 - normalized * normalized) * Math.exp(-0.5 * normalized * normalized);
        }
        return kernel;
    }


    private static double[] createTrapezoidKernel(double width, double slope) {
        final int size = roundUpToOddInteger(width + 2.0 / slope);
        final int half = size / 2;
        final double[] kernel = new double[size];
        double sum = 0.0;
        for (int i = -half; i <= half; i++) {
            final double distance = Math.abs(i);
            final double value = distance <= width / 2.0 ? 1.0 : Math.max(0.0, 1.0 - slope * (distance - width / 2.0));

            kernel[i + half] = value;
            sum += value;
        }

        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }

    private static int roundUpToOddInteger(double value) {
        final int roundedUp = (int) Math.ceil(value);
        return roundedUp % 2 == 0 ? roundedUp + 1 : roundedUp;
    }


    private static double[] createSavitzkyGolayKernel(int windowSize, int polynomialOrder) {
        final int half = windowSize / 2;
        final int cols = polynomialOrder + 1;

        final double[][] aData = new double[windowSize][cols];
        for (int row = 0; row < windowSize; row++) {
            final int x = row - half;
            double value = 1.0;
            for (int col = 0; col < cols; col++) {
                aData[row][col] = value;
                value *= x;
            }
        }

        final RealMatrix a = new Array2DRowRealMatrix(aData, false);
        final RealMatrix ata = a.transpose().multiply(a);

        final double[] e0Data = new double[cols];
        e0Data[0] = 1.0;
        final RealVector e0 = new ArrayRealVector(e0Data, false);

        final DecompositionSolver solver = new LUDecomposition(ata).getSolver();
        final RealVector tmp = solver.solve(e0);   // tmp = (A^T A)^-1 * e0

        final double[] kernel = a.operate(tmp).toArray();   // kernel = A * tmp

        double sum = 0.0;
        for (double value : kernel) {
            sum += value;
        }
        if (sum != 0.0) {
            for (int ii = 0; ii < kernel.length; ii++) {
                kernel[ii] /= sum;
            }
        }

        return kernel;
    }
}
