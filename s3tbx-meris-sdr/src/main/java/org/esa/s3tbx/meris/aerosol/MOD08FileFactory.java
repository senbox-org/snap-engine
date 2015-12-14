/*
 * $Id: MOD08FileFactory.java,v 1.1 2007/03/27 12:52:21 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.aerosol;

import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

public class MOD08FileFactory implements TemporalFileFactory {
    private static final int AVG_PERIOD_IN_DAYS = 8;
    private static final long AVG_PERIOD_IN_MILLIS = AVG_PERIOD_IN_DAYS * (24L * 60L * 60L * 1000L);

    private static final String FILENAME_PART = "MOD08_E3";
    private static final String FILENAME_DATE_START = ".A";
    private static final String FILENAME_SUFFIX = ".hdf";

    public TemporalFile createTemporalFile(final File file) {
        final String filename = file.getName();
        if (!filename.endsWith(FILENAME_SUFFIX)) {
            return null;
        }

        if ((filename.indexOf(FILENAME_PART) == -1) ||
                filename.indexOf(FILENAME_DATE_START) == -1) {
            return null;
        }
        final int dateStart = filename.indexOf(FILENAME_DATE_START);
        final int yearOffset = dateStart + FILENAME_DATE_START.length();
        final int dayOffset = yearOffset + 4;
        int year;
        int dayOfYear;

        try {
            year = Integer.parseInt(filename.substring(yearOffset, yearOffset + 4));
            dayOfYear = Integer.parseInt(filename.substring(dayOffset, dayOffset + 3));
        } catch (NumberFormatException e) {
            return null;
        }
        final long startTime = computeTime(year, dayOfYear);
        final long endTime = startTime + AVG_PERIOD_IN_MILLIS - 1;
        return new TemporalFile(file, new Date(startTime), new Date(endTime));
    }

    private static long computeTime(final int year, final int dayOfYear) {
        final Calendar calendar = createUTCCalendar();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        return calendar.getTimeInMillis();
    }

    private static Calendar createUTCCalendar() {
        return ProductData.UTC.createCalendar();
    }
}
