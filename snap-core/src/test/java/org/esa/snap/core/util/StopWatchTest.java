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

package org.esa.snap.core.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class StopWatchTest {

    /**
     * Tests the functionality for getEndTime
     */
    @Test
    public void testGetEndTime() {
        StopWatch watch = new StopWatch();
        long endTime;

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }

        // check if stop time is different from start
        watch.stop();
        endTime = watch.getEndTime();
        assertTrue(0 != endTime);

        // check if end time now is the same (we haven't started the timer agaiin
        assertEquals(endTime, watch.getEndTime());
    }

    /**
     * Tests the functionality of getTimeDiff
     */
    @Test
    public void testGetTimeDiff() {
        StopWatch watch = new StopWatch();
        long startTime;
        long endTime;

        startTime = watch.getStartTime();
        watch.stop();
        endTime = watch.getEndTime();
        assertEquals(endTime - startTime, watch.getTimeDiff());
    }

    /**
     * Tests the functionality of getTimeDiffString()
     */
    @Test
    public void testGetTimeDiffString() {
        StopWatch watch = new StopWatch();

        watch.stop();
        // just check that we don't get an empty string
        assertNotSame("", watch.getTimeDiffString());
    }

    /**
     * Tests the functionality of getTimeString
     */
    @Test
    public void testGetTimeString() {
        // just test that we don't get an empty string
        assertNotSame("", StopWatch.getTimeString(12));
    }

    /**
     * Tests the functionality of toString()
     */
    @Test
    public void testToString() {
        StopWatch watch = new StopWatch();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        watch.stop();
        assertEquals(watch.getTimeDiffString(), watch.toString());
    }
}
