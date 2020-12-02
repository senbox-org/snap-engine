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

package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;

public interface InverseCoding {

    /**
     * Returns the pixel coordinates as x/y for a given geographical position given as lat/lon.
     *
     * @param geoPos   the geographical position as lat/lon in the coordinate system determined by underlying CRS
     * @param pixelPos an instance of <code>Point</code> to be used as return value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     * @return the pixel co-ordinates as x/y
     */
    PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos);

    void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations);

    /**
     * An InverseCoding shall be instanced only by {@link ComponentFactory} using a {@link String} key.
     * Such an instance must be able to return the key, in order to persist the InverseCoding and recreate
     * such an instance via {@link ComponentFactory} if the {@link org.esa.snap.core.datamodel.Product} shall
     * be opened again.
     *
     * @return the key String used while instantiating via {@link ComponentFactory}
     */
    String getKey();

    void dispose();

    /**
     * Create a shallow copy of the InverseCoding, rasterdata is shared;
     *
     * @return the clone
     */
    InverseCoding clone();
}
