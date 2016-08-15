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

/**
 * @author muhammad.bc.
 */
public class SmileUtils {

    public static float[] multiple2ArrayFloat(float[] array1, float[] array2) {
        if (array1.length != array2.length) {
            throw new OperatorException("The arrays most have the same length.");
        }
        final float[] cal = new float[array1.length];
        for (int i = 0; i < array1.length; i++) {
            cal[i] = array1[i] * array2[i];
        }
        return cal;
    }

    public static float[] multiple3ArrayFloat(float[] array1, float[] array2, float[] array3) {
        return multiple2ArrayFloat(multiple2ArrayFloat(array1, array2), array3);
    }

    public static double[] convertDegreesToRadians(double[] angle) {
        final double[] rads = new double[angle.length];
        for (int i = 0; i < rads.length; i++) {
            rads[i] = Math.toRadians(angle[i]);
        }
        return rads;
    }

    public static float[] convertDegreesToRadians(float[] angle) {
        final float[] rads = new float[angle.length];
        for (int i = 0; i < rads.length; i++) {
            rads[i] = (float) Math.toRadians(angle[i]);
        }
        return rads;
    }

    public static double[] getAirMass(double[] sunZenithAngles, double[] viewZenithAngles) {
        double[] szaRads = convertDegreesToRadians(sunZenithAngles);
        double[] ozaRads = convertDegreesToRadians(viewZenithAngles);
        double massAir[] = new double[szaRads.length];
        for (int index = 0; index < szaRads.length; index++) {
            massAir[index] = 1 / Math.cos(szaRads[index]) + 1 / Math.cos(ozaRads[index]);
        }
        return massAir;

    }

    public static double[] getAziDiff(double[] sunAzimuthAngles, double[] viewAzimuthAngles) {
        double[] saaRads = convertDegreesToRadians(sunAzimuthAngles);
        double[] aooRads = convertDegreesToRadians(viewAzimuthAngles);
        double[] aziDiff = new double[saaRads.length];
        for (int index = 0; index < saaRads.length; index++) {
            double a = aooRads[index] - saaRads[index];
            double cosDelta = Math.cos(a);
            aziDiff[index] = Math.acos(cosDelta);
        }
        return aziDiff;
    }
}
