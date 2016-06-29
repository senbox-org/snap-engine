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

/**
 * @author muhammad.bc.
 */
public class RayleighConstants {
    static double AVOGADRO_NUMBER = 6.0221367E+23;
    static double MEAN_MOLECULAR_ZERO = 28.9595;
    static double ACCELERATION_GRAVITY_SEA_LEVEL_458_LATITUDE = 980.616;
    static double Molecular_cm3 = 2.5469E19;

    // constants describing the state of the atmosphere and which we don't know; better values may be used if known
    static double CO2 = 3.E-4; // CO2 concentration at pixel; typical values are 300 to 360 ppm
    static double C_CO2 = CO2 * 100;  // CO2 concentration in ppm
    static double MEAN_MOLECULAR_WEIGHT_C02 = 15.0556 * CO2 + MEAN_MOLECULAR_ZERO;

    static double PA = 0.9587256;
    static double PB = 1. - PA; // Rayleigh Phase function, molecular asymetry factor 2

}
