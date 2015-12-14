/*
 * $Id: ClimFileFactory.java,v 1.1 2007/03/27 12:52:21 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.aerosol;

import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

public class ClimFileFactory implements TemporalFileFactory {
    private static final int TIME1_OFFSET = 10;
    private static final int TIME2_OFFSET = 17;
    private static final String FILENAME_PREFIX = "CLIM_GADS_";
    private static final String FILENAME_SUFFIX = ".hdf";

    public TemporalFile createTemporalFile(final File file) {
        final String filename = file.getName();
        if (filename.length() < 21) {
            return null;
        }
        if (!filename.startsWith(FILENAME_PREFIX)) {
            return null;
        }
        if (!filename.endsWith(FILENAME_SUFFIX)) {
            return null;
        }
        int year1;
        int month1;
        int year2;
        int month2;
        try {
            year1 = Integer.parseInt(filename.substring(TIME1_OFFSET, TIME1_OFFSET + 4));
            month1 = Integer.parseInt(filename.substring(TIME1_OFFSET + 4, TIME1_OFFSET + 6));
            year2 = Integer.parseInt(filename.substring(TIME2_OFFSET, TIME2_OFFSET + 4));
            month2 = Integer.parseInt(filename.substring(TIME2_OFFSET + 4, TIME2_OFFSET + 6));
        } catch (NumberFormatException e) {
            return null;
        }
        final long startTime = computeTime(year1, month1);
        final long endTime = computeTime(year2, month2 + 1) - 1;
        return new TemporalFile(file, new Date(startTime), new Date(endTime));

    }

    private static long computeTime(final int year, final int month) {
        final Calendar calendar = createUTCCalendar();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        return calendar.getTimeInMillis();
    }

    private static Calendar createUTCCalendar() {
        return ProductData.UTC.createCalendar();
    }
}
