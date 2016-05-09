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

import java.util.ArrayList;

/**
 * @author muhammad.bc.
 */
public class GaseousAbsorptionAlgorithm {

    private float getAtmosphericGas(String bandName) {
        return 0;
    }


    //idea MPs use wavelength instance of name.
    private float getNormalizedConcentration(String bandName) {
        return 0;
    }

    private float getExponential(float atmosphericGas, float normConcentration, float massAir) {
        // to be confirm how the coofficient?
        final double calValue = -atmosphericGas * normConcentration * massAir;
        return (float) Math.exp(calValue);
    }


    public String[] gasToComputeForBand(String bandName) {
        GasToCompute gasToCompute = GasToCompute.valueOf(bandName);
        return gasToCompute.getGasBandToCompute();
    }

    public static float[] getMassAir(float[] sza, float[] sva) {
        float[] massAirs = new float[sza.length];
        for (int i = 0; i < sza.length; i++) {
            massAirs[i] = (float) (1 / Math.cos(sza[i]) + 1 / Math.cos(sva[i]));
        }
        return massAirs;
    }

    public float[] getTransmissionGas(String bandName, float[] sza, float[] ova) {
        float[] calMassAirs = getMassAir(sza, ova);
        String[] gasesToCompute = gasToComputeForBand(bandName);
        final ArrayList<float[]> arrayListExponential = new ArrayList();

        for (String gas : gasesToCompute) {
            final float calAtmosphericGas = getAtmosphericGas(gas);
            final float normalizedConcentration = getNormalizedConcentration(gas);
            final float[] calExponential = new float[ova.length];

            for (int i = 0; i < sza.length; i++) {
                calExponential[i] = getExponential(calAtmosphericGas, normalizedConcentration, calMassAirs[i]);
            }
            arrayListExponential.add(calExponential);
        }

        final int size = arrayListExponential.size();
        float[] transmissionGas = new float[0];
        if (size == 1) {
            transmissionGas = arrayListExponential.get(0);
        } else if (size == 2) {
            float[] gas_1 = arrayListExponential.get(0);
            float[] gas_2 = arrayListExponential.get(1);
            transmissionGas = SmileUtils.multiple2ArrayFloat(gas_1, gas_2);
        } else if (size == 3) {
            transmissionGas = SmileUtils.multiple3ArrayFloat(arrayListExponential.get(0), arrayListExponential.get(1), arrayListExponential.get(2));
        }
        return transmissionGas;
    }


    public static double getMassAir(double sunAngle, double veiwAngle) {
        return 1 / Math.cos(sunAngle) + 1 / Math.cos(veiwAngle);
    }
}
