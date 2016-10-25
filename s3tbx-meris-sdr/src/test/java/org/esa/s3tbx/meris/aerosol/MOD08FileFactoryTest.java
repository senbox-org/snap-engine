/*
 * $Id: MOD08FileFactoryTest.java,v 1.1 2007/03/27 12:52:23 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.aerosol;

import org.junit.Test;

import java.io.File;
import java.text.DateFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MOD08FileFactoryTest {

    @Test
    public void testCreateTemporalFile() {
        final DateFormat dateFormat = UTCTest.getDateTimeFormat();
        final MOD08FileFactory ff = new MOD08FileFactory();
        File f;
        TemporalFile tf;

        // fub files
        f = new File("MOD08_E3.A2003001.004.2003337033342.hdf_FUB.hdf");
        tf = ff.createTemporalFile(f);
        assertNotNull(tf);
        assertEquals("01.01.2003 00:00:00", dateFormat.format(tf.getStartDate()));
        assertEquals("08.01.2003 23:59:59", dateFormat.format(tf.getEndDate()));

        f = new File("MOD08_E3.A2003169.004.2003343085247.hdf_FUB.hdf");
        tf = ff.createTemporalFile(f);
        assertNotNull(tf);
        assertEquals("18.06.2003 00:00:00", dateFormat.format(tf.getStartDate()));
        assertEquals("25.06.2003 23:59:59", dateFormat.format(tf.getEndDate()));

        f = new File("MOD08_E3.A2003365.004.2003343085247.hdf_FUB.hdf");
        tf = ff.createTemporalFile(f);
        assertNotNull(tf);
        assertEquals("31.12.2003 00:00:00", dateFormat.format(tf.getStartDate()));
        assertEquals("07.01.2004 23:59:59", dateFormat.format(tf.getEndDate()));

        // nasa files
        f = new File("MOD08PSMOD08_E3.A2005001.004.2005011171600.hdf");
        tf = ff.createTemporalFile(f);
        assertNotNull(tf);
        assertEquals("01.01.2005 00:00:00", dateFormat.format(tf.getStartDate()));

        f = new File("MOD08_E3_MOD08SUB.A2006025.0000.001.2006038141658.hdf");
        tf = ff.createTemporalFile(f);
        assertNotNull(tf);
        assertEquals("25.01.2006 00:00:00", dateFormat.format(tf.getStartDate()));
    }

}

