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

/**
 * @author muhammad.bc.
 */
public class GaseousAbsorptionAlgorithm {

    public static double calMassAir(double sunAngle, double veiwAngle) {
        return 1 / Math.cos(sunAngle) + 1 / Math.cos(veiwAngle);
    }

    private double calAtmosphericGas(String bandName) {
        return 0;
    }

    //idea MPs use wavelength instance of name.
    private double calNormalizedConcentration(String bandName) {
        return 0;
    }


    public String[] gasToComputeForBand(String bandName) {
        GasToCompute gasToCompute = GasToCompute.valueOf(bandName);
        return gasToCompute.getGasBandToCompute();
    }

    public double calExponential(double atmosphericGas, double normConcentration, double massAir) {
        final double calValue = -atmosphericGas * normConcentration * massAir;
        return Math.exp(calValue);
    }


}
