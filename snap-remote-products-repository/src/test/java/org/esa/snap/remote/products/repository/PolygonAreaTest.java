package org.esa.snap.remote.products.repository;

import org.esa.snap.remote.products.repository.geometry.GeometryUtils;
import org.esa.snap.remote.products.repository.geometry.Polygon2D;
import org.esa.snap.remote.products.repository.geometry.PolygonArea;
import org.junit.Test;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PolygonAreaTest {

    // a triangle with its apex in the north
    private static PolygonArea triangle() {
        return new PolygonArea(new double[]{10, 14, 12}, new double[]{45, 45, 48});
    }

    @Test
    public void testBoundsAreTheBoundingBox() {
        final Rectangle2D area = triangle();
        assertEquals(10, area.getX(), 0);
        assertEquals(45, area.getY(), 0);
        assertEquals(4, area.getWidth(), 0);
        assertEquals(3, area.getHeight(), 0);
    }

    @Test
    public void testClosedRingAccepted() {
        final PolygonArea closed = new PolygonArea(new double[]{10, 14, 12, 10}, new double[]{45, 45, 48, 45});
        assertEquals(3, closed.getVertexCount());
        assertEquals(triangle(), closed);
    }

    @Test
    public void testInvalidPolygons() {
        try {
            new PolygonArea(new double[]{10, 14}, new double[]{45, 45});
            fail("two vertices are not a polygon");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            new PolygonArea(new double[]{10, 14, 12}, new double[]{45, 45});
            fail("vertex arrays of different lengths");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void testImmutableBounds() {
        try {
            triangle().setRect(0, 0, 1, 1);
            fail("the bounds follow the vertices");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testEquality() {
        assertEquals(triangle(), triangle());
        assertEquals(triangle().hashCode(), triangle().hashCode());
        // same bounding box, different polygon
        final PolygonArea other = new PolygonArea(new double[]{10, 14, 12}, new double[]{48, 48, 45});
        assertNotEquals(triangle(), other);
        assertNotEquals(triangle(), new Rectangle2D.Double(10, 45, 4, 3));
    }

    @Test
    public void testPath() {
        final Path2D.Double path = triangle().toPath();
        assertTrue(path.contains(12, 46));
        assertFalse("inside the bounding box but outside the triangle", path.contains(10.2, 47.8));
    }

    @Test
    public void testDatabasePolygonUsesTheVertices() {
        final Polygon2D polygon = GeometryUtils.buildPolygon(triangle());
        assertEquals("POLYGON((10.0 45.0,14.0 45.0,12.0 48.0,10.0 45.0))", polygon.toWKT());
        // rectangles as before
        assertEquals("POLYGON((10.0 45.0,14.0 45.0,14.0 48.0,10.0 48.0,10.0 45.0))",
                     GeometryUtils.buildPolygon(new Rectangle2D.Double(10, 45, 4, 3)).toWKT());
    }

    @Test
    public void testDrawingPathUsesTheVertices() {
        final Path2D.Double path = GeometryUtils.buildPath(triangle());
        assertFalse(path.contains(10.2, 47.8));
        assertTrue(GeometryUtils.buildPath(new Rectangle2D.Double(10, 45, 4, 3)).contains(10.2, 47.8));
    }
}
