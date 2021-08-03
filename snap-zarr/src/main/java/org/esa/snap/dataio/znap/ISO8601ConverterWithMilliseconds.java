/*
 *
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
 *
 */

package org.esa.snap.dataio.znap;

import org.esa.snap.core.datamodel.ProductData;

import java.text.DateFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;

public class ISO8601ConverterWithMilliseconds {

    private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private static final DateFormat FORMAT;
    private static final String SECONDS_PATTERN = PATTERN.substring(0, PATTERN.lastIndexOf("."));
    private static final DateFormat SECONDS_FORMAT;

    static {
        FORMAT = ProductData.UTC.createDateFormat(PATTERN);
        SECONDS_FORMAT = ProductData.UTC.createDateFormat(SECONDS_PATTERN);
    }

    public static ProductData.UTC parse(String iso8601String) throws ParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX");
        final TemporalAccessor accessor = formatter.parse(iso8601String);
        final ZonedDateTime time = ZonedDateTime.from(accessor);
        final Date date = Date.from(time.toInstant());
        return ProductData.UTC.create(date, 0);

        //return ProductData.UTC.parse(iso8601String, SECONDS_FORMAT);
    }

    public static String format(ProductData.UTC utc) {
        final Calendar asCalendar = utc.getAsCalendar();
        final LocalDateTime ldt = LocalDateTime.of(asCalendar.get(Calendar.YEAR),
                                                   asCalendar.get(Calendar.MONTH),
                                                   asCalendar.get(Calendar.DAY_OF_MONTH),
                                                   asCalendar.get(Calendar.HOUR_OF_DAY),
                                                   asCalendar.get(Calendar.MINUTE),
                                                   asCalendar.get(Calendar.SECOND),
                                                   (int) utc.getMicroSecondsFraction());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnX");
        return ldt.format(formatter);
        //return FORMAT.format(utc.getAsDate());
    }
}
