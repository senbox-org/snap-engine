package org.esa.s3tbx.idepix.core.util;

import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Test;

import java.awt.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BresenhamTest {

    final int w = 8;
    final int h = 8;
    int x = 5;
    int y = 4;
    int x2 = 21;
    int y2 = 28;
    final Rectangle rect = new Rectangle(0, 0, w, h);
    final Rectangle rect2 = new Rectangle(16, 24, w, h);

    @Test
    public void testFindBorderCoordinate() throws Exception {

        // first octant (UR angle is ~63 deg)
        double angle = 45.0;
        PixelPos borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(7, Math.round(borderPixel.getX()));
        assertEquals(2, Math.round(borderPixel.getY()));

        PixelPos borderPixel2 = Bresenham.findBorderPixel(x2, y2, rect2, angle);
        assertEquals(23, Math.round(borderPixel2.getX()));
        assertEquals(26, Math.round(borderPixel2.getY()));

        angle = 62.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(7, Math.round(borderPixel.getX()));
        assertEquals(0, Math.round(borderPixel.getY()));

        // second octant
        angle = 64.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(7, Math.round(borderPixel.getX()));
        assertEquals(0, Math.round(borderPixel.getY()));

        borderPixel = Bresenham.findBorderPixel(x2, y2, rect2, angle);
        assertEquals(23, Math.round(borderPixel.getX()));
        assertEquals(24, Math.round(borderPixel.getY()));

        angle = 89.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(5, Math.round(borderPixel.getX()));
        assertEquals(0, Math.round(borderPixel.getY()));

        // third octant
        angle = 91.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(5, Math.round(borderPixel.getX()));
        assertEquals(0, Math.round(borderPixel.getY()));

        angle = 135.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(1, Math.round(borderPixel.getX()));
        assertEquals(0, Math.round(borderPixel.getY()));

        // fourth octant
        angle = 160.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(0, Math.round(borderPixel.getX()));
        assertEquals(2, Math.round(borderPixel.getY()));

        borderPixel = Bresenham.findBorderPixel(x2, y2, rect2, angle);
        assertEquals(16, Math.round(borderPixel.getX()));
        assertEquals(26, Math.round(borderPixel.getY()));


        angle = 179.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(0, Math.round(borderPixel.getX()));
        assertEquals(4, Math.round(borderPixel.getY()));

        // fifth octant
        angle = 181.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(0, Math.round(borderPixel.getX()));
        assertEquals(4, Math.round(borderPixel.getY()));

        angle = 190.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(0, Math.round(borderPixel.getX()));
        assertEquals(5, Math.round(borderPixel.getY()));

        borderPixel = Bresenham.findBorderPixel(x2, y2, rect2, angle);
        assertEquals(16, Math.round(borderPixel.getX()));
        assertEquals(29, Math.round(borderPixel.getY()));

        // sixth octant
        angle = 225.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(2, Math.round(borderPixel.getX()));
        assertEquals(7, Math.round(borderPixel.getY()));

        angle = 269.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(5, Math.round(borderPixel.getX()));
        assertEquals(7, Math.round(borderPixel.getY()));

        // seventh octant
        angle = 271.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(5, Math.round(borderPixel.getX()));
        assertEquals(7, Math.round(borderPixel.getY()));

        angle = 280.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(6, Math.round(borderPixel.getX()));
        assertEquals(7, Math.round(borderPixel.getY()));

        // eighth octant
        angle = 315.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(7, Math.round(borderPixel.getX()));
        assertEquals(6, Math.round(borderPixel.getY()));
        borderPixel = Bresenham.findBorderPixel(x2, y2, rect2, angle);
        assertEquals(23, Math.round(borderPixel.getX()));
        assertEquals(30, Math.round(borderPixel.getY()));

        angle = 359.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(7, Math.round(borderPixel.getX()));
        assertEquals(4, Math.round(borderPixel.getY()));

        // horizontal and vertical lines
        angle = 0.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(7, Math.round(borderPixel.getX()));
        assertEquals(4, Math.round(borderPixel.getY()));

        angle = 90.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(5, Math.round(borderPixel.getX()));
        assertEquals(0, Math.round(borderPixel.getY()));

        angle = 180.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(0, Math.round(borderPixel.getX()));
        assertEquals(4, Math.round(borderPixel.getY()));

        angle = 270.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        assertEquals(5, Math.round(borderPixel.getX()));
        assertEquals(7, Math.round(borderPixel.getY()));

    }

    @Test
    public void testFirstQuadrant() throws Exception {

        // first octant (UR angle is ~63 deg)
        double angle = 45.0;
        PixelPos borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        List<PixelPos> pathPixels = Bresenham.getPathPixels(x, y, (int) borderPixel.getX(), (int) borderPixel.getY(), rect);
        assertNotNull(pathPixels);
        assertEquals(2, pathPixels.size());
        assertEquals(6, (int) pathPixels.get(0).getX());
        assertEquals(3, (int) pathPixels.get(0).getY());
        assertEquals(7, (int) pathPixels.get(1).getX());
        assertEquals(2, (int) pathPixels.get(1).getY());

        // second octant
        angle = 62.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        pathPixels = Bresenham.getPathPixels(x, y, (int) borderPixel.getX(), (int) borderPixel.getY(), rect);
        assertNotNull(pathPixels);
        assertEquals(4, pathPixels.size());
        assertEquals(5, (int) pathPixels.get(0).getX());
        assertEquals(3, (int) pathPixels.get(0).getY());
        assertEquals(6, (int) pathPixels.get(1).getX());
        assertEquals(2, (int) pathPixels.get(1).getY());
        assertEquals(6, (int) pathPixels.get(2).getX());
        assertEquals(1, (int) pathPixels.get(2).getY());
        assertEquals(7, (int) pathPixels.get(3).getX());
        assertEquals(0, (int) pathPixels.get(3).getY());

        borderPixel = Bresenham.findBorderPixel(x2, y2, rect2, angle);
        pathPixels = Bresenham.getPathPixels(x2, y2, (int) borderPixel.getX(), (int) borderPixel.getY(), rect2);
        assertNotNull(pathPixels);
        assertEquals(4, pathPixels.size());
        assertEquals(21, (int) pathPixels.get(0).getX());
        assertEquals(27, (int) pathPixels.get(0).getY());
        assertEquals(22, (int) pathPixels.get(1).getX());
        assertEquals(26, (int) pathPixels.get(1).getY());
        assertEquals(22, (int) pathPixels.get(2).getX());
        assertEquals(25, (int) pathPixels.get(2).getY());
        assertEquals(23, (int) pathPixels.get(3).getX());
        assertEquals(24, (int) pathPixels.get(3).getY());

    }

    @Test
    public void testSecondQuadrant() throws Exception {

        double angle = 135.0;
        PixelPos borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        List<PixelPos> pathPixels = Bresenham.getPathPixels(x, y, (int) borderPixel.getX(), (int) borderPixel.getY(), rect);
        assertNotNull(pathPixels);
        assertEquals(4, pathPixels.size());
        assertEquals(4, (int) pathPixels.get(0).getX());
        assertEquals(3, (int) pathPixels.get(0).getY());
        assertEquals(3, (int) pathPixels.get(1).getX());
        assertEquals(2, (int) pathPixels.get(1).getY());
        assertEquals(2, (int) pathPixels.get(2).getX());
        assertEquals(1, (int) pathPixels.get(2).getY());
        assertEquals(1, (int) pathPixels.get(3).getX());
        assertEquals(0, (int) pathPixels.get(3).getY());

        angle = 160.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        pathPixels = Bresenham.getPathPixels(x, y, (int) borderPixel.getX(), (int) borderPixel.getY(), rect);
        assertNotNull(pathPixels);
        assertEquals(5, pathPixels.size());
        assertEquals(4, (int) pathPixels.get(0).getX());
        assertEquals(4, (int) pathPixels.get(0).getY());
        assertEquals(3, (int) pathPixels.get(1).getX());
        assertEquals(3, (int) pathPixels.get(1).getY());
        assertEquals(2, (int) pathPixels.get(2).getX());
        assertEquals(3, (int) pathPixels.get(2).getY());
        assertEquals(1, (int) pathPixels.get(3).getX());
        assertEquals(2, (int) pathPixels.get(3).getY());
        assertEquals(0, (int) pathPixels.get(4).getX());
        assertEquals(2, (int) pathPixels.get(4).getY());

    }

    @Test
    public void testThirdQuadrant() throws Exception {

        double angle = 200.0;
        PixelPos borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        List<PixelPos> pathPixels = Bresenham.getPathPixels(x, y, (int) borderPixel.getX(), (int) borderPixel.getY(), rect);
        assertNotNull(pathPixels);
        assertEquals(5, pathPixels.size());
        assertEquals(4, (int) pathPixels.get(0).getX());
        assertEquals(4, (int) pathPixels.get(0).getY());
        assertEquals(3, (int) pathPixels.get(1).getX());
        assertEquals(5, (int) pathPixels.get(1).getY());
        assertEquals(2, (int) pathPixels.get(2).getX());
        assertEquals(5, (int) pathPixels.get(2).getY());
        assertEquals(1, (int) pathPixels.get(3).getX());
        assertEquals(6, (int) pathPixels.get(3).getY());
        assertEquals(0, (int) pathPixels.get(4).getX());
        assertEquals(6, (int) pathPixels.get(4).getY());

        angle = 225.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        pathPixels = Bresenham.getPathPixels(x, y, (int) borderPixel.getX(), (int) borderPixel.getY(), rect);
        assertNotNull(pathPixels);
        assertEquals(3, pathPixels.size());
        assertEquals(4, (int) pathPixels.get(0).getX());
        assertEquals(5, (int) pathPixels.get(0).getY());
        assertEquals(3, (int) pathPixels.get(1).getX());
        assertEquals(6, (int) pathPixels.get(1).getY());
        assertEquals(2, (int) pathPixels.get(2).getX());
        assertEquals(7, (int) pathPixels.get(2).getY());
    }

    @Test
    public void testFourthQuadrant() throws Exception {

        double angle = 280.0;
        PixelPos borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        List<PixelPos> pathPixels = Bresenham.getPathPixels(x, y, (int) borderPixel.getX(), (int) borderPixel.getY(), rect);
        assertNotNull(pathPixels);
        assertEquals(3, pathPixels.size());
        assertEquals(5, (int) pathPixels.get(0).getX());
        assertEquals(5, (int) pathPixels.get(0).getY());
        assertEquals(5, (int) pathPixels.get(1).getX());
        assertEquals(6, (int) pathPixels.get(1).getY());
        assertEquals(6, (int) pathPixels.get(2).getX());
        assertEquals(7, (int) pathPixels.get(2).getY());

        angle = 315.0;
        borderPixel = Bresenham.findBorderPixel(x, y, rect, angle);
        pathPixels = Bresenham.getPathPixels(x, y, (int) borderPixel.getX(), (int) borderPixel.getY(), rect);
        assertNotNull(pathPixels);
        assertEquals(2, pathPixels.size());
        assertEquals(6, (int) pathPixels.get(0).getX());
        assertEquals(5, (int) pathPixels.get(0).getY());
        assertEquals(7, (int) pathPixels.get(1).getX());
        assertEquals(6, (int) pathPixels.get(1).getY());
    }
}
