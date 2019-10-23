package org.esa.snap.remote.products.repository;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

/**
 * Created by jcoravu on 10/9/2019.
 */
public class Polygon2D {

    private Path2D.Double path;

    public Polygon2D() {
    }

    public void append(double x, double y) {
        if (this.path == null) {
            this.path = new Path2D.Double();
            this.path.moveTo(x, y);
        } else {
            this.path.lineTo(x, y);
        }
    }

    public Path2D.Double getPath() {
        return path;
    }

    public String toWKT() {
        StringBuilder wkt = new StringBuilder();
        wkt.append("POLYGON((");
        double[] segment = new double[2];
        PathIterator pathIterator = this.path.getPathIterator(null);
        while (!pathIterator.isDone()) {
            pathIterator.currentSegment(segment);
            wkt.append(segment[0])
                    .append(" ")
                    .append(segment[1])
                    .append(",");
            pathIterator.next();
        }
        wkt.setLength(wkt.length() - 1);
        wkt.append("))");
        return wkt.toString();
    }

    public static Polygon2D buildPolygon(Rectangle2D selectionArea) {
        Polygon2D polygon = new Polygon2D();
        polygon.append(selectionArea.getX(), selectionArea.getY()); // the top left corner
        polygon.append(selectionArea.getX() + selectionArea.getWidth(), selectionArea.getY()); // the top right corner
        polygon.append(selectionArea.getX() + selectionArea.getWidth(), selectionArea.getY() + selectionArea.getHeight()); // the bottom right corner
        polygon.append(selectionArea.getX(), selectionArea.getY() + selectionArea.getHeight()); // the bottom left corner
        polygon.append(selectionArea.getX(), selectionArea.getY()); // the top left corner
        return polygon;
    }

    public static Path2D.Double buildPath(Rectangle2D rectangle) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(rectangle.getX(), rectangle.getY()); // the top left corner
        path.lineTo(rectangle.getX() + rectangle.getWidth(), rectangle.getY()); // the top right corner
        path.lineTo(rectangle.getX() + rectangle.getWidth(), rectangle.getY() + rectangle.getHeight()); // the bottom right corner
        path.lineTo(rectangle.getX(), rectangle.getY() + rectangle.getHeight()); // the bottom left corner
        path.lineTo(rectangle.getX(), rectangle.getY()); // the top left corner
        return path;
    }
}
