package org.esa.snap.remote.products.repository.geometry;

import java.awt.geom.Path2D;
import java.util.Arrays;

/**
 * The coordinates of a multi-polygon.
 *
 * Created by jcoravu on 31/1/2020.
 */
public class MultiPolygon2D extends AbstractGeometry2D {

    private Polygon2D[] polygons;

    public MultiPolygon2D() {
    }

    @Override
    public int getPathCount() {
        return this.polygons.length;
    }

    @Override
    public Path2D.Double getPathAt(int index) {
        return this.polygons[index].getPath();
    }

    @Override
    public String toWKT() {
        StringBuilder wkt = new StringBuilder();
        wkt.append("MULTIPOLYGON(");
        for (int i=0; i<this.polygons.length; i++) {
            if (i > 0) {
                wkt.append(", ");
            }
            String polygonWKT = this.polygons[i].toWKT();
            if (!polygonWKT.startsWith(Polygon2D.POLYGON)) {
                throw new IllegalStateException("Wrong polygon WKT '" + polygonWKT + "'.");
            }
            int index = polygonWKT.indexOf("((");
            if (index < 0) {
                throw new IllegalStateException("Wrong polygon WKT '" + polygonWKT + "'.");
            }
            wkt.append(polygonWKT.substring(index).trim());
        }
        wkt.append(")");
        return wkt.toString();
    }

    public void setPolygon(int index, Polygon2D polygon2D) {
        if (this.polygons == null) {
            this.polygons = new Polygon2D[index + 1];
            this.polygons[index] = polygon2D;
        } else if (index >= this.polygons.length) {
            this.polygons = Arrays.copyOf(this.polygons, index + 1);
            this.polygons[index] = polygon2D;
        } else {
            this.polygons[index] = polygon2D;
        }
    }
}
