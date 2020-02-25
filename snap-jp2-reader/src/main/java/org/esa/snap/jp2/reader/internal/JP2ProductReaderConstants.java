/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
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

package org.esa.snap.jp2.reader.internal;

import java.io.File;

/**
 * Created by kraftek on 7/9/2015.
 */
public class JP2ProductReaderConstants {
    public static final Class[] INPUT_TYPES = new Class[] { String.class, File.class };
    public static final String[] FORMAT_NAMES = new String[] { "JPEG2000" };
    public static final String[] DEFAULT_EXTENSIONS = new String[] { ".jp2" };
    public static final String DESCRIPTION = "JPEG-2000 Files";
    public static final String JP2_INFO_FILE = "%s_dump.txt";
    public static final String TAG_RASTER_DIMENSIONS = "/featurecollection/featuremember/featurecollection/featuremember/rectifiedgridcoverage/rectifiedgriddomain/rectifiedgrid/limits/gridenvelope/@high";
    public static final String TAG_ORIGIN = "/featurecollection/featuremember/featurecollection/featuremember/rectifiedgridcoverage/rectifiedgriddomain/rectifiedgrid/origin/point/@pos";
    public static final String TAG_OFFSET_VECTOR = "/featurecollection/featuremember/featurecollection/featuremember/rectifiedgridcoverage/rectifiedgriddomain/rectifiedgrid/@offsetVector";
    public static final String TAG_CRS_NAME = "/featurecollection/featuremember/featurecollection/featuremember/rectifiedgridcoverage/rectifiedgriddomain/rectifiedgrid/origin/point/@srsname";
    public static final String TYPE = "JPEG-2000";
    public static final String TAG_BAND_NAME = "/featurecollection/featuremember/featurecollection/featuremember/metadataproperty/genericmetadata/band/@name";
    public static final String TAG_BAND_SCALE = "/featurecollection/featuremember/featurecollection/featuremember/metadataproperty/genericmetadata/band/@scalefactor";
    public static final String TAG_BAND_OFFSET = "/featurecollection/featuremember/featurecollection/featuremember/metadataproperty/genericmetadata/band/@offset";
    public static final String TAG_LOWER_CORNER = "/featurecollection/boundedBy/Envelope/@lowerCorner";
    public static final String TAG_UPPER_CORNER = "/featurecollection/boundedBy/Envelope/@upperCorner";
    public static final String TAG_POLYGON_POSITIONS = "/featurecollection/boundedBy/Envelope/Polygon/exterior/LinearRing/@posList";
    public static final String TAG_CRS_NAME_VARIANT = "/Polygon/@srsName";
}
