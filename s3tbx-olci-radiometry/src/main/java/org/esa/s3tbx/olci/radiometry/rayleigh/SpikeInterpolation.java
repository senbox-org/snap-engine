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

package org.esa.s3tbx.olci.radiometry.rayleigh;

import com.google.common.primitives.Doubles;
import org.apache.commons.math3.analysis.interpolation.BicubicSplineInterpolatingFunction;
import org.apache.commons.math3.analysis.interpolation.BicubicSplineInterpolator;

import javax.media.jai.Interpolation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author muhammad.bc.
 */
public class SpikeInterpolation {
    public static double interpolate2D(double[][] doubles2D, double[] xCoordinate, double[] yCoordinate,
                                       double x, double y) {

//        https://en.wikipedia.org/wiki/Bilinear_interpolation
        double x1 = getLowerBound(xCoordinate, x);
        double y1 = getLowerBound(yCoordinate, y);

        double x2 = getUpperValue(xCoordinate, x);
        double y2 = getUpperValue(yCoordinate, y);

        int ix1 = arrayIndex(xCoordinate, x1);
        int ix2 = arrayIndex(xCoordinate, x2);

        int iy1 = arrayIndex(yCoordinate, y1);
        int iy2 = arrayIndex(yCoordinate, y2);

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
        return new double[]{interpolateBI};
    }

    public static double useApacheMath(double[] xval, double[] yval, double[][] fval, double x, double y) {
        BicubicSplineInterpolator interpolator = new BicubicSplineInterpolator();
        BicubicSplineInterpolatingFunction interpolate = interpolator.interpolate(xval, yval, fval);
        return interpolate.value(x, y);

    }

    public static double interBetween(double lowerY1, double upperY2, double upperX2, double lowerX1, double position) {
        return lowerY1 + ((upperY2 - lowerY1) * (position - lowerX1)) / (upperX2 - lowerX1);
    }

    public static int arrayIndex(double[] xCoordinate, double val) {
        return Doubles.asList(xCoordinate).indexOf(val);
    }

    public static double getUpperValue(double[] doubles, double val) {
        final List<Double> xMin = new ArrayList<>();
        int length = doubles.length;
        IntStream.range(0, length).forEach(i -> {
            double v = doubles[i];
            if (v >= val) {
                xMin.add(v);
            }
        });
        double[] allMax = Doubles.toArray(xMin);
        if (allMax.length == 0) {
            throw new IllegalArgumentException("Can fine the closest max value of " + val);
        }
        return Doubles.min(allMax);
    }

    public static double getLowerBound(double[] doubles, double val) {
        final double[] xMin = new double[1];
        int length = doubles.length;
        IntStream.range(0, length).forEach(i -> {
            double v = doubles[i];
            xMin[0] = v < val ? v : xMin[0];
        });
        if (xMin[0] > val) {
            throw new IllegalArgumentException("Can find the closest min value of " + val);
        }
        return xMin[0];
    }
}
