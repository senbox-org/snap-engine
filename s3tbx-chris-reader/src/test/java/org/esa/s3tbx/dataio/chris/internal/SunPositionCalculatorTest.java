/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.dataio.chris.internal;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * Tests for class {@link SunPositionCalculator}.
 *
 * @author Ralf Quast
 */
public class SunPositionCalculatorTest {

    /**
     * Test calculation of angular position of the Sun.
     * <p/>
     * Expected values are from the <a href="http://www.srrb.noaa.gov/highlights/sunrise/azel.html">NOAA
     * Solar Position Calculator</a>.
     */
    @Test
    public void testCalculate() {
        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

        calendar.set(2002, 6, 1, 12, 0, 0);
        SunPosition sunPosition = SunPositionCalculator.calculate(calendar, 55.0, 10.00);
        assertEquals(32.59, sunPosition.getZenithAngle(), 0.02);
        assertEquals(195.57, sunPosition.getAzimuthAngle(), 0.02);

        calendar.set(2004, 8, 1, 14, 0, 0);
        sunPosition = SunPositionCalculator.calculate(calendar, 55.0, 10.00);
        assertEquals(56.69, sunPosition.getZenithAngle(), 0.02);
        assertEquals(229.67, sunPosition.getAzimuthAngle(), 0.02);
    }
}
