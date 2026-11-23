package org.esa.snap.remote.products.repository.download;

import org.esa.snap.remote.products.repository.geometry.PolygonArea;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueryPolygonTest {

    @Test
    public void testPolygonAreaQueriesItsVertices() {
        // clockwise: west, north, east
        final PolygonArea triangle = new PolygonArea(new double[]{10, 12, 14}, new double[]{45, 48, 45});
        final List<Point2D> points = RemoteRepositoriesManager.buildQueryPolygon(triangle).getPoints();
        assertEquals(4, points.size());
        assertEquals(new Point2D.Double(10, 45), points.get(0));
        assertEquals(new Point2D.Double(12, 48), points.get(1));
        assertEquals(new Point2D.Double(14, 45), points.get(2));
        assertEquals("closed", points.get(0), points.get(3));
    }

    @Test
    public void testCounterClockwisePolygonIsReversed() {
        // drawn counter-clockwise: west, east, north
        final PolygonArea triangle = new PolygonArea(new double[]{10, 14, 12}, new double[]{45, 45, 48});
        final List<Point2D> points = RemoteRepositoriesManager.buildQueryPolygon(triangle).getPoints();
        assertEquals(4, points.size());
        assertEquals(new Point2D.Double(10, 45), points.get(0));
        assertEquals(new Point2D.Double(12, 48), points.get(1));
        assertEquals(new Point2D.Double(14, 45), points.get(2));
        assertEquals("closed", points.get(0), points.get(3));
        assertTrue("clockwise like the rectangle", signedArea(points) < 0);
    }

    private static double signedArea(List<Point2D> ring) {
        double sum = 0;
        for (int i = 0; i + 1 < ring.size(); i++) {
            sum += ring.get(i).getX() * ring.get(i + 1).getY() - ring.get(i + 1).getX() * ring.get(i).getY();
        }
        return sum;
    }

    @Test
    public void testRectangleQueriesItsCorners() {
        final List<Point2D> points = RemoteRepositoriesManager.buildQueryPolygon(new Rectangle2D.Double(10, 45, 4, 3)).getPoints();
        assertEquals(5, points.size());
        // unchanged corner order of the remote query: top left, bottom left, bottom right, top right
        assertEquals(new Point2D.Double(10, 45), points.get(0));
        assertEquals(new Point2D.Double(10, 48), points.get(1));
        assertEquals(new Point2D.Double(14, 48), points.get(2));
        assertEquals(new Point2D.Double(14, 45), points.get(3));
        assertEquals(new Point2D.Double(10, 45), points.get(4));
        assertTrue("clockwise", signedArea(points) < 0);
    }
}
