package org.esa.s3tbx.idepix.core.util;

import org.esa.snap.core.util.math.MathUtils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Class for calculating the position of the Sun.
 *
 * @author olafd
 */
public class SunPositionCalculator {

    private static final double RAD_PER_DEG = Math.PI / 180.0;
    private static final double SEC_PER_DAY = 86400;
    private static final double DAY_PER_YEAR = 365.25;
    private static final double DAY_PER_LYEAR = 366.0;
    private static final int SUMSOL = 172;   /* Julian date of the summer solstice */

    // Days Per Month
    private static final int[] dpm = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    // Days Per Month for Leap year
    private static final int[] ldpm = new int[]{31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    //
    private static final double[] gha = new double[]{179.23, 176.63, 176.88, 179.0, 180.72, 180.56, 179.06,
            178.4, 179.98, 182.56, 184.91, 182.75};

    /**
     * Based on C code of D.Hollaren for LAS5.0 software (1993)
     * <p/>
     * The angular position of the Sun is calculated with an accuracy of 1-2 arcmin.
     *
     * @param calendar the date/time of observation
     * @return the position of the Sun (lat/lon in degrees)
     */
    public static SunPosition calculate(final Calendar calendar) {
        final Calendar utc = toUTC(calendar);

        final int year = utc.get(Calendar.YEAR);
        int month = utc.get(Calendar.MONTH) + 1;
        final int date = utc.get(Calendar.DATE);
        final int day = utc.get(Calendar.DAY_OF_MONTH);
        final int hour = utc.get(Calendar.HOUR_OF_DAY);
        final int minute = utc.get(Calendar.MINUTE);
        final int second = utc.get(Calendar.SECOND);
        final double h = hour + minute / 60.0 + second / 3600.0;

        // Julian date
//        long jdate = toJulianDay(year, month, day);
        long jdate = utc.get(Calendar.DAY_OF_YEAR);      // !!!!

        // Total seconds from midnight in UTC
        double tsec = hour * 3600.0 + minute * 60.0 + second;

        // Calculate the earth rotation rate
        final double we = 0.0041666678 * RAD_PER_DEG;

        // Calculate fractional portion of the julian date
        double fjd = jdate + tsec / SEC_PER_DAY;

        // Determine if time went over the next year and subtract a years worth
        // of days if it did.
        if (fjd > DAY_PER_YEAR) {
            if (IdepixUtils.isLeapYear(year) && (fjd > DAY_PER_LYEAR)) {
                fjd -= DAY_PER_LYEAR;
            } else {
                fjd -= DAY_PER_YEAR;
            }
        }

        // Subtract one for indexing
        double djd = fjd - 1.0;

        // Find the month and calculate the day of the month
        double dmo = 0.0;  // todo: is this ok??
        if (IdepixUtils.isLeapYear(year)) {
            for (int i = 0; i < 12; i++) {
                djd -= ldpm[i];
                if (djd < 0.0) {
                    month = i + 1;
                    dmo = (ldpm[i] + djd) / ldpm[i];
                    break;
                }
            }
        } else {
            for (int i = 0; i < 12; i++) {
                djd -= dpm[i];
                if (djd < 0.0) {
                    month = i + 1;
                    dmo = (dpm[i] + djd) / dpm[i];
                    break;
                }
            }
        }

        //  Save the index into the Greenwich hour angle array
        int ist = month;
        int iend = month + 1;
        if (month == 12) {
            ist = 12;
            iend = 1;
        }

        // Calculate the Greenwich hour angle at midnight GMT for the specific date.
        double sungha = (((gha[iend-1] - gha[ist-1]) * dmo) + gha[ist-1]) * RAD_PER_DEG;

        // Calculate the sun longitude angle.  Use the Greenwich hour angle at
        // midnight GMT as the origin and subtract the amount the earth has
        // rotated since midnight GMT.  This value is then subtraced from 2PI
        // so the longitude result is referenced from Greenwich meridian.
        double sunlng = 2.0*Math.PI - sungha - (we * tsec);
        if (sunlng < 0.0) {
            sunlng += 2.0*Math.PI;
        }

        // The maximum sun angle (23.5) occures at julian date 172 (Summer Solstice).
        // Also the change in the sun angle from day to day models a cosine function.
        // These two facts are used to scale the sun angles between 23.5 and -23.5
        // degrees using the cosine of the distance the earth has progresed in it's
        // orbit from the summer solstace.

        // Calculate the distance the earth has moved and use that to determine the
        // sun's latitude.
        double earc = ((fjd - SUMSOL) / DAY_PER_YEAR) * 2.0*Math.PI;
        double sunlat = (23.5 * Math.cos(earc * MathUtils.DTOR)) * RAD_PER_DEG;

        return new SunPosition(Math.toDegrees(sunlat), Math.toDegrees(sunlng));
    }

    private static Calendar toUTC(Calendar calendar) {
        final Calendar utc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(calendar.getTimeInMillis());

        return utc;
    }

    static long toJulianDay(int year, int month, int dayOfMonth) {
        // todo: taken from EqualizationAlgorithm. Move to a general place!

        final double millisPerDay = 86400000.0;

        // The epoch (days) for the Julian Date (JD) which corresponds to 4713-01-01 12:00 BC.
        final double epochJulianDate = -2440587.5;

        final GregorianCalendar utc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(year, month, dayOfMonth, 0, 0, 0);
        utc.set(Calendar.MILLISECOND, 0);

        return (long) (utc.getTimeInMillis() / millisPerDay - epochJulianDate);
    }


}
