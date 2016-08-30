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

package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import com.google.common.primitives.Doubles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import javax.media.jai.Interpolation;
import org.apache.commons.math3.analysis.interpolation.BicubicSplineInterpolatingFunction;
import org.apache.commons.math3.analysis.interpolation.BicubicSplineInterpolator;

/**
 * @author muhammad.bc.
 */
public class SpikeInterpolation {
    public static double interpolate2D(double[][] doubles2D, double[] xCoordinate, double[] yCoordinate,
                                       double x, double y) {

//        https://en.wikipedia.org/wiki/Bilinear_interpolation
        double x1 = getMin(xCoordinate, x);
        double y1 = getMin(yCoordinate, y);

        double x2 = getMax(xCoordinate, x);
        double y2 = getMax(yCoordinate, y);

        int ix1 = arrayIndex(xCoordinate, x1);
        int ix2 = arrayIndex(xCoordinate, x2);

        int iy1 = arrayIndex(yCoordinate, y1);
        int iy2 = arrayIndex(yCoordinate, y2);

        if (iy1 == -1 || iy2 == -1 || ix1 == -1 || ix2 == -1) {
            // todo: mba ask Carsten what to do about the extrapolation 8/26/2016
            // check if is ascending coordinate
            // check if the same length
//            findNormDistance() (y-y2)/(yCoordinate[iy2+1]-yCoordinate[iy2])
            doExtrapolation();
        }
        double f11 = doubles2D[ix1][iy1];
        double f12 = doubles2D[ix1][iy2];
        double f21 = doubles2D[ix2][iy1];
        double f22 = doubles2D[ix2][iy2];

        double q11 = interBetween(f11, f21, x2, x1, x);
        double q12 = interBetween(f12, f22, x2, x1, x);
        double interpolateValue = interBetween(q11, q12, y2, y1, y);

        return interpolateValue;
    }

    private static void doExtrapolation() {

    }

    public static double[] useLibJAI(double[][] samples, float xfrac, float yfrac) {
        Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        double interpolateBI = interpolation.interpolate(samples, xfrac, yfrac);
        System.out.println("interpolate with bilinear JAI lib = " + interpolateBI);
        return new double[]{interpolateBI};
    }

    public static double useApacheMath(double[] xval, double[] yval, double[][] fval, double x, double y) {
        BicubicSplineInterpolator interpolator = new BicubicSplineInterpolator();
        BicubicSplineInterpolatingFunction interpolate = interpolator.interpolate(xval, yval, fval);
        System.out.println("interpolate with bicubic Apache Lib = " + interpolate.value(x, y));
        return interpolate.value(x, y);

    }

    private static double interBetween(double f11, double f12, double x2, double x1, double x) {
        return f11 + ((f12 - f11) * (x - x1)) / (x2 - x1);
    }

    private static int arrayIndex(double[] xCoordinate, double val) {
        return Doubles.asList(xCoordinate).indexOf(val);
    }

    public static double getMax(double[] doubles, double val) {
        final List<Double> xMin = new ArrayList<>();
        int length = doubles.length;
        IntStream.range(0, length).forEach(i -> {
            double v = doubles[i];
            if (v >= val) {
                xMin.add(v);
            }
        });
        double[] allMax = Doubles.toArray(xMin);
        return Doubles.min(allMax);
    }

    public static double getMin(double[] doubles, double val) {
        final double[] xMin = new double[1];
        int length = doubles.length;
        IntStream.range(0, length).forEach(i -> {
            double v = doubles[i];
            xMin[0] = v < val ? v : xMin[0];
        });
        return xMin[0];
    }
}
