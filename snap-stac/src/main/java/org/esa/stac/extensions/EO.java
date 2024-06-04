/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.stac.extensions;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.stac.internal.JsonUtils;
import org.json.simple.JSONObject;

// Electro-Optical Extension
@SuppressWarnings("unchecked")
public class EO implements StacExtension {

    private EO() {}

    public final static String eo = "eo";
    public final static String schema = "https://stac-extensions.github.io/eo/v1.0.0/schema.json";

    //This is a list of the available bands where each item is a Band Object.
    public final static String bands = "eo:bands";

    //Estimate of cloud cover as a percentage (0-100) of the entire scene. If not available the field should not be provided.
    public final static String cloud_cover = "eo:cloud_cover";

    public final static String name = "name";
    public final static String common_name = "common_name";
    public final static String description = "description";
    public final static String center_wavelength = "center_wavelength";
    public final static String full_width_half_max = "full_width_half_max";

    //Common band names
    public final static String coastal = "coastal";
    public final static String blue = "blue";
    public final static String green = "green";
    public final static String red = "red";
    public final static String yellow = "yellow";
    public final static String pan = "pan";
    public final static String rededge = "rededge";
    public final static String nir = "nir";
    public final static String nir08 = "nir08";
    public final static String nir09 = "nir09";
    public final static String cirrus = "cirrus";
    public final static String swir16 = "swir16";
    public final static String swir22 = "swir22";
    public final static String lwir = "lwir";
    public final static String lwir11 = "lwir11";
    public final static String lwir12 = "lwir12";

    public static class KeyWords {
        private KeyWords() {}

        public final static String earth_observation = "earth observation";
        public final static String satellite = "satellite";
        public final static String optical = "optical";
    }

    public static String getCommonName(final String name) {
        switch (name) {
            case "coastal":
                return coastal;
            case "red":
                return red;
            case "green":
                return green;
            case "blue":
                return blue;
            case "yellow":
                return yellow;
            case "pan":
            case "panchromatic":
                return pan;
            case "rededge":
            case "red-edge":
            case "red_edge":
                return rededge;
            case "nir":
            case "near-infrared":
            case "near_infrared":
            case "nearinfrared":
                return nir;
            case "nir08":
                return nir08;
            case "nir09":
                return nir09;
            case "cirrus":
                return cirrus;
            case "swir":
            case "swir16":
            case "swir1":
            case "shortwave-infrared1":
                return swir16;
            case "swir22":
            case "swir2":
            case "shortwave-infrared2":
                return swir22;
            case "lwir":
            case "longwave-infrared":
                return lwir;
            case "lwir11":
            case "lwir1":
            case "longwave-infrared11":
                return lwir11;
            case "lwir12":
            case "lwir2":
            case "longwave-infrared12":
                return lwir12;
            default:
                return name;
        }
    }

    public static void getBandProperties(final Band band, final JSONObject bandProperties) {
        if(bandProperties.containsKey(Raster.unit)) {
            band.setUnit((String)bandProperties.get(Raster.unit));
        }
        if(bandProperties.containsKey(Raster.nodata)) {
            band.setNoDataValue(JsonUtils.getDouble(bandProperties.get(Raster.nodata)));
            band.setNoDataValueUsed(true);
        }
    }

    public static JSONObject writeBand(final Band band) {
        final String commonName = EO.getCommonName(band.getName());
        final int halfBW = (int)(band.getSpectralBandwidth()/2);
        final int minWL = (int)(band.getSpectralWavelength()-halfBW);
        final int maxWL = (int)(band.getSpectralWavelength()+halfBW);

        final JSONObject bandJSON = new JSONObject();
        bandJSON.put(EO.name, band.getName());
        bandJSON.put(EO.common_name, commonName);
        if(band.getDescription() != null) {
            bandJSON.put(EO.description, band.getDescription());
        } else if(halfBW > 0) {
            bandJSON.put(EO.description, commonName + " "+minWL + "-" + maxWL + " nm");
        }

        bandJSON.put(Raster.data_type, ProductData.getTypeString(band.getDataType()));
        final String unit = band.getUnit();
        if (unit != null && unit.length() > 0) {
            bandJSON.put(Raster.unit, unit);
        }
        bandJSON.put(EO.center_wavelength, band.getSpectralWavelength()/1000);
        bandJSON.put(EO.full_width_half_max, band.getSpectralBandwidth()/1000);

      /*  bandJSON.put(TAG_SOLAR_FLUX, band.getSolarFlux());
        if (band.getSpectralBandIndex() > -1) {
            bandJSON.put(TAG_SPECTRAL_BAND_INDEX, band.getSpectralBandIndex());
        }
        final FlagCoding flagCoding = band.getFlagCoding();
        if (flagCoding != null) {
            bandJSON.put(TAG_FLAG_CODING_NAME, flagCoding.getName());
        }
        final IndexCoding indexCoding = band.getIndexCoding();
        if (indexCoding != null) {
            bandJSON.put(TAG_INDEX_CODING_NAME, indexCoding.getName());
        }
        bandJSON.put(TAG_SCALING_FACTOR, band.getScalingFactor());
        bandJSON.put(TAG_SCALING_OFFSET, band.getScalingOffset());
        bandJSON.put(TAG_SCALING_LOG_10, band.isLog10Scaled());*/
        if(band.isNoDataValueUsed()) {
            bandJSON.put(Raster.nodata, band.getNoDataValue());
        }
        if (band instanceof VirtualBand) {
            final VirtualBand vb = (VirtualBand) band;
            bandJSON.put(SNAP.virtual_band, true);
            bandJSON.put(SNAP.expression, vb.getExpression());
        }
        final String validMaskExpression = band.getValidPixelExpression();
        if (validMaskExpression != null) {
            bandJSON.put(SNAP.valid_mask_term, validMaskExpression);
        }

        return bandJSON;
    }
}
