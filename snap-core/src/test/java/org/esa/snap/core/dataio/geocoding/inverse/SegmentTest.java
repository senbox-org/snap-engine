/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.dataio.geocoding.inverse;

import org.esa.snap.core.datamodel.GeoPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SegmentTest {

    @Test
    public void testConstruction() {
        final Segment segment = new Segment(1, 2, 3, 4);

        assertEquals(1, segment.x_min);
        assertEquals(2, segment.x_max);
        assertEquals(3, segment.y_min);
        assertEquals(4, segment.y_max);
    }

    @Test
    public void testSplit_acrossTrack() {
        final Segment segment = new Segment(0, 10, 0, 20);

        final Segment[] splits = segment.split(true);
        assertEquals(2, splits.length);

        final Segment upper = splits[0];
        assertEquals(0, upper.x_min);
        assertEquals(10, upper.x_max);
        assertEquals(0, upper.y_min);
        assertEquals(10, upper.y_max);

        final Segment lower = splits[1];
        assertEquals(0, lower.x_min);
        assertEquals(10, lower.x_max);
        assertEquals(11, lower.y_min);
        assertEquals(20, lower.y_max);
    }

    @Test
    public void testSplit_acrossTrack_segmentTooSmall() {
        final Segment segment = new Segment(0, 3, 0, 20);

        final Segment[] splits = segment.split(true);
        assertEquals(1, splits.length);

        final Segment split = splits[0];
        assertEquals(0, split.x_min);
        assertEquals(3, split.x_max);
        assertEquals(0, split.y_min);
        assertEquals(20, split.y_max);
    }

    @Test
    public void testSplit_alongTrack() {
        final Segment segment = new Segment(10, 60, 100, 140);

        final Segment[] splits = segment.split(false);
        assertEquals(2, splits.length);

        final Segment left = splits[0];
        assertEquals(10, left.x_min);
        assertEquals(35, left.x_max);
        assertEquals(100, left.y_min);
        assertEquals(140, left.y_max);

        final Segment right = splits[1];
        assertEquals(36, right.x_min);
        assertEquals(60, right.x_max);
        assertEquals(100, right.y_min);
        assertEquals(140, right.y_max);
    }

    @Test
    public void testSplit_x() {
        final Segment segment = new Segment(20, 70, 110, 150);

        final Segment[] splits = segment.split_x(28);
        assertEquals(2, splits.length);

        final Segment left = splits[0];
        assertEquals(20, left.x_min);
        assertEquals(27, left.x_max);
        assertEquals(110, left.y_min);
        assertEquals(150, left.y_max);

        final Segment right = splits[1];
        assertEquals(28, right.x_min);
        assertEquals(70, right.x_max);
        assertEquals(110, right.y_min);
        assertEquals(150, right.y_max);
    }

    @Test
    public void testSplit_x_segmentTooSmall() {
        final Segment segment = new Segment(20, 70, 110, 150);

        Segment[] splits = segment.split_x(23);
        assertEquals(1, splits.length);

        Segment left = splits[0];
        assertEquals(20, left.x_min);
        assertEquals(70, left.x_max);
        assertEquals(110, left.y_min);
        assertEquals(150, left.y_max);

        splits = segment.split_x(67);
        assertEquals(1, splits.length);

        left = splits[0];
        assertEquals(20, left.x_min);
        assertEquals(70, left.x_max);
        assertEquals(110, left.y_min);
        assertEquals(150, left.y_max);
    }

    @Test
    public void testSplit_x_outside() {
        final Segment segment = new Segment(30, 80, 120, 160);

        try {
            segment.split_x(29);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            segment.split_x(30);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            segment.split_x(80);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            segment.split_x(81);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testSplit_y() {
        final Segment segment = new Segment(30, 80, 120, 160);

        final Segment[] splits = segment.split_y(136);
        assertEquals(2, splits.length);

        final Segment left = splits[0];
        assertEquals(30, left.x_min);
        assertEquals(80, left.x_max);
        assertEquals(120, left.y_min);
        assertEquals(135, left.y_max);

        final Segment right = splits[1];
        assertEquals(30, right.x_min);
        assertEquals(80, right.x_max);
        assertEquals(136, right.y_min);
        assertEquals(160, right.y_max);
    }

    @Test
    public void testSplit_y_segmentTooSmall() {
        final Segment segment = new Segment(30, 80, 120, 160);

        Segment[] splits = segment.split_y(123);
        assertEquals(1, splits.length);

        Segment left = splits[0];
        assertEquals(30, left.x_min);
        assertEquals(80, left.x_max);
        assertEquals(120, left.y_min);
        assertEquals(160, left.y_max);

        splits = segment.split_y(157);
        assertEquals(1, splits.length);

        left = splits[0];
        assertEquals(30, left.x_min);
        assertEquals(80, left.x_max);
        assertEquals(120, left.y_min);
        assertEquals(160, left.y_max);
    }

    @Test
    public void testSplit_y_outside() {
        final Segment segment = new Segment(40, 90, 130, 170);

        try {
            segment.split_y(129);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            segment.split_y(130);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            segment.split_y(170);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            segment.split_y(171);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetWidth() {
        final Segment segment = new Segment(20, 69, 110, 150);

        assertEquals(50, segment.getWidth());
    }

    @Test
    public void testGetHeight() {
        final Segment segment = new Segment(30, 70, 120, 159);

        assertEquals(40, segment.getHeight());
    }

    @Test
    public void testContains_inside() {
        final Segment segment = new Segment(-1, -1, -1, -1);    // x/y coordinates are unimportant here tb 2021-03-11

        segment.lon_min = 15;
        segment.lon_max = 17;

        segment.lat_min = -43;
        segment.lat_max = -41;

        assertTrue(segment.isInside(15.2, -42.87));
        assertTrue(segment.isInside(15.9, -41.99));
        assertTrue(segment.isInside(16.928, -41.025));
    }

    @Test
    public void testContains_outside() {
        final Segment segment = new Segment(-1, -1, -1, -1);    // x/y coordinates are unimportant here tb 2021-03-11

        segment.lon_min = -163;
        segment.lon_max = -161;

        segment.lat_min = -1;
        segment.lat_max = 1;

        assertFalse(segment.isInside(-163.02, 0));
        assertFalse(segment.isInside(-162, 1.03));
        assertFalse(segment.isInside(-160.98, 0));
        assertFalse(segment.isInside(-162, -1.004));
    }

    @Test
    public void testContains_inside_withAntiMeridian() {
        final Segment segment = new Segment(-1, -1, -1, -1);    // x/y coordinates are unimportant here tb 2021-03-11

        segment.containsAntiMeridian = true;

        segment.lon_min = -175;
        segment.lon_max = 178;

        segment.lat_min = -28;
        segment.lat_max = -25;

        assertTrue(segment.isInside(179.3, -27.5));
        assertTrue(segment.isInside(-177.62, -26));
    }

    @Test
    public void testContains_outside_withAntiMeridian() {
        final Segment segment = new Segment(-1, -1, -1, -1);    // x/y coordinates are unimportant here tb 2021-03-11

        segment.containsAntiMeridian = true;

        segment.lon_min = -177;
        segment.lon_max = 176;

        segment.lat_min = 11;
        segment.lat_max = 14;

        assertFalse(segment.isInside(175.34, 12));
        assertFalse(segment.isInside(-174.8, 13));
        assertFalse(segment.isInside(177, 10.926));
        assertFalse(segment.isInside(-178, 14.003));
    }

    @Test
    public void testCalculateGeoPoints() {
        final Segment segment = new Segment(20, 40, 280, 390);

        segment.calculateGeoPoints(new MockCalculator());

        assertEquals(0.2, segment.lon_min, 1e-8);
        assertEquals(0.4, segment.lon_max, 1e-8);
        assertEquals(2.8, segment.lat_min, 1e-8);
        assertEquals(3.9, segment.lat_max, 1e-8);

        assertFalse(segment.containsAntiMeridian);
    }

    @Test
    public void testCalculateGeoPoints_antiMeridian() {
        // this because the mock just divides rasterpositiony by 100 to create geolocations tb 2021-03-16
        final Segment segment = new Segment(-17000, 17000, 280, 390);

        segment.calculateGeoPoints(new MockCalculator());

        assertEquals(-170.0, segment.lon_min, 1e-8);
        assertEquals(170.0, segment.lon_max, 1e-8);
        assertEquals(2.8, segment.lat_min, 1e-8);
        assertEquals(3.9, segment.lat_max, 1e-8);

        assertTrue(segment.containsAntiMeridian);
    }

    @Test
    public void testClone() {
        final Segment segment = new Segment(1, 2, 3, 4);
        segment.lon_min = 5;
        segment.lon_max = 6;
        segment.lat_min = 7;
        segment.lat_max = 8;
        segment.containsAntiMeridian = true;

        final Segment clone = segment.clone();

        assertEquals(segment.x_min, clone.x_min);
        assertEquals(segment.x_max, clone.x_max);
        assertEquals(segment.y_min, clone.y_min);
        assertEquals(segment.y_max, clone.y_max);

        assertEquals(segment.lon_min, clone.lon_min, 1e-8);
        assertEquals(segment.lon_max, clone.lon_max, 1e-8);
        assertEquals(segment.lat_min, clone.lat_min, 1e-8);
        assertEquals(segment.lat_max, clone.lat_max, 1e-8);

        assertEquals(segment.containsAntiMeridian, clone.containsAntiMeridian);
    }

    private class MockCalculator implements GeoPosCalculator {
        @Override
        public void getGeoPos(int pixelX, int pixelY, GeoPos geoPos) {
            geoPos.lon = (double) pixelX / 100.0;
            geoPos.lat = (double) pixelY / 100.0;
        }
    }
}
