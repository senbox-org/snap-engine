package org.esa.snap.remote.products.repository;

import org.esa.snap.remote.products.repository.geometry.GeometryUtils;
import org.esa.snap.remote.products.repository.geometry.Polygon2D;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by jcoravu on 20/1/2020.
 */
public class Polygon2DTest {

    public Polygon2DTest() {
    }

    @Test
    public void testPolygon() {
        Polygon2D polygon = new Polygon2D();
        polygon.append(0.0f, 0.0d);
        polygon.append(100.0f, 0.0d);
        polygon.append(100.0f, 100.0d);
        polygon.append(0.0f, 100.0d);
        polygon.append(0.0f, 0.0d);

        Path2D.Double path = polygon.getPath();
        assertNotNull(path);
        assertEquals(1, path.getWindingRule());
        assertEquals(true, path.contains(0.d, 0.0d));
        assertEquals(false, path.contains(100.1d, 0.0d));
        assertEquals(true, path.contains(50.d, 50.0d));

        Rectangle2D bounds = path.getBounds2D();
        assertNotNull(bounds);
        assertEquals(true, bounds.equals(new Rectangle.Double(0.0d, 0.0d, 100.0d, 100.0d)));

        String wkt = polygon.toWKT();
        assertNotNull(wkt);
        assertEquals("POLYGON((0.0 0.0,100.0 0.0,100.0 100.0,0.0 100.0,0.0 0.0))", wkt);
    }

    @Test
    public void testBuildPath() {
        Path2D.Double path = GeometryUtils.buildPath(new Rectangle.Double(25.0d, 25.0d, 75.0d, 75.0d));
        assertNotNull(path);
        assertEquals(false, path.contains(0.d, 0.0d));
        assertEquals(false, path.contains(24.1d, 75.0d));
        assertEquals(true, path.contains(50.d, 50.0d));

        Rectangle2D bounds = path.getBounds2D();
        assertNotNull(bounds);
        assertEquals(true, bounds.equals(new Rectangle.Double(25.0d, 25.0d, 75.0d, 75.0d)));
    }

    @Test
    public void testBuildPolygon() {
        Polygon2D polygon = GeometryUtils.buildPolygon(new Rectangle.Double(25.0d, 25.0d, 75.0d, 75.0d));
        assertNotNull(polygon);
        assertEquals(1, polygon.getPathCount());

        String wkt = polygon.toWKT();
        assertNotNull(wkt);
        assertEquals("POLYGON((25.0 25.0,100.0 25.0,100.0 100.0,25.0 100.0,25.0 25.0))", wkt);

        Path2D.Double path = polygon.getPath();
        assertNotNull(path);
        assertEquals(1, path.getWindingRule());
        assertEquals(false, path.contains(0.d, 0.0d));
        assertEquals(false, path.contains(10.1d, 0.0d));
        assertEquals(true, path.contains(50.d, 50.0d));
    }
}
