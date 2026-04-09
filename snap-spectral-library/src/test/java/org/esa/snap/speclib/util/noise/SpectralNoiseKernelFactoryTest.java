package org.esa.snap.speclib.util.noise;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.*;


public class SpectralNoiseKernelFactoryTest {


    private static final double DOUBLE_ERR = 1.0e-10;


    @Test
    @STTM("SNAP-4173")
    public void test_EnsureKernelSizeDoesNotThrowWhenKernelFits() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_BOX, 5, 1.0, 3);

        parameter.ensureKernelSize(5);
        parameter.ensureKernelSize(6);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_EnsureKernelSizeThrowsWhenKernelIsGreaterThanNumBands() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_BOX, 7, 1.0, 3);

        expectIllegalArgumentException(() -> parameter.ensureKernelSize(6),
                "Kernel size must not be greater than the number of selected bands.");
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ValidateFilterParametersAcceptsValidBoxParameters() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_BOX, 5, 1.0, 3);

        parameter.validateFilterParameters();
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ValidateFilterParametersAcceptsValidGaussianParameters() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_GAUSSIAN, 5, 1.0, 3);

        parameter.validateFilterParameters();
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ValidateFilterParametersAcceptsValidSavitzkyGolayParameters() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_SG, 11, 1.0, 3);

        parameter.validateFilterParameters();
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ValidateFilterParametersThrowsForNullFilterType() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                null, 5, 1.0, 3);

        expectIllegalArgumentException(parameter::validateFilterParameters,
                "Filter type must not be null");
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ValidateFilterParametersThrowsForKernelSizeLessThanThree() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_BOX, 1, 1.0, 3);

        expectIllegalArgumentException(parameter::validateFilterParameters,
                "Kernel size must be odd and >= 3.");
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ValidateFilterParametersThrowsForEvenKernelSize() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_BOX, 4, 1.0, 3);

        expectIllegalArgumentException(parameter::validateFilterParameters,
                "Kernel size must be odd and >= 3.");
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ValidateFilterParametersThrowsForGaussianSigmaZero() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_GAUSSIAN, 5, 0.0, 3);

        expectIllegalArgumentException(parameter::validateFilterParameters,
                "Gaussian sigma must be > 0.");
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ValidateFilterParametersThrowsForGaussianSigmaNegative() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_GAUSSIAN, 5, -1.0, 3);

        expectIllegalArgumentException(parameter::validateFilterParameters,
                "Gaussian sigma must be > 0.");
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ValidateFilterParametersThrowsForNegativeSavitzkyGolayPolynomialOrder() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_SG, 5, 1.0, -1);

        expectIllegalArgumentException(parameter::validateFilterParameters,
                "Savitzky-Golay polynomial order must be >= 0.");
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ValidateFilterParametersThrowsForSavitzkyGolayPolynomialOrderEqualToKernelSize() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_SG, 5, 1.0, 5);

        expectIllegalArgumentException(parameter::validateFilterParameters,
                "Savitzky-Golay polynomial order must be smaller than kernel size.");
    }

    @Test
    @STTM("SNAP-4173")
    public void test_ValidateFilterParametersThrowsForSavitzkyGolayPolynomialOrderGreaterThanKernelSize() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_SG, 5, 1.0, 6);

        expectIllegalArgumentException(parameter::validateFilterParameters,
                "Savitzky-Golay polynomial order must be smaller than kernel size.");
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateKernelReturnsBoxKernel() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_BOX, 5, 1.0, 3);

        double[] kernel = parameter.createKernel();

        assertEquals(5, kernel.length);
        assertArrayEquals(new double[]{0.2, 0.2, 0.2, 0.2, 0.2}, kernel, DOUBLE_ERR);
        assertKernelNormalized(kernel);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateKernelReturnsGaussianKernel() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_GAUSSIAN, 5, 1.0, 3);

        double[] kernel = parameter.createKernel();

        assertEquals(5, kernel.length);
        assertKernelNormalized(kernel);
        assertSymmetric(kernel);
        assertTrue(kernel[2] > kernel[1]);
        assertTrue(kernel[1] > kernel[0]);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateKernelReturnsSavitzkyGolayKernel_Window5_Order2() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_SG, 5, 1.0, 2);

        double[] kernel = parameter.createKernel();

        assertEquals(5, kernel.length);
        assertKernelNormalized(kernel);
        assertSymmetric(kernel);
        assertArrayEquals(new double[]{
                -3.0 / 35.0,
                12.0 / 35.0,
                17.0 / 35.0,
                12.0 / 35.0,
                -3.0 / 35.0
        }, kernel, 1.0e-8);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateKernelReturnsSavitzkyGolayKernel_Window5_Order0() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_SG, 5, 1.0, 0);

        double[] kernel = parameter.createKernel();

        assertEquals(5, kernel.length);
        assertKernelNormalized(kernel);
        assertSymmetric(kernel);
        assertArrayEquals(new double[]{
                1.0 / 5.0,
                1.0 / 5.0,
                1.0 / 5.0,
                1.0 / 5.0,
                1.0 / 5.0
        }, kernel, 1.0e-8);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateKernelReturnsSavitzkyGolayKernel_Window5_Order3() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_SG, 5, 1.0, 3);

        double[] kernel = parameter.createKernel();

        assertEquals(5, kernel.length);
        assertKernelNormalized(kernel);
        assertSymmetric(kernel);
        assertArrayEquals(new double[]{
                -3.0 / 35.0,
                12.0 / 35.0,
                17.0 / 35.0,
                12.0 / 35.0,
                -3.0 / 35.0
        }, kernel, 1.0e-8);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateKernelReturnsSavitzkyGolayKernel_Window7_Order2() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_SG, 7, 1.0, 2);

        double[] kernel = parameter.createKernel();

        assertEquals(7, kernel.length);
        assertKernelNormalized(kernel);
        assertSymmetric(kernel);
        assertArrayEquals(new double[]{
                -2.0 / 21.0,
                1.0 / 7.0,
                2.0 / 7.0,
                1.0 / 3.0,
                2.0 / 7.0,
                1.0 / 7.0,
                -2.0 / 21.0
        }, kernel, 1.0e-8);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateKernelReturnsSavitzkyGolayKernel_Window7_Order4() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_SG, 7, 1.0, 4);

        double[] kernel = parameter.createKernel();

        assertEquals(7, kernel.length);
        assertKernelNormalized(kernel);
        assertSymmetric(kernel);
        assertArrayEquals(new double[]{
                5.0 / 231.0,
                -10.0 / 77.0,
                25.0 / 77.0,
                131.0 / 231.0,
                25.0 / 77.0,
                -10.0 / 77.0,
                5.0 / 231.0
        }, kernel, 1.0e-8);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateKernelReturnsSavitzkyGolayKernel_Window9_Order2() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_SG, 9, 1.0, 2);

        double[] kernel = parameter.createKernel();

        assertEquals(9, kernel.length);
        assertKernelNormalized(kernel);
        assertSymmetric(kernel);
        assertArrayEquals(new double[]{
                -1.0 / 11.0,
                2.0 / 33.0,
                13.0 / 77.0,
                18.0 / 77.0,
                59.0 / 231.0,
                18.0 / 77.0,
                13.0 / 77.0,
                2.0 / 33.0,
                -1.0 / 11.0
        }, kernel, 1.0e-8);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateKernelReturnsSavitzkyGolayKernel_Window9_Order4() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                SpectralNoiseKernelFactory.FILTER_SG, 9, 1.0, 4);

        double[] kernel = parameter.createKernel();

        assertEquals(9, kernel.length);
        assertKernelNormalized(kernel);
        assertSymmetric(kernel);
        assertArrayEquals(new double[]{
                5.0 / 143.0,
                -5.0 / 39.0,
                10.0 / 143.0,
                45.0 / 143.0,
                179.0 / 429.0,
                45.0 / 143.0,
                10.0 / 143.0,
                -5.0 / 39.0,
                5.0 / 143.0
        }, kernel, 1.0e-8);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateKernelThrowsForUnsupportedFilterType() {
        SpectralNoiseKernelFactory parameter = new SpectralNoiseKernelFactory(
                "Unsupported", 5, 1.0, 3);

        expectIllegalArgumentException(parameter::createKernel,
                "Unsupported filter type: Unsupported");
    }

    private static void assertKernelNormalized(double[] kernel) {
        double sum = 0.0;
        for (double v : kernel) {
            sum += v;
        }
        assertEquals(1.0, sum, 1.0e-8);
    }

    private static void assertSymmetric(double[] kernel) {
        for (int i = 0; i < kernel.length / 2; i++) {
            assertEquals(kernel[i], kernel[kernel.length - 1 - i], 1.0e-10);
        }
    }

    private static void expectIllegalArgumentException(ThrowingRunnable runnable, String expectedMessage) {
        try {
            runnable.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}