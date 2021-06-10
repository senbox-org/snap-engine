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

package org.esa.snap.dataio.znap.snap;

import org.esa.snap.core.datamodel.ProductData;

import java.text.DateFormat;
import java.text.ParseException;

public class ISO8601ConverterWithMlliseconds {

    private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private static final DateFormat FORMAT;
    private static final String SECONDS_PATTERN = PATTERN.substring(0, PATTERN.lastIndexOf("."));
    private static final DateFormat SECONDS_FORMAT;

    static {
        FORMAT = ProductData.UTC.createDateFormat(PATTERN);
        SECONDS_FORMAT = ProductData.UTC.createDateFormat(SECONDS_PATTERN);
    }

    public static ProductData.UTC parse(String iso8601String) throws ParseException {
        return ProductData.UTC.parse(iso8601String, SECONDS_FORMAT);
    }

    public static String format(ProductData.UTC utc) {
        return FORMAT.format(utc.getAsDate());
    }
}
