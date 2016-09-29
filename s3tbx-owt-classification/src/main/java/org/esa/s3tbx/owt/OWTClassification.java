/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.owt;


import Jama.Matrix;
import com.bc.ceres.core.Assert;

/**
 * Computes fractional class memberships for a spectrum.
 */
public class OWTClassification {

    private final double[][] uReflecMeans;
    private final double[][][] invCovMatrix;
    private int wavelengthCount;
    private int classCount;

    /**
     * Creates an instance of the fuzzy classification class.
     *
     * @param reflectanceMeans       a two dimensional array specifying the mean spectrum for each class.
     *                               The first dimension specifies the number of bands,
     *                               the second specifies the number of classes.
     * @param invertedClassCovMatrix a three dimensional array.
     *                               The first dimension specifies the number of classes,
     *                               the second and third dimensions build up the squared matrix defined by
     *                               the number of wavelength.
     */
    public OWTClassification(double[][] reflectanceMeans, double[][][] invertedClassCovMatrix) {
        wavelengthCount = reflectanceMeans.length;
        classCount = reflectanceMeans[0].length;
        final String pattern = "Number of %s of reflectanceMeans [%d] and invertedClassCovMatrix [%d] do not match.";
        Assert.argument(invertedClassCovMatrix.length == classCount,
                        String.format(pattern, "classes", classCount, invertedClassCovMatrix.length));
        Assert.argument(invertedClassCovMatrix[0].length == wavelengthCount,
                        String.format(pattern, "wavelength", wavelengthCount, invertedClassCovMatrix[0].length));
        uReflecMeans = reflectanceMeans.clone();
        invCovMatrix = invertedClassCovMatrix.clone();
    }

    /**
     * The number of bands used by the classification.
     *
     * @return the number bands used.
     */
    public int getWavelengthCount() {
        return wavelengthCount;
    }

    /**
     * The number of classes computed by the classification.
     *
     * @return the number classes computed.
     */
    public int getClassCount() {
        return classCount;
    }

    /**
     * Computes the fractional class memberships for the given spectrum.
     *
     * @param reflectances The spectrum to compute the class memberships for.
     *                     The length of the spectrum must be equal to {@link #getWavelengthCount()}
     * @return The fractional class memberships. The length of the returned array
     * is equal to {@link #getClassCount()}
     */
    public double[] computeClassMemberships(double[] reflectances) throws OWTException {
        final String pattern = "Number of reflectances must be %d but is %d.";
        Assert.argument(reflectances.length == wavelengthCount, String.format(pattern, wavelengthCount, reflectances.length));

        double[] y = new double[wavelengthCount];
        double[][] yInvers = new double[wavelengthCount][wavelengthCount];   // yinv
        double[] alphaChi = new double[classCount];

        for (int i = 0; i < classCount; i++) {
            for (int j = 0; j < wavelengthCount; j++) {
                y[j] = reflectances[j] - uReflecMeans[j][i];
                System.arraycopy(invCovMatrix[i][j], 0, yInvers[j], 0, wavelengthCount);
            }
            final Matrix yInvMatrix = new Matrix(yInvers);
            final Matrix matrixB = yInvMatrix.times(new Matrix(y, y.length));  // b
            double zSquare = 0;
            for (int j = 0; j < wavelengthCount; j++) {
                zSquare += y[j] * matrixB.getArray()[j][0];
            }
            double x = zSquare / 2.0;   // no idea why this is needed. Even Tim doesn't have
            double chiSquare = wavelengthCount / 2.0;
            if (x <= (chiSquare + 1.0)) {
                double gamma = computeIGFSeries(chiSquare, x);
                alphaChi[i] = 1.0 - gamma;
            } else {
                double gamma = computeIGFContinuedFraction(chiSquare, x);
                alphaChi[i] = gamma;
            }
        }

        return alphaChi;

    }

    // Computes the incomplete gamma function by its continued fraction
    private static double computeIGFContinuedFraction(double a, double x) throws OWTException {
        final double min = 1.0e-30;
        final double constFactor = Math.exp(-x + a * Math.log(x) - logGamma(a));
        double b = x + 1.0 - a;
        double c = 1.0 / min;
        double d = 1.0 / b;
        double h = d;
        for (int i = 1; i <= 100; i++) {
            double an = -i * (i - a);
            b += 2.0;
            d = an * d + b;
            c = b + an / c;
            if (Math.abs(d) < min) {
                d = min;
            }
            if (Math.abs(c) < min) {
                c = min;
            }
            d = 1.0 / d;
            double del = d * c;
            h *= del;
            if (Math.abs(del - 1.0) < 3.0e-7) {
                return constFactor * h;
            }
        }
        throw new OWTException("Parameter 'a' is too large");
    }

    // Computes the incomplete gamma function by its series representation
    private static double computeIGFSeries(double a, double x) throws OWTException {
        if (x < 0.0) {
            throw new OWTException("x must be greater or equal to zero");
        }
        if (x > 0.0) {
            double incA = a;
            double sum = 1.0 / a;
            double del = sum;

            final int maxIteration = 100;
            final double eps = 3.0e-7;
            final double constFactor = Math.exp(-x + a * Math.log(x) - logGamma(a));
            for (int i = 1; i <= maxIteration; i++) {
                del *= x / ++incA;
                sum += del;
                if (Math.abs(del) < Math.abs(sum) * eps) {
                    return sum * constFactor;
                }
            }
            throw new OWTException("Parameter 'a' is too large");
        } else {
            return 0.0;
        }
    }

    private static double logGamma(double x) {
        final double[] coefficients = {
                76.18009172947146, -86.50532032941677, 24.01409824083091,
                -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5
        };
        double tempX = x;

        double tmp = x + 5.5;
        tmp -= (x + 0.5) * Math.log(tmp);
        double sum = 1.000000000190015;
        for (int i = 0; i <= 5; i++) {
            sum += coefficients[i] / ++tempX;
        }
        return -tmp + Math.log(2.5066282746310005 * sum / x);
    }

}
