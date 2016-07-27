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
public class RayleighBands {

    public double rho_BRR;
    public double sphericalFactor;
    public double rho_toaR;
    public double tR_thetaV;
    public double sARay;
    public double tR_thetaS;

    public double getRho_BRR() {
        return rho_BRR;
    }

    public double getSphericalFactor() {
        return sphericalFactor;
    }

    public double getRho_toaR() {
        return rho_toaR;
    }

    public double gettR_thetaV() {
        return tR_thetaV;
    }

    public double getsARay() {
        return sARay;
    }

    public double gettR_thetaS() {
        return tR_thetaS;
    }
}

