/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.datamodel;

import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;


public class ProductDataUTCTest {

    @Test
    public void testParse() throws ParseException {
        final ProductData.UTC utc = ProductData.UTC.parse("05-Jan-2000 00:00:06.000007");
        assertEquals(4, utc.getElemIntAt(0));
        assertEquals(6, utc.getElemIntAt(1));
        assertEquals(7, utc.getElemIntAt(2));
    }

    @Test
    public void testFormat() {
        final ProductData.UTC utc = new ProductData.UTC(4, 6, 7);
        assertEquals(4, utc.getElemIntAt(0));
        assertEquals(6, utc.getElemIntAt(1));
        assertEquals(7, utc.getElemIntAt(2));
        assertEquals(4, utc.getDaysFraction());
        assertEquals(6, utc.getSecondsFraction());
        assertEquals(7, utc.getMicroSecondsFraction());
        assertEquals("05-JAN-2000 00:00:06.000007", utc.format());
    }

    @Test
    public void testProductDataTimeZone() {
        final ProductData.UTC data = ProductData.UTC.create(new Date(), 0);
        assertEquals(TimeZone.getTimeZone("UTC"), data.getAsCalendar().getTimeZone());
    }

    @Test
    public void testCreateDateFormatTimeZone() {
        final DateFormat format = ProductData.UTC.createDateFormat();
        assertEquals(TimeZone.getTimeZone("UTC"), format.getTimeZone());
    }
    @Test
    public void testCreateDateFormatTimeZoneWithPattern() {
        final DateFormat format = ProductData.UTC.createDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        assertEquals(TimeZone.getTimeZone("UTC"), format.getTimeZone());
    }

    @Test
    public void testMjdToUTCConversion() throws Exception {
        // these dates represent 3 consecutive scanline-times of a MERIS RR orbit
        ProductData.UTC utc1 = new ProductData.UTC(2923.999998208953);
        ProductData.UTC utc2 = new ProductData.UTC(2924.000000245851);
        ProductData.UTC utc3 = new ProductData.UTC(2924.0000022827494);

        assertEquals(2923, utc1.getDaysFraction());
        assertEquals(2924, utc2.getDaysFraction());
        assertEquals(2924, utc3.getDaysFraction());

        assertEquals(86399, utc1.getSecondsFraction());
        assertEquals(0, utc2.getSecondsFraction());
        assertEquals(0, utc3.getSecondsFraction());

        assertEquals(845253, utc1.getMicroSecondsFraction());
        assertEquals(21241, utc2.getMicroSecondsFraction());
        assertEquals(197229, utc3.getMicroSecondsFraction());

    }

    @Test
    public void testMjdAfter2000() throws Exception {
        final ProductData.UTC utc = ProductData.UTC.parse("02 Jul 2001 13:10:11", "dd MMM yyyy hh:mm:ss");
        final double mjd = utc.getMJD();
        final ProductData.UTC utc1 = new ProductData.UTC(mjd);
        assertEquals(utc1.getAsDate().getTime(), utc.getAsDate().getTime());
    }

