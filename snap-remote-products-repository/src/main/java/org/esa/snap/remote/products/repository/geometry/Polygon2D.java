package org.esa.snap.remote.products.repository.geometry;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

/**
 * Created by jcoravu on 31/1/2020.
 */
public class Polygon2D extends AbstractGeometry2D {

    public static final String POLYGON = "POLYGON";

    private Path2D.Double path;

    public Polygon2D() {
    }

    @Override
    public int getPathCount() {
        return 1;
    }

    @Override
    public Path2D.Double getPathAt(int index) {
        if (index != 0) {
            throw new ArrayIndexOutOfBoundsException("The index " + index + " is invalid.");
        }
        return this.path;
    }

    @Override
    public String toWKT() {
        StringBuilder wkt = new StringBuilder();
        wkt.append(POLYGON)
           .append("((");
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

    public Path2D.Double getPath() {
        return path;
    }

    public void append(double x, double y) {
        if (this.path == null) {
            this.path = new Path2D.Double();
            this.path.moveTo(x, y);
        } else {
            this.path.lineTo(x, y);
        }
    }
}
