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

package org.esa.s3tbx.idepix.core.pixel;

/**
 * Abstract base class for pixel properties. Provides default implementations for isLand() and isWater(), using the
 * SRTM-shapefile-based land-water-mask.
 *
 * @author Thomas Storm
 */
public abstract class AbstractPixelProperties implements PixelProperties {

    public boolean isWater;
    public boolean usel1bLandWaterFlag;

    @Override
    public boolean isWater() {
        return !isInvalid() && isWater;
    }

    public void setIsWater(boolean isWater) {
        this.isWater = !isInvalid() && isWater;
    }
}
