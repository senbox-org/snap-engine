/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.znap.preferences;

public final class ZnapPreferencesConstants {

    public static final String PROPERTY_NAME_COMPRESSION_LEVEL = "znap.compression.level";
    public static final int DEFAULT_COMPRESSION_LEVEL = 3;

    public static final String PROPERTY_NAME_COMPRESSOR_ID = "znap.compressor.id";
    public static final String DEFAULT_COMPRESSOR_ID = "null";

    public static final String PROPERTY_NAME_USE_ZIP_ARCHIVE = "znap.use.zip.archive";
    public static final boolean DEFAULT_USE_ZIP_ARCHIVE = true;

    public static final String PROPERTY_NAME_BINARY_FORMAT = "znap.binary.format";
}
