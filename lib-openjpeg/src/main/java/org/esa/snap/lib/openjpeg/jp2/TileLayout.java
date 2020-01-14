/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2013-2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.lib.openjpeg.jp2;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Norman Fomferra
 */
public class TileLayout {
    /**
     * Width of L1C tile
     */
    public final int width;
    /**
     * Height of L1C tile
     */
    public final int height;
    /**
     * Width of internal JP2 tiles
     */
    public final int tileWidth;
    /**
     * Height of internal JP2 tiles
     */
    public final int tileHeight;
    /**
     * Width of internal JP2 X-tiles
     */
    public final int numXTiles;
    /**
     * Number of internal JP2 Y-tiles
     */
    public final int numYTiles;
    public final int numResolutions;
    public int numBands;

    public int dataType = 0;

    public TileLayout(int width, int height, int tileWidth, int tileHeight, int numXTiles, int numYTiles, int numResolutions) {
        this.width = width;
        this.height = height;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.numXTiles = numXTiles;
        this.numYTiles = numYTiles;
        this.numResolutions = numResolutions;
        this.numBands = 1;
    }

    public TileLayout(int width, int height, int tileWidth, int tileHeight, int numXTiles, int numYTiles, int numResolutions, int dataType) {
        this(width,height,tileWidth,tileHeight, numXTiles, numYTiles, numResolutions);
        this.dataType = dataType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TileLayout)) return false;

        TileLayout that = (TileLayout) o;

        if (height != that.height) return false;
        if (numResolutions != that.numResolutions) return false;
        if (numXTiles != that.numXTiles) return false;
        if (numYTiles != that.numYTiles) return false;
        if (tileHeight != that.tileHeight) return false;
        if (tileWidth != that.tileWidth) return false;
        if (width != that.width) return false;
        if (dataType != that.dataType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        result = 31 * result + tileWidth;
        result = 31 * result + tileHeight;
        result = 31 * result + numXTiles;
        result = 31 * result + numYTiles;
        result = 31 * result + numResolutions;
        result = 31 * result + dataType;
        return result;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
