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

import org.esa.snap.core.datamodel.ProductData;
import org.json.simple.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

public class DateTime implements StacExtension {

    public final static String datetime = "datetime";
    public final static String start_datetime = "start_datetime";
    public final static String end_datetime = "end_datetime";
    public final static String created = "created";
    public final static String updated = "updated";

    public static ProductData.UTC getStartTime(final JSONObject propertiesJSON) throws ParseException {
        if (propertiesJSON.containsKey(DateTime.start_datetime)) {
            return toUTC((String) propertiesJSON.get(DateTime.start_datetime));
        }
        if (propertiesJSON.containsKey(DateTime.datetime)) {
            return toUTC((String) propertiesJSON.get(DateTime.datetime));
        }
        return null;
    }

    public static ProductData.UTC getEndTime(final JSONObject propertiesJSON) throws ParseException {
        if (propertiesJSON.containsKey(DateTime.end_datetime)) {
            return toUTC((String) propertiesJSON.get(DateTime.end_datetime));
        }
        if (propertiesJSON.containsKey(DateTime.datetime)) {
            return toUTC((String) propertiesJSON.get(DateTime.datetime));
        }
        return null;
    }

    public static String getFormattedTime(final ProductData.UTC utc) {
        if (utc != null) {
            final Calendar calendar = ProductData.UTC.createCalendar();
            calendar.add(Calendar.DATE, utc.getDaysFraction());
            calendar.add(Calendar.SECOND, (int) utc.getSecondsFraction());
            final Date time = calendar.getTime();
            final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
            final String dateString = dateFormat.format(time);
            final String microsString = String.valueOf(utc.getMicroSecondsFraction());
            StringBuilder sb = new StringBuilder(dateString.toUpperCase());
            sb.append('.');
            for (int i = microsString.length(); i < 6; i++) {
                sb.append('0');
            }
            sb.append(microsString);
            sb.append('Z');
            return sb.toString().replace(' ', 'T');
        }
        return "unknown";
    }

    public static String getNowUTC() {
        return Instant.now().toString();
    }

    public static ProductData.UTC toUTC(String timeStr) throws ParseException {
        timeStr = timeStr.replace("Z", "").replace("T", " ");
        final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
        return ProductData.UTC.parse(timeStr, dateFormat);
    }
}
