/*
 * $Id: WaterProcessorOzone.java, MS0610151415
 *
 * Copyright (C) 2005/7 by WeW (michael.schaale@wew.fu-berlin.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
 
/*
//#                               #
//# STANDARD IS 344 DU = 0.344 cm #
//#                               #
*/

package org.esa.s3tbx.fub.wew.util;

public class WaterProcessorOzone {

    private WaterProcessorOzone() {
    }

    public static double O3tau(double l, double O3du) {
        return (O3du / 1000.0 * O3excoeff(l));
    }

    public static double O3excoeff(double l) {
        int no3data = 69;

        // Wavelength [nm]
        double[] lambda = {
                +2.050000e+02, +2.100000e+02, +2.150000e+02, +2.200000e+02,
                +2.250000e+02, +2.300000e+02, +2.350000e+02, +2.400000e+02,
                +2.450000e+02, +2.500000e+02, +2.550000e+02, +2.600000e+02,
                +2.650000e+02, +2.700000e+02, +2.750000e+02, +2.800000e+02,
                +2.850000e+02, +2.900000e+02, +2.950000e+02, +3.000000e+02,
                +3.050000e+02, +3.100000e+02, +3.150000e+02, +3.200000e+02,
                +3.250000e+02, +3.300000e+02, +3.350000e+02, +3.400000e+02,
                +3.450000e+02, +3.500000e+02, +3.550000e+02, +3.600000e+02,
                +3.650000e+02, +3.800000e+02, +4.000000e+02, +4.200000e+02,
                +4.400000e+02, +4.500000e+02, +4.600000e+02, +4.700000e+02,
                +4.800000e+02, +4.900000e+02, +5.000000e+02, +5.100000e+02,
                +5.200000e+02, +5.300000e+02, +5.400000e+02, +5.500000e+02,
                +5.600000e+02, +5.700000e+02, +5.800000e+02, +5.900000e+02,
                +6.000000e+02, +6.100000e+02, +6.200000e+02, +6.300000e+02,
                +6.400000e+02, +6.500000e+02, +6.600000e+02, +6.700000e+02,
                +6.800000e+02, +6.900000e+02, +7.000000e+02, +7.100000e+02,
                +7.200000e+02, +7.300000e+02, +7.400000e+02, +7.500000e+02,
                +1.800000e+03};

        // Ozone extinction coefficient [1/cm]
        double[] excoeff = {
                +9.500000e+00, +1.450000e+01, +2.600000e+01, +4.590000e+01,
                +7.580000e+01, +1.162000e+02, +1.615000e+02, +2.111000e+02,
                +2.560000e+02, +2.839000e+02, +2.929000e+02, +2.829000e+02,
                +2.489000e+02, +2.015000e+02, +1.470000e+02, +9.900000e+01,
                +6.060000e+01, +3.460000e+01, +1.920000e+01, +9.800000e+00,
                +4.900000e+00, +2.640000e+00, +1.170000e+00, +6.220000e-01,
                +3.020000e-01, +1.440000e-01, +7.650000e-02, +3.810000e-02,
                +1.590000e-02, +6.310000e-03, +2.370000e-03, +1.250000e-03,
                +5.200000e-04, +1.000000e-07, +1.000000e-07, +1.000000e-07,
                +2.400000e-03, +3.600000e-03, +6.500000e-03, +8.800000e-03,
                +1.600000e-02, +1.970000e-02, +3.000000e-02, +3.900000e-02,
                +4.600000e-02, +6.300000e-02, +7.400000e-02, +8.400000e-02,
                +9.600000e-02, +1.150000e-01, +1.160000e-01, +1.100000e-01,
                +1.210000e-01, +1.210000e-01, +1.040000e-01, +9.000000e-02,
                +7.900000e-02, +6.300000e-02, +5.500000e-02, +4.300000e-02,
                +3.400000e-02, +2.700000e-02, +2.300000e-02, +1.900000e-02,
                +1.500000e-02, +1.200000e-02, +1.100000e-02, +1.000000e-02,
                +1.000000e-06};

        return (inpol(lambda, excoeff, no3data, l));
    }


    private static double inpol(double[] x, double[] y, int n, double xi) {
        int found = 0, i;

        for (i = 0; i < n - 1 && (found == 0); i++) {
            if (xi >= x[i] && xi <= x[i + 1]) {
                found = 1;
                return (y[i] + (xi - x[i]) * (y[i + 1] - y[i]) / (x[i + 1] - x[i]));
            }
        }
        return (0.0 / 0.0); // NaN
    }
}

