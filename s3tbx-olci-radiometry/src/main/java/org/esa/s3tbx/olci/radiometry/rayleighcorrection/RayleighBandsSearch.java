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

import com.google.common.primitives.Doubles;
import java.util.ArrayList;
import java.util.List;

/**
 * @author muhammad.bc.
 */
public class RayleighBandsSearch {

    private final List<Double> rho_brr = new ArrayList<>();
    private final List<Double> rho_toaR = new ArrayList<>();
    private final List<Double> sphericalFactor = new ArrayList<>();
    private final List<Double> tR_thetaV = new ArrayList<>();
    private final List<Double> tR_thetaS = new ArrayList<>();
    private final List<Double> sARay = new ArrayList<>();

    public RayleighBandsSearch(List<RayleighBands> rayleighBands) {
        for (RayleighBands rayleighBand : rayleighBands) {
            rho_brr.add(rayleighBand.getRho_BRR());
            rho_toaR.add(rayleighBand.getRho_toaR());
            sphericalFactor.add(rayleighBand.getSphericalFactor());
            tR_thetaV.add(rayleighBand.getR_thetaV());
            tR_thetaS.add(rayleighBand.gettR_thetaS());
            sARay.add(rayleighBand.getsARay());
        }
    }

    public double[] getBandSamples(String targetBandName) {
        if (targetBandName.matches("rBRR_\\d{2}")) {
            return Doubles.toArray(rho_brr);
        } else if (targetBandName.matches("transSRay_\\d{2}")) {
            return Doubles.toArray(tR_thetaS);
        } else if (targetBandName.matches("transVRay_\\d{2}")) {
            return Doubles.toArray(tR_thetaV);
        } else if (targetBandName.matches("sARay_\\d{2}")) {
            return Doubles.toArray(sARay);
        } else if (targetBandName.matches("rtoaRay_\\d{2}")) {
            return Doubles.toArray(rho_toaR);
        } else if (targetBandName.matches("sphericalAlbedoFactor_\\d{2}")) {
            return Doubles.toArray(sphericalFactor);
        }
        throw new IllegalArgumentException("The target band didn't exist.");
    }
}
