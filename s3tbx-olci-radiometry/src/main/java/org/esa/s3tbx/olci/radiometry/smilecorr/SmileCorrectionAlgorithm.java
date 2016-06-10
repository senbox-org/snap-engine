/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.olci.radiometry.smilecorr;


/**
 * Applies a SMILE correction on the given OLCI L1b sample.
 */
public class SmileCorrectionAlgorithm {

    private SmileCorrectionAlgorithm() {
    }

    public static float correct(float sourceReflectance, float lowerReflectance, float upperReflectance,
                                float sourceTargetLambda, float lowerLambda, float upperLambda, float refCentralWaveLength) {
        double dl = (refCentralWaveLength - sourceTargetLambda) / (upperLambda - lowerLambda);
        double dr = (upperReflectance - lowerReflectance) * dl;
        return (float) (sourceReflectance + dr);
    }
}
