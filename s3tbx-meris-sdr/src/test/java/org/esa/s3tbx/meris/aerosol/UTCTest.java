/*
 * $Id: UTCTest.java,v 1.1 2007/03/27 12:52:23 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.aerosol;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class UTCTest {
    @Test
    public void testUTC() {
        final Calendar calendar = getCalendar();
        calendar.set(2005, Calendar.JUNE, 7, 18, 30, 15);
        final DateFormat dateTimeFormat = getDateTimeFormat();
        assertEquals("07.06.2005 18:30:15", dateTimeFormat.format(calendar.getTime()));
    }

    static DateFormat getDateTimeFormat() {
        final Calendar calendar = getCalendar();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.ENGLISH);
        dateFormat.setCalendar(calendar);
        return dateFormat;
    }

    static Calendar getCalendar() {
        return ProductData.UTC.createCalendar();
    }
}
