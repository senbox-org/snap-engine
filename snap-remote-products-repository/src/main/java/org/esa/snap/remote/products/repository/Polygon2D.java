package org.esa.snap.remote.products.repository;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

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
}
