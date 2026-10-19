package org.esa.snap.remote.products.repository.download;

import org.esa.snap.remote.products.repository.geometry.PolygonArea;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class QueryPolygonTest {

    @Test
    public void testPolygonAreaQueriesItsVertices() {
        final PolygonArea triangle = new PolygonArea(new double[]{10, 14, 12}, new double[]{45, 45, 48});
        final List<Point2D> points = RemoteRepositoriesManager.buildQueryPolygon(triangle).getPoints();
        assertEquals(4, points.size());
        assertEquals(new Point2D.Double(10, 45), points.get(0));
        assertEquals(new Point2D.Double(14, 45), points.get(1));
        assertEquals(new Point2D.Double(12, 48), points.get(2));
        assertEquals("closed", points.get(0), points.get(3));
    }

    @Test
    public void testRectangleQueriesItsCorners() {
        final List<Point2D> points = RemoteRepositoriesManager.buildQueryPolygon(new Rectangle2D.Double(10, 45, 4, 3)).getPoints();
        assertEquals(5, points.size());
        assertEquals(new Point2D.Double(10, 45), points.get(0));
        assertEquals(new Point2D.Double(14, 48), points.get(2));
    }
}
