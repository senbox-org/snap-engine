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

    private final SmileCorrectionAuxdata auxdata;

    /**
     * Creates an instance of this class with the given auxiliary data.
     *
     * @param auxdata the auxiliary data
     */
    public SmileCorrectionAlgorithm(SmileCorrectionAuxdata auxdata) {
        this.auxdata = auxdata;
    }

//    loat correct = correctionAlgorithm.correct(sourceRef, refUpperBand, refLowerBand, lambdaLowerBand, lambdaUpperBand, lambdaActualBand, targetBandIndex)
    public float correct(float sourceRef, float r2, float r1, float l1, float l2, float l_a, int targetBandIndex) {
        final double[] refCentralWaveLengths = auxdata.getRefCentralWaveLenghts();
        double centralWaveLength = refCentralWaveLengths[targetBandIndex];
        double dl = (centralWaveLength - l_a) / (l2 - l1);
        double dr = (r2 - r1) * dl;

        return (float) (sourceRef + dr);
    }
}
