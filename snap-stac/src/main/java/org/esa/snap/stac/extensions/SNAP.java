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

public class SNAP implements StacExtension {

    private SNAP() {}

    public final static String snap = "snap";

    public final static String product_type = "snap:product_type";
    public final static String quicklook_band_name = "snap:quicklook_band_name";

    public final static String virtual_band = "snap:virtual_band";
    public final static String expression = "snap:expression";
    public final static String valid_mask_term = "snap:valid_mask_term";
    public final static String filter_band_info = "snap:filter_band_info";

    // geocodings
    public final static String geocoding = "snap:geocoding";
    public final static String geocoding_class = "snap:geocoding_class";
    public final static String crs = "crs";
    public final static String geoposition_points = "geoposition_points";
    public final static String tpg_lat = "tpg_lat_name";
    public final static String tpg_lon = "tpg_lon_name";

    // datum
    public final static String datum_horizontal_datum_name = "horizontal_datum_name";
    public final static String datum_ellipsoid = "ellipsoid";
    public final static String datum_ellipsoid_name = "ellipsoid_name";
    public final static String datum_ellipsoid_maj_axis = "ellipsoid_maj_axis";
    public final static String datum_ellipsoid_min_axis = "ellipsoid_min_axis";

    // tie point grids
    public final static String tie_point_grids = "snap:tie_point_grids";
    public final static String tpg_description = "tie_point_description";
    public final static String tpg_physical_unit = "physical_unit";
    public final static String tpg_name = "tie_point_grid_name";
    public final static String tpg_data_type = "data_type";
    public final static String tpg_ncols = "ncols";
    public final static String tpg_nrows = "nrows";
    public final static String tpg_offset_x = "offset_x";
    public final static String tpg_offset_y = "offset_y";
    public final static String tpg_step_x = "step_x";
    public final static String tpg_step_y = "step_y";
    public final static String tpg_data = "data";
    public final static String tpg_discontinuity = "discontinuity";

    // flag codings
    public final static String flag_coding = "snap:flag_coding";
    public final static String flag = "flag";
    public final static String flag_name = "flag_name";
    public final static String flag_index = "flag_index";
    public final static String flag_description = "flag_description";

    // index codings
    public final static String index_coding = "snap:index_coding";
    public final static String index = "index";
    public final static String index_name = "index_name";
    public final static String index_value = "index_value";
    public final static String index_description = "index_description";

    // masks
    public final static String masks = "snap:masks";
    public final static String mask = "mask";
    public final static String mask_name = "name";
    public final static String mask_description = "description";
    public final static String mask_transparency = "transparency";
    public final static String mask_raster_width = "mask_raster_width";
    public final static String mask_raster_height = "mask_raster_height";
    public final static String mask_colour = "colour";
    public final static String mask_vector_data = "vector_data";
    public final static String mask_expression = "expression";
    public final static String mask_min = "min";
    public final static String mask_max = "max";
    public final static String mask_raster = "raster";

    // metadata tags
    public final static String name = "name";
    public final static String unit = "unit";
    public final static String type = "type";
    public final static String value = "value";
    public final static String red = "red";
    public final static String green = "green";
    public final static String blue = "blue";
    public final static String alpha = "alpha";
}
