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
import org.esa.snap.core.util.math.VectorLookupTable;

/**
 * This class is a container for an arbitrary sized list of sst retrieval coefficients.
 */
public class ArcCoefficients {

    private String _name;
    private String _description;
    private VectorLookupTable _lut;

    /**
     * Constructs the object with default values
     */
    public ArcCoefficients(String name, String desc, VectorLookupTable lut) {
        Guardian.assertNotNull("name", name);
        _name = name;
        _description = desc;
        _lut = lut;
    }

    /**
     * Sets the name for this coefficient set
     */
    public final void setName(String name) {
        Guardian.assertNotNull("name", name);
        _name = name;
    }

    /**
     * Retrieves the name of this coefficient set
     */
    public final String getName() {
        return _name;
    }

    /**
     * Sets the description for this coefficient set
     */
    public final void setDescription(String desc) {
        Guardian.assertNotNull("description", desc);
        _description = desc;
    }

    /**
     * Retrieves the description of this coefficient set
     */
    public final String getDescription() {
        return _description;
    }

    /**
     * Sets the a coefficient set for the calculation of the nadir view sst on daytime
     */
    public void set_Coeffs(VectorLookupTable lut) {
        Guardian.assertNotNull("lut", lut);
        _lut = lut;
    }

    /**
     * Returns the current a parameter set for the calculation of the nadir view sst on daytime - or null if none is
     * set
     */
    public VectorLookupTable get_Coeffs() {
        return _lut;
    }

}
