package org.esa.snap.remote.products.repository.geometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Created by jcoravu on 31/1/2020.
 */
public class GeometryUtils {

    public GeometryUtils() {
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

    private static Polygon2D buildPolygon(Polygon polygon) {
        Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();
        Coordinate firstCoordinate = coordinates[0];
        Coordinate lastCoordinate = coordinates[coordinates.length-1];
        if (firstCoordinate.getX() != lastCoordinate.getX() || firstCoordinate.getY() != lastCoordinate.getY()) {
            throw new IllegalStateException("The first and last coordinates of the polygon do not match.");
        }
        Polygon2D polygon2D = new Polygon2D();
        for (Coordinate coordinate : coordinates) {
            polygon2D.append(coordinate.getX(), coordinate.getY());
        }
        return polygon2D;
    }

    public static AbstractGeometry2D convertProductGeometry(Geometry productGeometry) {
        AbstractGeometry2D geometry;
        if (productGeometry instanceof MultiPolygon) {
            MultiPolygon multiPolygon = (MultiPolygon)productGeometry;
            MultiPolygon2D multiPolygon2D = new MultiPolygon2D();
            for (int p=0; p<multiPolygon.getNumGeometries(); p++) {
                if (multiPolygon.getGeometryN(p) instanceof Polygon) {
                    Polygon2D polygon2D = buildPolygon((Polygon)multiPolygon.getGeometryN(p));
                    multiPolygon2D.setPolygon(p, polygon2D);
                } else {
                    throw new IllegalStateException("The multipolygon first geometry is not a polygon.");
                }
            }
            geometry = multiPolygon2D;
        } else if (productGeometry instanceof Polygon) {
            geometry = buildPolygon((Polygon)productGeometry);
        } else {
            throw new IllegalStateException("The product geometry type '"+productGeometry.getClass().getName()+"' is not a '"+Polygon.class.getName()+"' type.");
        }
        return geometry;
    }
}
