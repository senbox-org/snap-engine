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
package org.esa.snap.core.dataio;

import java.util.List;

/**
 * @author Oana H.
 */
public class ProductReaderExposedParams {

    // the usual band names that most of products of a certain type have
    List<String> bandNames;

    // the usual mask names that most of products of a certain type have
    List<String> maskNames;

    // a flag that indicates if the products of a certain type can have masks
    boolean hasMasks;

    public ProductReaderExposedParams(List<String> bandNames, boolean hasMasks) {
        this.bandNames = bandNames;
        this.hasMasks = hasMasks;
    }

    public ProductReaderExposedParams(List<String> bandNames, List<String> maskNames, boolean hasMasks) {
        this.bandNames = bandNames;
        this.maskNames = maskNames;
        this.hasMasks = hasMasks;
    }

    public List<String> getBandNames() {
        return bandNames;
    }

    public void setBandNames(final List<String> bandNames) {
        this.bandNames = bandNames;
    }

    public List<String> getMaskNames() {
        return maskNames;
    }

    public void setMaskNames(final List<String> maskNames) {
        this.maskNames = maskNames;
    }

    public boolean isHasMasks() {
        return hasMasks;
    }

    public void setHasMasks(final boolean hasMasks) {
        this.hasMasks = hasMasks;
    }
}
