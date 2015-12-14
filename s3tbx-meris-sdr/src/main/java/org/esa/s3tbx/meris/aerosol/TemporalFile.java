/*
 * $Id: TemporalFile.java,v 1.1 2007/03/27 12:52:21 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.aerosol;

import java.io.File;
import java.util.Date;

public class TemporalFile {
    protected final File file;
    protected final Date startDate;
    protected final Date endDate;

    public TemporalFile(File file, Date startDate, Date endDate) {
        this.file = file;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public File getFile() {
        return file;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Date getMeanDate() {
        return new Date((startDate.getTime() + endDate.getTime()) / 2L);
    }

    public static int compare(final Date date, final TemporalFile f1, final TemporalFile f2) {
        final long d1 = Math.abs(getMeanTimeDifference(date, f1));
        final long d2 = Math.abs(getMeanTimeDifference(date, f2));
        final long delta = d1 - d2;
        return delta == 0 ? 0 : delta > 0 ? 1 : -1;
    }

    public static long getMeanTimeDifference(final Date date, final TemporalFile file) {
        final long t = date.getTime();
        final long t1 = file.getStartDate().getTime();
        final long t2 = file.getEndDate().getTime();
        return t - (t1 + t2) / 2;
    }

    @Override
    public String toString() {
        return file.toString();
    }
}
