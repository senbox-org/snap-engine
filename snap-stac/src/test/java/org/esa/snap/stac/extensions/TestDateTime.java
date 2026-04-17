/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.stac.extensions;

import org.esa.snap.core.datamodel.ProductData;
import org.json.simple.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
public class TestDateTime {

    @Test
    public void testGetFormattedTime() throws Exception {
        ProductData.UTC utc = ProductData.UTC.parse("2019-07-25 09:51:19", "yyyy-mm-dd hh:mm:ss");

        String time = DateTime.getFormattedTime(utc);
        assertEquals("2019-01-25T09:51:19.000000Z", time);
    }

    @Test
    public void testGetFormattedTimeNull() {
        assertEquals("unknown", DateTime.getFormattedTime(null));
    }

    @Test
    public void testGetStartTimeFromStartDatetime() throws Exception {
        JSONObject props = new JSONObject();
        props.put(DateTime.start_datetime, "2022-05-10T12:30:00Z");

        ProductData.UTC utc = DateTime.getStartTime(props);
        assertNotNull(utc);
    }

    @Test
    public void testGetStartTimeFallbackToDatetime() throws Exception {
        JSONObject props = new JSONObject();
        props.put(DateTime.datetime, "2022-05-10T12:30:00Z");

        ProductData.UTC utc = DateTime.getStartTime(props);
        assertNotNull(utc);
    }

    @Test
    public void testGetStartTimeReturnsNullWhenMissing() throws Exception {
        JSONObject props = new JSONObject();
        ProductData.UTC utc = DateTime.getStartTime(props);
        assertNull(utc);
    }

    @Test
    public void testGetEndTimeFromEndDatetime() throws Exception {
        JSONObject props = new JSONObject();
        props.put(DateTime.end_datetime, "2022-05-10T14:30:00Z");

        ProductData.UTC utc = DateTime.getEndTime(props);
        assertNotNull(utc);
    }

    @Test
    public void testGetEndTimeFallbackToDatetime() throws Exception {
        JSONObject props = new JSONObject();
        props.put(DateTime.datetime, "2022-05-10T12:30:00Z");

        ProductData.UTC utc = DateTime.getEndTime(props);
        assertNotNull(utc);
    }

    @Test
    public void testGetEndTimeReturnsNullWhenMissing() throws Exception {
        JSONObject props = new JSONObject();
        ProductData.UTC utc = DateTime.getEndTime(props);
        assertNull(utc);
    }

    @Test
    public void testGetStartTimePrefersStartDatetime() throws Exception {
        JSONObject props = new JSONObject();
        props.put(DateTime.start_datetime, "2022-01-01T00:00:00Z");
        props.put(DateTime.datetime, "2022-06-15T00:00:00Z");

        ProductData.UTC utc = DateTime.getStartTime(props);
        assertNotNull(utc);
    }

    @Test
    public void testGetEndTimePrefersEndDatetime() throws Exception {
        JSONObject props = new JSONObject();
        props.put(DateTime.end_datetime, "2022-12-31T23:59:59Z");
        props.put(DateTime.datetime, "2022-06-15T00:00:00Z");

        ProductData.UTC utc = DateTime.getEndTime(props);
        assertNotNull(utc);
    }

    @Test
    public void testToUTC() throws Exception {
        ProductData.UTC utc = DateTime.toUTC("2022-05-10T12:30:00Z");
        assertNotNull(utc);
    }

    @Test
    public void testToUTCWithoutZ() throws Exception {
        ProductData.UTC utc = DateTime.toUTC("2022-05-10T12:30:00");
        assertNotNull(utc);
    }

    @Test
    public void testGetNowUTC() {
        String now = DateTime.getNowUTC();
        assertNotNull(now);
        // ISO 8601 format contains 'T'
        assertTrue(now.contains("T"));
    }

}
