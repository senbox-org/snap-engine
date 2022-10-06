/*
 * Copyright (c) 2022.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.ProductData;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;

public class ISO8601ConverterTest {

    private static final int YEAR = 2023;
    private static final int MONTH = 11;
    private static final int DAY = 13;
    private static final int HOUR = 14;
    private static final int MINUTE = 25;
    private static final int SECOND = 36;
    private static final long MICROSECOND = 126935;
    private static final String ZONEOFFSET = "Z";
    private static final String DATE_TIME_STRING = "" + YEAR + "-" + MONTH + "-" + DAY + "T" + HOUR + ":" + MINUTE + ":" + SECOND + "." + MICROSECOND + ZONEOFFSET;

    @Before
    public void setUp() throws Exception {
        MatcherAssert.assertThat(DATE_TIME_STRING, Matchers.is(Matchers.equalTo("2023-11-13T14:25:36.126935Z")));
    }

    @Test
    public void parse() throws ParseException {
        //execution
        final ProductData.UTC parsedUTC = ISO8601Converter.parse(DATE_TIME_STRING);

        MatcherAssert.assertThat(parsedUTC.getAsCalendar().get(Calendar.YEAR), Matchers.is(Matchers.equalTo(YEAR)));
        MatcherAssert.assertThat(parsedUTC.getAsCalendar().get(Calendar.MONTH) + 1, Matchers.is(Matchers.equalTo(MONTH)));
        MatcherAssert.assertThat(parsedUTC.getAsCalendar().get(Calendar.DAY_OF_MONTH), Matchers.is(Matchers.equalTo(DAY)));
        MatcherAssert.assertThat(parsedUTC.getAsCalendar().get(Calendar.HOUR_OF_DAY), Matchers.is(Matchers.equalTo(HOUR)));
        MatcherAssert.assertThat(parsedUTC.getAsCalendar().get(Calendar.MINUTE), Matchers.is(Matchers.equalTo(MINUTE)));
        MatcherAssert.assertThat(parsedUTC.getAsCalendar().get(Calendar.SECOND), Matchers.is(Matchers.equalTo(SECOND)));
        MatcherAssert.assertThat(parsedUTC.getMicroSecondsFraction(), Matchers.is(Matchers.equalTo(MICROSECOND)));
    }

    @Test
    public void format() throws ParseException {
        //preparation
        final ProductData.UTC parsedUTC = ISO8601Converter.parse(DATE_TIME_STRING);

        //execution
        final String formattedUTC = ISO8601Converter.format(parsedUTC);

        MatcherAssert.assertThat(formattedUTC, Matchers.is(Matchers.equalTo(DATE_TIME_STRING)));
    }
}