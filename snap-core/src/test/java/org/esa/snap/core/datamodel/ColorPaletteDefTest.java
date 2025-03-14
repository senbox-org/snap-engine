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
package org.esa.snap.core.datamodel;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.Test;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.esa.snap.core.datamodel.ColorPaletteDef.*;
import static org.esa.snap.core.datamodel.ColorPaletteDef.Point;
import static org.junit.Assert.*;

public class ColorPaletteDefTest {

    @Test
    public void testConstructors() {
        ColorPaletteDef cpd = new ColorPaletteDef(-1.0, 1.0);
        assertEquals(256, cpd.getNumColors());
        assertEquals(2, cpd.getNumPoints());
        assertEquals(-1.0, cpd.getPointAt(0).getSample(), 1e-10);
        assertEquals(+1.0, cpd.getPointAt(1).getSample(), 1e-10);
        assertEquals(Color.BLACK, cpd.getPointAt(0).getColor());
        assertEquals(Color.WHITE, cpd.getPointAt(1).getColor());

        cpd = new ColorPaletteDef(-1.0, 0.5, 1.0);
        assertEquals(256, cpd.getNumColors());
        assertEquals(3, cpd.getNumPoints());
        assertEquals(-1.0, cpd.getPointAt(0).getSample(), 1e-10);
        assertEquals(+0.5, cpd.getPointAt(1).getSample(), 1e-10);
        assertEquals(+1.0, cpd.getPointAt(2).getSample(), 1e-10);
        assertEquals(Color.BLACK, cpd.getPointAt(0).getColor());
        assertEquals(Color.GRAY, cpd.getPointAt(1).getColor());
        assertEquals(Color.WHITE, cpd.getPointAt(2).getColor());

        cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });
        assertEquals(4, cpd.getNumPoints());
        assertEquals(256, cpd.getNumColors());
        assertTrue(cpd.isFullyOpaque());

        cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        }, 512);
        assertEquals(4, cpd.getNumPoints());
        assertEquals(512, cpd.getNumColors());
        assertTrue(cpd.isFullyOpaque());

        cpd = new ColorPaletteDef(new Point[]{
                new Point(100, new Color(100, 100, 100, 100)),
                new Point(600, Color.WHITE)
        }, 16);
        assertEquals(2, cpd.getNumPoints());
        assertEquals(16, cpd.getNumColors());
        assertFalse(cpd.isFullyOpaque());
    }

    @SuppressWarnings("SimplifiableAssertion")
    @Test
    public void testCreateClone_andEquals() {
        //preparation
        final Point[] points = {
                new Point(1, Color.black),
                new Point(2, Color.red),
                new Point(3, Color.green),
                new Point(4, Color.blue),
                new Point(5, Color.white),
        };
        final ColorPaletteDef cpd = new ColorPaletteDef(points, 256);
        cpd.setDiscrete(true);
        cpd.setAutoDistribute(true);

        //execution
        final ColorPaletteDef clone = (ColorPaletteDef) cpd.clone();

        //verification
        assertTrue(cpd.equals(clone));
    }

    @Test
    @STTM("SNAP-3962")
    public void testSetIsDiscrete() {
        final ColorPaletteDef cpd = new ColorPaletteDef(-1.0, 1.0);

        assertFalse(cpd.isDiscrete());

        cpd.setDiscrete(true);
        assertTrue(cpd.isDiscrete());
    }

    @Test
    @STTM("SNAP-3962")
    public void testSetGetNumColors() {
        final ColorPaletteDef cpd = new ColorPaletteDef(-1.0, 1.0);
        assertEquals(256, cpd.getNumColors());

        cpd.setNumColors(125);
        assertEquals(125, cpd.getNumColors());
    }

    @Test
    @STTM("SNAP-3962")
    public void testSetGetNumPoints_increase() {
        final ColorPaletteDef cpd = new ColorPaletteDef(-1.0, 1.0);
        assertEquals(2, cpd.getNumPoints());

        cpd.setNumPoints(5);
        assertEquals(5, cpd.getNumPoints());

        final Point[] points = cpd.getPoints();
        assertEquals(5, points.length);
    }

    @Test
    @STTM("SNAP-3962")
    public void testSetGetNumPoints_decrease() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });
        assertEquals(4, cpd.getNumPoints());

        cpd.setNumPoints(3);
        assertEquals(3, cpd.getNumPoints());

        final Point[] points = cpd.getPoints();
        assertEquals(3, points.length);
    }

    @Test
    @STTM("SNAP-3962")
    public void testSetIsAutoDistribute() {
        final ColorPaletteDef cpd = new ColorPaletteDef(-1.0, 1.0);
        assertFalse(cpd.isAutoDistribute());

        cpd.setAutoDistribute(true);
        assertTrue(cpd.isAutoDistribute());
    }

    @Test
    @STTM("SNAP-3962")
    public void testGetFirstPoint() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });

        final Point firstPoint = cpd.getFirstPoint();
        assertEquals(100.0, firstPoint.getSample(), 1e-8);
    }

    @Test
    @STTM("SNAP-3962")
    public void testGetLastPoint() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });

        final Point lastPoint = cpd.getLastPoint();
        assertEquals(600.0, lastPoint.getSample(), 1e-8);
    }

    @Test
    @STTM("SNAP-3962")
    public void testGetMinDisplaySample() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });

        assertEquals(100.0, cpd.getMinDisplaySample(), 1e-8);
    }

    @Test
    @STTM("SNAP-3962")
    public void testGetMaxDisplaySample() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });

        assertEquals(600.0, cpd.getMaxDisplaySample(), 1e-8);
    }

    @Test
    @STTM("SNAP-3962")
    public void testInsertPointAfter() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });

        final Point toInsert = new Point(400, Color.RED);

        cpd.insertPointAfter(1, toInsert);
        final Point[] points = cpd.getPoints();
        assertEquals(5, points.length);
        assertEquals(400.0, points[2].getSample(), 1e-8);
        assertEquals(Color.RED, points[2].getColor());
    }

    @Test
    @STTM("SNAP-3962")
    public void testCreatePointAfter() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });


        final boolean isInserted = cpd.createPointAfter(2, new TestScaling());
        assertTrue(isInserted);

        final Point[] points = cpd.getPoints();
        assertEquals(5, points.length);
        assertEquals(550.0, points[3].getSample(), 1e-8);
        assertEquals(new Color(128, 128, 255), points[3].getColor());
    }

    @Test
    @STTM("SNAP-3962")
    public void testCreatePointAfter_invalidIndex() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });


        boolean isInserted = cpd.createPointAfter(3, new TestScaling());
        assertFalse(isInserted);

        isInserted = cpd.createPointAfter(-2, new TestScaling());
        assertFalse(isInserted);

        final Point[] points = cpd.getPoints();
        assertEquals(4, points.length);
    }

    @Test
    @STTM("SNAP-3962")
    public void testGetCenterColor() {
        final Color centerColor = getCenterColor(Color.RED, Color.orange);
        assertEquals(255, centerColor.getRed(), 1e-8);
        assertEquals(100, centerColor.getGreen(), 1e-8);
        assertEquals(0, centerColor.getBlue(), 1e-8);
        assertEquals(255, centerColor.getAlpha(), 1e-8);
    }

    @Test
    @STTM("SNAP-3962")
    public void testRemovePointAt() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });

        cpd.removePointAt(1);

        final Point[] points = cpd.getPoints();
        assertEquals(3, points.length);
        assertEquals(500.0, points[1].getSample(), 1e-8);
    }

    @Test
    @STTM("SNAP-3962")
    public void testAddPoint() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });

        cpd.addPoint(new Point(700, Color.BLACK));

        final Point[] points = cpd.getPoints();
        assertEquals(5, points.length);
        assertEquals(700.0, points[4].getSample(), 1e-8);
    }

    @Test
    @STTM("SNAP-3962")
    public void testSetGetPoints() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });

        final Point[] newPoints = {
                new Point(200, Color.ORANGE),
                new Point(300, Color.MAGENTA),
                new Point(400, Color.BLUE),
                new Point(500, Color.WHITE),
                new Point(500, Color.BLACK)
        };


        Point[] points = cpd.getPoints();
        assertEquals(4, points.length);

        cpd.setPoints(newPoints);
        points = cpd.getPoints();
        assertEquals(5, points.length);
    }

    @Test
    @STTM("SNAP-3962")
    public void testSetGetPoints_notEnough() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });

        final Point[] newPoints = {
                new Point(200, Color.ORANGE),
        };

        try {
            cpd.setPoints(newPoints);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    @STTM("SNAP-3962")
    public void testReadColorPaletteDef_withoutLabels() throws URISyntaxException, IOException {
        final Path cpdPath = getResourceAsPath("AlosAV2_color_palette.cpd");

        final ColorPaletteDef cpd = loadColorPaletteDef(cpdPath);
        assertEquals(3, cpd.getNumPoints());
        assertEquals(256, cpd.getNumColors());
        assertTrue(cpd.isAutoDistribute());

        final Point[] points = cpd.getPoints();
        assertEquals(3, points.length);
        assertEquals(new Color(0, 0, 0), points[0].getColor());
        assertEquals("AlosAV2_color_palette.cpd", points[0].getLabel());

        assertEquals(50, points[1].getSample(), 1e-8);
        assertEquals("AlosAV2_color_palette.cpd", points[1].getLabel());

        assertEquals(new Color(255, 255, 255), points[2].getColor());
        assertEquals("AlosAV2_color_palette.cpd", points[2].getLabel());
    }

    @Test
    @STTM("SNAP-3962")
    public void testReadColorPaletteDef_withLabels() throws URISyntaxException, IOException {
        final Path cpdPath = getResourceAsPath("spectrum.cpd");

        final ColorPaletteDef cpd = loadColorPaletteDef(cpdPath);
        assertEquals(8, cpd.getNumPoints());
        assertEquals(256, cpd.getNumColors());
        assertTrue(cpd.isAutoDistribute());

        final Point[] points = cpd.getPoints();
        assertEquals(8, points.length);

        assertEquals(new Color(0, 0, 0), points[0].getColor());
        assertEquals("land", points[0].getLabel());

        assertEquals(2, points[2].getSample(), 1e-8);
        assertEquals("fresh_inland_water", points[2].getLabel());

        assertEquals(new Color(0, 255, 0), points[4].getColor());
        assertEquals("bright", points[4].getLabel());

        assertEquals(6, points[6].getSample(), 1e-8);
        assertEquals("invalid", points[6].getLabel());
    }

    @Test
    @STTM("SNAP-3962")
    public void testReadColorPaletteDef_notEnoughPoints() throws URISyntaxException, IOException {
        final Path cpdPath = getResourceAsPath("too_small_spectrum.cpd");

        try {
            loadColorPaletteDef(cpdPath);
            fail("IOException expected");
        } catch (IOException expected) {
        }
    }

    @Test
    @STTM("SNAP-3962")
    public void testStoreAndLoadColorPaletteDef_withoutLabels() throws IOException {
        final Path tempDirectory = Files.createTempDirectory("SNAP");

        try {
            final Path cpdFile = tempDirectory.resolve("a_test_spectrum.cpd");
            final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                    new Point(100, Color.ORANGE),
                    new Point(200, Color.MAGENTA),
                    new Point(500, Color.BLUE),
                    new Point(600, Color.WHITE)
            });

            ColorPaletteDef.storeColorPaletteDef(cpd, cpdFile);

            final ColorPaletteDef cpdFromDisk = loadColorPaletteDef(cpdFile);
            assertEquals(4, cpdFromDisk.getNumPoints());

            final Point[] points = cpdFromDisk.getPoints();
            assertEquals(new Color(255, 200, 0), points[0].getColor());
            assertEquals("a_test_spectrum.cpd", points[0].getLabel());

            assertEquals(500, points[2].getSample(), 1e-8);
            assertEquals("a_test_spectrum.cpd", points[2].getLabel());

            assertEquals(new Color(255, 255, 255), points[3].getColor());
            assertEquals("a_test_spectrum.cpd", points[3].getLabel());
        } finally {
            FileUtils.deleteTree(tempDirectory.toFile());
        }
    }

    @Test
    @STTM("SNAP-3962")
    public void testStoreAndLoadColorPaletteDef_withLabels() throws IOException {
        final Path tempDirectory = Files.createTempDirectory("SNAP");

        try {
            final Path cpdFile = tempDirectory.resolve("a_test_spectrum.cpd");
            final ColorPaletteDef cpd = new ColorPaletteDef(new Point[]{
                    new Point(100, Color.ORANGE, "water"),
                    new Point(200, Color.MAGENTA, "whisky"),
                    new Point(500, Color.BLUE, "coffee"),
                    new Point(600, Color.WHITE, "champagne")
            });

            ColorPaletteDef.storeColorPaletteDef(cpd, cpdFile);

            final ColorPaletteDef cpdFromDisk = loadColorPaletteDef(cpdFile);
            assertEquals(4, cpdFromDisk.getNumPoints());

            final Point[] points = cpdFromDisk.getPoints();
            assertEquals(new Color(255, 200, 0), points[0].getColor());
            assertEquals("water", points[0].getLabel());

            assertEquals(500, points[2].getSample(), 1e-8);
            assertEquals("coffee", points[2].getLabel());

            assertEquals(new Color(255, 255, 255), points[3].getColor());
            assertEquals("champagne", points[3].getLabel());
        } finally {
            FileUtils.deleteTree(tempDirectory.toFile());
        }
    }

    private static Path getResourceAsPath(String resourceName) throws URISyntaxException {
        final URL resource = ColorPaletteDef.class.getResource(resourceName);
        assertNotNull(resource);
        return Paths.get(resource.toURI());
    }

    private static class TestScaling implements Scaling {
        @Override
        public double scale(double value) {
            return 0.8 * value;
        }

        @Override
        public double scaleInverse(double value) {
            return value / 0.8;
        }
    }
}
