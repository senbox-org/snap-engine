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

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;

/**
 * Converts between {@code String} and {@link ProductData.UTC}.
 * The String representation is ISO8601 compliant. The formatted String follows the pattern: {@code yyyy-MM-dd'T'HH:mm:ss.SSSSSSX}.
 * With real numbers it looks like: {@code 2023-11-13T14:25:36.126935Z}
 */
public class ISO8601Converter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX");
    private static final int MILLI_TO_MICRO = 1000;


    public static ProductData.UTC parse(String iso8601String) throws ParseException {
        final TemporalAccessor accessor = FORMATTER.parse(iso8601String);
        final ZonedDateTime time = ZonedDateTime.from(accessor);
        final Date date = Date.from(time.toInstant());
        return ProductData.UTC.create(date, time.get(ChronoField.MICRO_OF_SECOND));

    }

    public static String format(ProductData.UTC utc) {
        final Calendar asCalendar = utc.getAsCalendar();
        final OffsetDateTime odt = OffsetDateTime.of(asCalendar.get(Calendar.YEAR),
                                                     asCalendar.get(Calendar.MONTH) + 1,
                                                     asCalendar.get(Calendar.DAY_OF_MONTH),
                                                     asCalendar.get(Calendar.HOUR_OF_DAY),
                                                     asCalendar.get(Calendar.MINUTE),
                                                     asCalendar.get(Calendar.SECOND),
                                                     (int) utc.getMicroSecondsFraction() * MILLI_TO_MICRO,
                                                     ZoneOffset.UTC);
        return odt.format(FORMATTER);
    }
}
