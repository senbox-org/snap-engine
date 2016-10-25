/*
 * $Id: TemporalFileArrayTest.java,v 1.1 2007/03/27 12:52:23 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.aerosol;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;

public class TemporalFileArrayTest {

    @Test
    public void testTemporalFilesSorted() {
        final Calendar calendar = UTCTest.getCalendar();
        final TemporalFileArray fileArray = TemporalFileArray.create(getMOD08FileNames(), new MOD08FileFactory());
        TemporalFile[] sortedFiles;

        calendar.clear();
        calendar.set(2003, Calendar.JANUARY, 1);
        sortedFiles = fileArray.getTemporalFilesSorted(calendar.getTime(), 2);
        assertEquals(2, sortedFiles.length);
        assertEquals("MOD08_E3.A2002361.004.2003005220416.hdf_FUB.hdf", sortedFiles[0].toString());
        assertEquals("MOD08_E3.A2003001.004.2003012031014.hdf_FUB.hdf", sortedFiles[1].toString());

        calendar.clear();
        calendar.set(2003, Calendar.JULY, 23, 0, 0, 0);
        sortedFiles = fileArray.getTemporalFilesSorted(calendar.getTime(), 4);
        assertEquals(4, sortedFiles.length);
        assertEquals("MOD08_E3.A2003201.004.2003219111946.hdf_FUB.hdf", sortedFiles[0].toString());
        assertEquals("MOD08_E3.A2003193.004.2003220110313.hdf_FUB.hdf", sortedFiles[1].toString());

        calendar.clear();
        calendar.set(2003, Calendar.JULY, 24, 0, 0, 0);
        sortedFiles = fileArray.getTemporalFilesSorted(calendar.getTime(), -1);
        assertEquals(47, sortedFiles.length);
        assertEquals("MOD08_E3.A2003201.004.2003219111946.hdf_FUB.hdf", sortedFiles[0].toString());
        assertEquals("MOD08_E3.A2003209.004.2003234194604.hdf_FUB.hdf", sortedFiles[1].toString());

        calendar.clear();
        calendar.set(2003, Calendar.JULY, 24, 0, 0, 1);
        sortedFiles = fileArray.getTemporalFilesSorted(calendar.getTime(), 3);
        assertEquals(3, sortedFiles.length);
        assertEquals("MOD08_E3.A2003201.004.2003219111946.hdf_FUB.hdf", sortedFiles[0].toString());
        assertEquals("MOD08_E3.A2003209.004.2003234194604.hdf_FUB.hdf", sortedFiles[1].toString());
    }

    static String[] getMOD08FileNames() {
        return new String[]{
                "MOD08_E3.A2002361.004.2003005220416.hdf_FUB.hdf",
                "MOD08_E3.A2003001.004.2003012031014.hdf_FUB.hdf",
                "MOD08_E3.A2003009.004.2003337033341.hdf_FUB.hdf",
                "MOD08_E3.A2003017.004.2003026013203.hdf_FUB.hdf",
                "MOD08_E3.A2003025.004.2003337033343.hdf_FUB.hdf",
                "MOD08_E3.A2003033.004.2003337033342.hdf_FUB.hdf",
                "MOD08_E3.A2003041.004.2003054170540.hdf_FUB.hdf",
                "MOD08_E3.A2003049.004.2003060233621.hdf_FUB.hdf",
                "MOD08_E3.A2003057.004.2003337041048.hdf_FUB.hdf",
                "MOD08_E3.A2003065.004.2003076023819.hdf_FUB.hdf",
                "MOD08_E3.A2003073.004.2003084092227.hdf_FUB.hdf",
                "MOD08_E3.A2003081.004.2003337044540.hdf_FUB.hdf",
                "MOD08_E3.A2003089.004.2003104030339.hdf_FUB.hdf",
                "MOD08_E3.A2003097.004.2003112222723.hdf_FUB.hdf",
                "MOD08_E3.A2003105.004.2003339080217.hdf_FUB.hdf",
                "MOD08_E3.A2003113.004.2003337044850.hdf_FUB.hdf",
                "MOD08_E3.A2003121.004.2003337044922.hdf_FUB.hdf",
                "MOD08_E3.A2003129.004.2003337044925.hdf_FUB.hdf",
                "MOD08_E3.A2003137.004.2003152195821.hdf_FUB.hdf",
                "MOD08_E3.A2003145.004.2003159065435.hdf_FUB.hdf",
                "MOD08_E3.A2003153.004.2003337093722.hdf_FUB.hdf",
                "MOD08_E3.A2003161.004.2003337094201.hdf_FUB.hdf",
                "MOD08_E3.A2003169.004.2003337093735.hdf_FUB.hdf",
                "MOD08_E3.A2003177.004.2003191085448.hdf_FUB.hdf",
                "MOD08_E3.A2003185.004.2003209174019.hdf_FUB.hdf",
                "MOD08_E3.A2003193.004.2003220110313.hdf_FUB.hdf",
                "MOD08_E3.A2003201.004.2003219111946.hdf_FUB.hdf",
                "MOD08_E3.A2003209.004.2003234194604.hdf_FUB.hdf",
                "MOD08_E3.A2003217.004.2003240042431.hdf_FUB.hdf",
                "MOD08_E3.A2003225.004.2003243162431.hdf_FUB.hdf",
                "MOD08_E3.A2003233.004.2003250062627.hdf_FUB.hdf",
                "MOD08_E3.A2003241.004.2003260033606.hdf_FUB.hdf",
                "MOD08_E3.A2003249.004.2003266091028.hdf_FUB.hdf",
                "MOD08_E3.A2003257.004.2003273164550.hdf_FUB.hdf",
                "MOD08_E3.A2003265.004.2003282123258.hdf_FUB.hdf",
                "MOD08_E3.A2003273.004.2003285153250.hdf_FUB.hdf",
                "MOD08_E3.A2003281.004.2003294164950.hdf_FUB.hdf",
                "MOD08_E3.A2003289.004.2003300020217.hdf_FUB.hdf",
                "MOD08_E3.A2003297.004.2003307173713.hdf_FUB.hdf",
                "MOD08_E3.A2003305.004.2003315004621.hdf_FUB.hdf",
                "MOD08_E3.A2003313.004.2003322135506.hdf_FUB.hdf",
                "MOD08_E3.A2003321.004.2003333053112.hdf_FUB.hdf",
                "MOD08_E3.A2003329.004.2003343085247.hdf_FUB.hdf",
                "MOD08_E3.A2003337.004.2003351231134.hdf_FUB.hdf",
                "MOD08_E3.A2003345.004.2003362171201.hdf_FUB.hdf",
                "MOD08_E3.A2003353.004.2004006235830.hdf_FUB.hdf",
                "MOD08_E3.A2003361.004.2004007003203.hdf_FUB.hdf",
        };
    }

}


