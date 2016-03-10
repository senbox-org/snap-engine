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
package org.esa.s3tbx.arc;

import org.esa.snap.core.util.Guardian;

import java.util.Vector;

/**
 * This class serves to hold a commplete set of ARC retrieval coefficients.
 */
public final class ArcCoefficientSet {

    private final Vector _coeffs;

    /**
     * Creates the object with default values
     */
    public ArcCoefficientSet() {
        _coeffs = new Vector();
    }

    /**
     * Adds coefficients to this set
     */
    public final void addCoefficients(ArcCoefficients coeffs) {
        Guardian.assertNotNull("coefficients", coeffs);
        _coeffs.add(coeffs);
    }

    /**
     * Retrieves the number of coefficients in this set
     */
    public final int getNumCoefficients() {
        return _coeffs.size();
    }

    /**
     * Retrieves the coefficients at the index passed in
     */
    public final ArcCoefficients getCoefficientsAt(int index) {
        return (ArcCoefficients) _coeffs.elementAt(index);
    }

}