    @Test
    public void testMjdBefore2000() throws Exception {
        final ProductData.UTC utc = ProductData.UTC.parse("02 Jul 1999 13:10:11", "dd MMM yyyy hh:mm:ss");
        final double mjd = utc.getMJD();
        final ProductData.UTC utc1 = new ProductData.UTC(mjd);
        assertEquals(utc1.getAsDate().getTime(), utc.getAsDate().getTime());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDateParsingNull() throws ParseException {
        ProductData.UTC.parse(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDateParsingEmptyString() throws ParseException {
        ProductData.UTC.parse("");
    }

    @Test
    public void testMerisDateParsing() throws ParseException {

        final TimeZone utcZone = TimeZone.getTimeZone("UTC");
        Calendar calendar = GregorianCalendar.getInstance(utcZone, Locale.ENGLISH);

        String janString = "03-JAN-2003 01:02:03.3456";
        Date date = ProductData.UTC.parse(janString).getAsDate();
        calendar.clear();
        calendar.setTime(date);
        assertEquals(3, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(1 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2003, calendar.get(Calendar.YEAR));
        assertEquals(1, calendar.get(Calendar.HOUR));
        assertEquals(2, calendar.get(Calendar.MINUTE));
        assertEquals(3, calendar.get(Calendar.SECOND));
        assertEquals(346, calendar.get(Calendar.MILLISECOND));

        String febString = "05-FEB-2002 02:03:04.67890";
        date = ProductData.UTC.parse(febString).getAsDate();
        calendar.clear();
        calendar.setTime(date);
        assertEquals(5, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(2 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2002, calendar.get(Calendar.YEAR));
        assertEquals(2, calendar.get(Calendar.HOUR));
        assertEquals(3, calendar.get(Calendar.MINUTE));
        assertEquals(4, calendar.get(Calendar.SECOND));
        assertEquals(679, calendar.get(Calendar.MILLISECOND));

        String marString = "06-MAR-2002 02:03:04.67890";
        date = ProductData.UTC.parse(marString).getAsDate();
        calendar.clear();
        calendar.setTime(date);
        assertEquals(6, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(3 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2002, calendar.get(Calendar.YEAR));
        assertEquals(2, calendar.get(Calendar.HOUR));
        assertEquals(3, calendar.get(Calendar.MINUTE));
        assertEquals(4, calendar.get(Calendar.SECOND));
        assertEquals(679, calendar.get(Calendar.MILLISECOND));

        String aprString = "07-APR-2004 04:06:22.32311";
        date = ProductData.UTC.parse(aprString).getAsDate();
        calendar.clear();
        calendar.setTime(date);
        assertEquals(7, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(4 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2004, calendar.get(Calendar.YEAR));
        assertEquals(4, calendar.get(Calendar.HOUR));
        assertEquals(6, calendar.get(Calendar.MINUTE));
        assertEquals(22, calendar.get(Calendar.SECOND));
        assertEquals(323, calendar.get(Calendar.MILLISECOND));

        String mayString = "08-MAY-2005 12:33:57.32311";
        date = ProductData.UTC.parse(mayString).getAsDate();
        calendar.clear();
        calendar.setTime(date);
        assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(5 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2005, calendar.get(Calendar.YEAR));
        assertEquals(12, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(33, calendar.get(Calendar.MINUTE));
        assertEquals(57, calendar.get(Calendar.SECOND));
        assertEquals(323, calendar.get(Calendar.MILLISECOND));

        String decString = "23-DEC-2004 22:16:43.556677";
        date = ProductData.UTC.parse(decString).getAsDate();
        calendar.clear();
        calendar.setTime(date);
        assertEquals(23, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(12 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2004, calendar.get(Calendar.YEAR));
        assertEquals(22, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(16, calendar.get(Calendar.MINUTE));
        assertEquals(43, calendar.get(Calendar.SECOND));
        assertEquals(557, calendar.get(Calendar.MILLISECOND));
    }

    @Test
    public void testGetCalendar() {
        Calendar calendar = ProductData.UTC.createCalendar();
        assertEquals(ProductData.UTC.UTC_TIME_ZONE, calendar.getTimeZone());
        assertEquals(946684800000L, calendar.getTimeInMillis());
    }

    @Test
    public void testGetAsDate() throws ParseException {
        Date date = ProductData.UTC.parse("23-DEC-2004 22:16:43.556677").getAsDate();
        Calendar calendar = ProductData.UTC.createCalendar();
        calendar.setTime(date);
        assertEquals(23, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(12 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2004, calendar.get(Calendar.YEAR));
        assertEquals(22, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(16, calendar.get(Calendar.MINUTE));
        assertEquals(43, calendar.get(Calendar.SECOND));
        assertEquals(557, calendar.get(Calendar.MILLISECOND));
    }

    @Test
    public void testParseAndFormat() throws ParseException {
        final String expected = "23-DEC-2004 22:16:43.556677";
        final ProductData.UTC utc = ProductData.UTC.parse(expected);
        assertEquals(expected, utc.format());
    }

    @Test
    public void testDoubleConstructor() throws ParseException {
        final String expected = "23-DEC-2004 22:16:43.556677";
        final ProductData.UTC utc = ProductData.UTC.parse(expected);
        final double mjd = utc.getMJD();
        final ProductData.UTC utc2 = new ProductData.UTC(mjd);
        assertEquals(expected, utc2.format());
    }
}
