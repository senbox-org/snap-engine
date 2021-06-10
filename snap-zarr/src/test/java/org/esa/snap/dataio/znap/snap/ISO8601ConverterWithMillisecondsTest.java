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

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;


import java.text.ParseException;
import java.util.Calendar;

public class ISO8601ConverterWithMillisecondsTest {

    private static final int YEAR = 2023;
    private static final int MONTH = 11;
    private static final int DAY = 13;
    private static final int HOUR = 14;
    private static final int MINUTE = 25;
    private static final int SECOND = 36;
    private static final int MILLISECOND = 126;
    private static final String DATE_TIME_STRING = "" + YEAR + "-" + MONTH + "-" + DAY + "T" + HOUR + ":" + MINUTE + ":" + SECOND + "." + MILLISECOND;

    @Before
    public void setUp() throws Exception {
        assertThat(DATE_TIME_STRING, is(equalTo("2023-11-13T14:25:36.126")));
    }

    @Test
    public void parse() throws ParseException {
        //execution
        final ProductData.UTC parsedUTC = ISO8601ConverterWithMlliseconds.parse(DATE_TIME_STRING);

        assertThat(parsedUTC.getAsCalendar().get(Calendar.YEAR), is(equalTo(YEAR)));
        assertThat(parsedUTC.getAsCalendar().get(Calendar.MONTH) + 1, is(equalTo(MONTH)));
        assertThat(parsedUTC.getAsCalendar().get(Calendar.DAY_OF_MONTH), is(equalTo(DAY)));
        assertThat(parsedUTC.getAsCalendar().get(Calendar.HOUR_OF_DAY), is(equalTo(HOUR)));
        assertThat(parsedUTC.getAsCalendar().get(Calendar.MINUTE), is(equalTo(MINUTE)));
        assertThat(parsedUTC.getAsCalendar().get(Calendar.SECOND), is(equalTo(SECOND)));
        assertThat(parsedUTC.getAsCalendar().get(Calendar.MILLISECOND), is(equalTo(MILLISECOND)));
    }

    @Test
    public void format() throws ParseException {
        //preparation
        final ProductData.UTC parsedUTC = ISO8601ConverterWithMlliseconds.parse(DATE_TIME_STRING);

        //execution
        final String formattedUTC = ISO8601ConverterWithMlliseconds.format(parsedUTC);

        assertThat(formattedUTC, is(equalTo(DATE_TIME_STRING)));
    }
}