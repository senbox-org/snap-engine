/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.ProductData;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;


/**
 * This utility class provides some date/time related methods.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see java.util.Date
 */
public class DateTimeUtils {

    /**
     * An ISO 8601 date/time format. This does not give UTC times.
     */
    public static final SimpleDateFormat ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    public static final SimpleDateFormat ISO_8601_UTC_FORMAT;

    static{
        ISO_8601_UTC_FORMAT = (SimpleDateFormat) ISO_8601_FORMAT.clone();
        final Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        ISO_8601_UTC_FORMAT.setCalendar(calendar);
    }

    /**
     * The number of days from noon Jan 1, 4713 BC (Proleptic Julian) to midnight 1/1/1970 AD (Gregorian).
     * 1/1/1970 is time zero for a java.util.Date.
     */
    public static final double JD_OFFSET = 2440587.5;
    /**
     * The Modified Julian Day (MJD) gives the number of days since midnight on November 17, 1858. This date
     * corresponds to {@code MJD_OFFSET = 2400000.5} days after day zero of the Julian calendar.
     */
    public static final double MJD_OFFSET = 2400000.5;


    /**
     * The number of hours per day.
     */
    public static final double HOURS_PER_DAY = 24.0;
    /**
     * The number of seconds per day.
     */
    public static final double SECONDS_PER_DAY = 3600.0 * HOURS_PER_DAY;
    /**
     * The number of milli-seconds per day.
     */
    public static final double MILLIS_PER_DAY = 1000.0 * SECONDS_PER_DAY;
    /**
     * The number of micro-seconds per day.
     */
    public static final double MICROS_PER_DAY = 1000.0 * MILLIS_PER_DAY;


    /**
     * Converts a julian day (JD) to a modified julian day (MJD) value.
     *
     * @param jd the julian day
     *
     * @return the modified julian day
     */
    public static double jdToMJD(double jd) {
        return jd - MJD_OFFSET;
    }

    /**
     * Converts a modified julian day (MJD) to a julian day (JD) value.
     *
     * @param mjd the modified julian day
     *
     * @return the julian day
     */
    public static double mjdToJD(double mjd) {
        return MJD_OFFSET + mjd;
    }

    /**
     * Converts a julian day (JD) to a UTC date/time value.
     * <p><i>Important note:</i> Due to the limitations of {@link java.util.Date java.util.Date} this method does not
     * take leap seconds into account.
     *
     * @param jd the julian day
     *
     * @return the UTC date/time
     */
    public static Date jdToUTC(double jd) {
        long millis = Math.round((jd - JD_OFFSET) * MILLIS_PER_DAY);
        return new Date(millis);
    }

    /**
     * Converts a UTC date/time value to a julian day (JD).
     * <p><i>Important note:</i> Due to the limitations of {@link java.util.Date java.util.Date} this method does not
     * take leap seconds into account.
     *
     * @param utc the UTC date/time, if {@code null} the current time is converted
     *
     * @return the julian day
     */
    public static double utcToJD(Date utc) {
        long millis = utc != null ? utc.getTime() : System.currentTimeMillis();
        return JD_OFFSET + millis / MILLIS_PER_DAY;
    }

    /**
     * Converts a UTC date/time value to a string. The method uses the ISO 8601 date/time format {@code YYYY-MM-DD
     * hh:mm:ss.S}
     * <p><i>Important note:</i> Due to the limitations of {@link java.util.Date java.util.Date} this method does not
     * take leap seconds into account.
     *
     * @param utc the UTC date/time value
     *
     * @return the UTC date/time string
     */
    public static String utcToString(Date utc) {
        return ISO_8601_UTC_FORMAT.format(utc != null ? utc : new Date());
    }

    /**
     * Converts a UTC date/time string to a UTC date/time value. The method uses the ISO 8601 date/time format
     * {@code YYYY-MM-DD hh:mm:ss.S}.
     * <p><i>Important note:</i> Due to the limitations of {@link java.util.Date java.util.Date} this method does not
     * take leap seconds into account.
     *
     * @param utc the UTC date/time string
     */
    public static Date stringToUTC(String utc) throws ParseException {
        return ISO_8601_UTC_FORMAT.parse(utc);
    }

