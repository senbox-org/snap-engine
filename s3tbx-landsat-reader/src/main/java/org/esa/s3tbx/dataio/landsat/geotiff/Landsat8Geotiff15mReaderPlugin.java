/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.dataio.landsat.geotiff;

import org.esa.snap.core.dataio.ProductReader;

import java.util.Locale;

public class Landsat8Geotiff15mReaderPlugin extends AbstractLandsat8ScalingGeotiffReaderPlugin  {

    private static final String[] FORMAT_NAMES = new String[]{"Landsat8GeoTIFF15m"};
    private static final String READER_DESCRIPTION = "Landsat8 Data Products in 15m Resolution (GeoTIFF)";

    @Override
    public ProductReader createReaderInstance() {
        return new LandsatGeotiffReader(this, LandsatGeotiffReader.Resolution.L8_PANCHROMATIC);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String getDescription(Locale locale) {
        return READER_DESCRIPTION;
    }

}
