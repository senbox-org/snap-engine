package org.esa.snap.core.util;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class DateTimeUtilsTest {

    private static TimeZone defaultTZ;

    @BeforeClass
    public static void setUp() {
        defaultTZ = TimeZone.getDefault();
    }

    @AfterClass
    public static void tearDown() {
        TimeZone.setDefault(defaultTZ);
    }

    @Test
    public void testStringToUTC() throws ParseException {
        Date date = DateTimeUtils.stringToUTC("2012-05-30 02:55:00.000000");
        SimpleDateFormat format;

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        assertEquals("2012-05-30 02:55:00.0", format.format(date));

        TimeZone.setDefault(TimeZone.getTimeZone("CET"));
        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        assertEquals("2012-05-30 04:55:00.0", format.format(date));
    }

    @Test
    public void testUtcToString() throws ParseException {
        Date date = DateTimeUtils.stringToUTC("2012-05-30 02:55:00.0");

        String utcToString = DateTimeUtils.utcToString(date);

        assertEquals("2012-05-30 02:55:00.0", utcToString);

    }

    @Test
    @STTM("SNAP-3507")
    public void testAverage() throws ParseException{
        Date startDate = DateTimeUtils.stringToUTC("2023-05-18 12:40:00.0");
        Date endDate = DateTimeUtils.stringToUTC("2023-05-18 18:00:00.0");
        Date averageDate = DateTimeUtils.average(startDate, endDate);
        assertEquals(DateTimeUtils.stringToUTC("2023-05-18 15:20:00.0"), averageDate);

        ProductData.UTC startUTC = ProductData.UTC.parse("2023-05-18 12:40:00", "yyyy-MM-dd HH:mm:ss");
        ProductData.UTC endUTC = ProductData.UTC.parse("2023-05-18 18:00:00", "yyyy-MM-dd HH:mm:ss");
        ProductData.UTC avgUTC = DateTimeUtils.average(startUTC, endUTC);
        ProductData.UTC expectedAvgUTC = ProductData.UTC.parse("2023-05-18 15:20:00", "yyyy-MM-dd HH:mm:ss");
        assertEquals(expectedAvgUTC.getAsDate(), avgUTC.getAsDate());
    }
}