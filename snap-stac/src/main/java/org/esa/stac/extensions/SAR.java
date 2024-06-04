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

public class SAR implements StacExtension {

    private SAR() {
    }

    public final static String sar = "sar";
    public final static String schema = "https://stac-extensions.github.io/sar/v1.0.0/schema.json";

    //string	REQUIRED. The name of the sensor acquisition mode that is commonly used. This should be the short name, if available. For example, WV for "Wave mode" of Sentinel-1 and Envisat ASAR satellites.
    public final static String instrument_mode = "sar:instrument_mode";

    //string	REQUIRED. The common name for the frequency band to make it easier to search for bands across instruments. See section "Common Frequency Band Names" for a list of accepted names.
    public final static String frequency_band = "sar:frequency_band";

    //number	The center frequency of the instrument, in gigahertz (GHz).
    public final static String center_frequency = "sar:center_frequency";

    //[string]	REQUIRED. Any combination of polarizations.
    public final static String polarizations = "sar:polarizations";

    //string	REQUIRED. The product type, for example SSC, MGD, or SGC
    public final static String product_type = "sar:product_type";

    //number	The range resolution, which is the maximum ability to distinguish two adjacent targets perpendicular to the flight path, in meters (m).
    public final static String resolution_range = "sar:resolution_range";

    //number	The azimuth resolution, which is the maximum ability to distinguish two adjacent targets parallel to the flight path, in meters (m).
    public final static String resolution_azimuth = "sar:resolution_azimuth";

    //number	The range pixel spacing, which is the distance between adjacent pixels perpendicular to the flight path, in meters (m). Strongly RECOMMENDED to be specified for products of type GRD.
    public final static String pixel_spacing_range = "sar:pixel_spacing_range";

    //number	The azimuth pixel spacing, which is the distance between adjacent pixels parallel to the flight path, in meters (m). Strongly RECOMMENDED to be specified for products of type GRD.
    public final static String pixel_spacing_azimuth = "sar:pixel_spacing_azimuth";

    //number	Number of range looks, which is the number of groups of signal samples (looks) perpendicular to the flight path.
    public final static String looks_range = "sar:looks_range";

    //number	Number of azimuth looks, which is the number of groups of signal samples (looks) parallel to the flight path.
    public final static String looks_azimuth = "sar:looks_azimuth";

    //number	The equivalent number of looks (ENL).
    public final static String looks_equivalent_number = "sar:looks_equivalent_number";

    //string	Antenna pointing direction relative to the flight trajectory of the satellite, either left or right.
    public final static String observation_direction = "sar:observation_direction";


    public static class KeyWords {
        private KeyWords() {
        }

        public final static String sar = "sar";
    }
}
