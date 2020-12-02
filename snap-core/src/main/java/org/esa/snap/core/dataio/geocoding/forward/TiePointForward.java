/*
 *
 * Copyright (C) 2020 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.ForwardCoding;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.datamodel.TiePointGrid;

abstract class TiePointForward implements ForwardCoding {

    void checkGrids(TiePointGrid lonGrid, TiePointGrid latGrid) {
        if (lonGrid.getGridWidth() != latGrid.getGridWidth() ||
            lonGrid.getGridHeight() != latGrid.getGridHeight() ||
            lonGrid.getOffsetX() != latGrid.getOffsetX() ||
            lonGrid.getOffsetY() != latGrid.getOffsetY() ||
            lonGrid.getSubSamplingX() != latGrid.getSubSamplingX() ||
            lonGrid.getSubSamplingY() != latGrid.getSubSamplingY()) {
            throw new IllegalArgumentException("lonGrid is not compatible with latGrid");
        }
    }

    void checkGeoRaster(GeoRaster geoRaster) {
        if (geoRaster.getLongitudes().length != geoRaster.getLatitudes().length) {
            throw new IllegalArgumentException("lonGrid is not compatible with latGrid");
        }
    }

    @Override
    abstract public ForwardCoding clone();
}
