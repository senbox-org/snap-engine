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
package org.esa.snap.stac.extensions;

public class Instrument implements StacExtension {

    private Instrument() {}

    public final static String schema = "https://stac-extensions.github.io/instrument/v1.0.0/schema.json";

    public static final String platform = "platform";
    public static final String instruments = "instruments";
    public static final String constellation = "constellation";
    public static final String mission = "mission";

    public final static String instrument_mode = "instrument_mode";
    public final static String sensor_mode = "sensor_mode";
    public static final String gsd = "gsd";
}
