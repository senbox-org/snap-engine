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

package org.esa.s3tbx.olci.radiometry.smilecorr;

import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;

import java.util.stream.IntStream;

/**
 * @author muhammad.bc.
 */
public class SmileCorrectionUtils {

    public static float[] multiple2ArrayFloat(float[] array1, float[] array2) {
        if (array1.length != array2.length) {
            throw new OperatorException("The arrays most have the same length.");
        }
        final float[] cal = new float[array1.length];
        IntStream.range(0, cal.length).forEach(value -> cal[value] = array1[value] * array2[value]);
        return cal;
    }

    public static float[] multiple3ArrayFloat(float[] array1, float[] array2, float[] array3) {
        return multiple2ArrayFloat(multiple2ArrayFloat(array1, array2), array3);
    }

    public static double[] convertDegreesToRadians(double[] angle) {
        final double[] rads = new double[angle.length];
        IntStream.range(0, rads.length).forEach(value -> rads[value] = Math.toRadians(angle[value]));
        return rads;
    }

    public static float[] convertDegreesToRadians(float[] angle) {
        final float[] rads = new float[angle.length];
        IntStream.range(0, rads.length).forEach(value -> rads[value] = (float) Math.toRadians(angle[value]));
        return rads;
    }

    public static double[] getAirMass(double[] cosOZARads, double[] cosSZARads) {
        double massAir[] = new double[cosOZARads.length];
        IntStream.range(0, massAir.length).forEach(value -> massAir[value] = 1 / cosSZARads[value] + 1 / cosOZARads[value]);
        return massAir;
    }

    public static double[] getAziDiff(double[] saaRads, double[] aooRads) {
        int length = saaRads.length;
        double[] aziDiff = new double[length];
        IntStream.range(0, length).forEach(value -> {
            double a = aooRads[value] - saaRads[value];
            double cosDelta = Math.cos(a);
            aziDiff[value] = Math.acos(cosDelta);
        });
        return aziDiff;
    }

    public static double[] getSampleDoubles(Tile sourceTile) {
        int maxX = sourceTile.getWidth();
        int maxY = sourceTile.getHeight();

        double[] val = new double[maxX * maxY];
        int index = 0;
        for (int y = sourceTile.getMinY(); y <= sourceTile.getMaxY(); y++) {
            for (int x = sourceTile.getMinX(); x <= sourceTile.getMaxX(); x++) {
                val[index++] = sourceTile.getSampleDouble(x, y);
            }
        }
        return val;
    }

    public static float[] getSampleFloats(Tile sourceTile) {
        int maxX = sourceTile.getWidth();
        int maxY = sourceTile.getHeight();

        float[] val = new float[maxX * maxY];
        int index = 0;
        for (int y = sourceTile.getMinY(); y <= sourceTile.getMaxY(); y++) {
            for (int x = sourceTile.getMinX(); x <= sourceTile.getMaxX(); x++) {
                val[index++] = sourceTile.getSampleFloat(x, y);
            }
        }
        return val;
    }
}
