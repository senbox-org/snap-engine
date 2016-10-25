/*
 * $Id: ClimFileFactoryTest.java,v 1.1 2007/03/27 12:52:23 marcoz Exp $
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

public class ClimFileFactoryTest {

    @Test
    public void testCreateTemporalFile() {
        final DateFormat dateFormat = UTCTest.getDateTimeFormat();
        final ClimFileFactory ff = new ClimFileFactory();
        File f;
        TemporalFile tf;

        f = new File("CLIM_GADS_200310_200403_FUB.hdf");
        tf = ff.createTemporalFile(f);
        assertNotNull(tf);
        assertEquals("01.10.2003 00:00:00", dateFormat.format(tf.getStartDate()));
        assertEquals("31.03.2004 23:59:59", dateFormat.format(tf.getEndDate()));

        f = new File("CLIM_GADS_200304_200309_FUB.hdf");
        tf = ff.createTemporalFile(f);
        assertNotNull(tf);
        assertEquals("01.04.2003 00:00:00", dateFormat.format(tf.getStartDate()));
        assertEquals("30.09.2003 23:59:59", dateFormat.format(tf.getEndDate()));
    }
}