    /**
     * Converts a UTC date/time calendar to a LocalDateTime. The method interpretes this UTC value as a MJD 2000 date
     * (Modified Julian Day where the  first day is the 01.01.2000).
     *
     * @param utc the UTC date/time calendar
     */
    public static LocalDateTime calendarToLocalDateTime(Calendar utc) {
        return LocalDateTime.ofInstant(utc.toInstant(), utc.getTimeZone().toZoneId());
    }

    /**
     * Computes the median (average) of two dates.
     * This method handles the possible overflow that can occur.
     *
     * @param startDate The first date
     * @param endDate   The second date
     * @return The date between the two input dates
     */
    public static Date average(Date startDate, Date endDate) {
        Date averageDate = null;
        if (startDate != null && endDate != null) {
            BigInteger averageMillis = BigInteger.valueOf(startDate.getTime())
                    .add(BigInteger.valueOf(endDate.getTime()))
                    .divide(BigInteger.valueOf(2L));
            averageDate = new Date(averageMillis.longValue());
        }
        return averageDate;
    }

    /**
     * Computes the median (average) of two <code>ProductData.UTC</code> data structures.
     *
     * @param startDate The first date
     * @param endDate   The second date
     * @return The date between the two input dates
     */
    public static ProductData.UTC average(ProductData.UTC startDate, ProductData.UTC endDate) {
        ProductData.UTC average = null;
        if (startDate != null && endDate != null) {
            BigInteger averageMillis = BigInteger.valueOf(startDate.getAsDate().getTime()).add(BigInteger.valueOf(endDate.getAsDate().getTime())).divide(BigInteger.valueOf(2L));
            Date averageDate = new Date(averageMillis.longValue());
            average = ProductData.UTC.create(averageDate, 0L);
        }
        return average;
    }

    /**
     * Utility method for returning a <code>ProductData.UTC</code> date from a string
     * using the given date format.
     * Why not using <code>ProductData.UTC.parse(text, pattern)</code> method?
     * Because it errors in the case of a format like dd-MM-yyyy'T'HH:mm:ss.SSSSSS (which should be
     * perfectly fine).
     *
     * @param stringData The string to be converted into a date
     * @param dateFormat The format of the string date
     * @return The UTC date representation.
     */
    public static ProductData.UTC parseDate(String stringData, String dateFormat) {
        ProductData.UTC parsedDate = null;
        if (stringData != null) {
            try {
                if (stringData.endsWith("Z"))
                    stringData = stringData.substring(0, stringData.length() - 1);
                if (!stringData.contains(".") && dateFormat.contains("."))
                    stringData = stringData + ".000000";
                Long microseconds = 0L;
                if (dateFormat.contains(".")) {
                    String stringMicroseconds = stringData.substring(stringData.indexOf(".") + 1);

                    //check the microseconds length
                    if (stringMicroseconds.length() > 6) {
                        //if there are more than 6 digits, the last ones are removed
                        stringMicroseconds = stringMicroseconds.substring(0, 6);
                    } else {
                        //fill until 6 digits
                        while (stringMicroseconds.length() < 6) stringMicroseconds = stringMicroseconds + "0";
                    }

                    microseconds = Long.parseLong(stringMicroseconds);
                    stringData = stringData.substring(0, stringData.lastIndexOf("."));
                    dateFormat = dateFormat.substring(0, dateFormat.lastIndexOf("."));
                }
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
                simpleDateFormat.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                Date date = simpleDateFormat.parse(stringData);
                parsedDate = ProductData.UTC.create(date, microseconds);
            } catch (ParseException e) {
                Logger.getLogger(DateTimeUtils.class.getName()).warning(String.format("Date not in expected format. Found %s, expected %s",
                        stringData,
                        dateFormat));
            }
        }
        return parsedDate;
    }
}
