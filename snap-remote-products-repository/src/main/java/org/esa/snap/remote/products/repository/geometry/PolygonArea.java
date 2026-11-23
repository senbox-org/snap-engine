package org.esa.snap.remote.products.repository.geometry;

import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * An area of interest drawn as a polygon, in degrees (x = longitude, y = latitude). The polygon is simple (its edges
 * do not cross) and is not split at the antimeridian: its edges are straight lines in longitude and latitude.
 * <p>
 * The area of interest of the product library is a {@link Rectangle2D}; a polygon area is one too, with the bounds of
 * the polygon. Code that knows polygon areas ({@link GeometryUtils#buildPolygon(Rectangle2D)}, the remote repository
 * queries) uses the vertices; code that only knows rectangles searches the bounding box, which contains the polygon.
 * Some remote data sources search the bounding box even so: the USGS source uses only the bounds of the query polygon,
 * and the default TAO polygon converter sends the bounds of polygons with 200 or more points.
 * <p>
 * The bounds cannot be changed: the setters throw and the getters do not read the public fields of
 * {@link Rectangle2D.Double}. Equality compares the vertices, so a polygon area is never equal to a plain rectangle,
 * although a plain rectangle with the same bounds considers itself equal to it ({@link Rectangle2D#equals}).
 */
public class PolygonArea extends Rectangle2D.Double {

    private final double[] lons;
    private final double[] lats;
    // the bounds, independent of the public fields of Rectangle2D.Double
    private final double minX;
    private final double minY;
    private final double boundsWidth;
    private final double boundsHeight;

    /**
     * @param lons the longitudes of the vertices, without repeating the first vertex at the end
     * @param lats the latitudes of the vertices
     */
    public PolygonArea(double[] lons, double[] lats) {
        if (lons.length != lats.length) {
            throw new IllegalArgumentException("The polygon has " + lons.length + " longitudes and " + lats.length + " latitudes.");
        }
        int count = lons.length;
        if (count > 1 && lons[0] == lons[count - 1] && lats[0] == lats[count - 1]) {
            count--; // closed ring given
        }
        if (count < 3) {
            throw new IllegalArgumentException("A polygon needs at least 3 vertices.");
        }
        for (int i = 0; i < count; i++) {
            if (!java.lang.Double.isFinite(lons[i]) || !java.lang.Double.isFinite(lats[i])) {
                throw new IllegalArgumentException("Invalid polygon vertex " + lons[i] + " " + lats[i] + ".");
            }
        }
        if (isSelfIntersecting(lons, lats, count)) {
            throw new IllegalArgumentException("The edges of the polygon cross each other.");
        }
        this.lons = Arrays.copyOf(lons, count);
        this.lats = Arrays.copyOf(lats, count);
        double minX = this.lons[0], maxX = this.lons[0], minY = this.lats[0], maxY = this.lats[0];
        for (int i = 1; i < count; i++) {
            minX = Math.min(minX, this.lons[i]);
            maxX = Math.max(maxX, this.lons[i]);
            minY = Math.min(minY, this.lats[i]);
            maxY = Math.max(maxY, this.lats[i]);
        }
        super.setRect(minX, minY, maxX - minX, maxY - minY);
        this.minX = minX;
        this.minY = minY;
        this.boundsWidth = maxX - minX;
        this.boundsHeight = maxY - minY;
    }

    /**
     * True if two edges that do not share a vertex touch or cross; such polygons are invalid for spatial queries.
     */
    static boolean isSelfIntersecting(double[] lons, double[] lats, int count) {
        for (int i = 0; i < count; i++) {
            int i1 = (i + 1) % count;
            for (int j = i + 1; j < count; j++) {
                int j1 = (j + 1) % count;
                if (i1 == j || j1 == i) {
                    continue; // adjacent edges share a vertex
                }
                if (Line2D.linesIntersect(lons[i], lats[i], lons[i1], lats[i1], lons[j], lats[j], lons[j1], lats[j1])) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getVertexCount() {
        return this.lons.length;
    }

    public double getLongitude(int index) {
        return this.lons[index];
    }

    public double getLatitude(int index) {
        return this.lats[index];
    }

    /**
     * The closed outline of the polygon.
     */
    public Path2D.Double toPath() {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(this.lons[0], this.lats[0]);
        for (int i = 1; i < this.lons.length; i++) {
            path.lineTo(this.lons[i], this.lats[i]);
        }
        path.closePath();
        return path;
    }

    @Override
    public double getX() {
        return this.minX;
    }

    @Override
    public double getY() {
        return this.minY;
    }

    @Override
    public double getWidth() {
        return this.boundsWidth;
    }

    @Override
    public double getHeight() {
        return this.boundsHeight;
    }

    @Override
    public boolean isEmpty() {
        return this.boundsWidth <= 0.0 || this.boundsHeight <= 0.0;
    }

    @Override
    public Rectangle2D getBounds2D() {
        return new Rectangle2D.Double(this.minX, this.minY, this.boundsWidth, this.boundsHeight);
    }

    @Override
    public int outcode(double px, double py) {
        return getBounds2D().outcode(px, py);
    }

    @Override
    public Rectangle2D createIntersection(Rectangle2D r) {
        return getBounds2D().createIntersection(r);
    }

    @Override
    public Rectangle2D createUnion(Rectangle2D r) {
        return getBounds2D().createUnion(r);
    }

    @Override
    public void setRect(Rectangle2D r) {
        throw new UnsupportedOperationException("The bounds of a polygon area cannot be changed.");
    }

    @Override
    public void setRect(double x, double y, double w, double h) {
        // the bounds follow the vertices; Rectangle2D.Double calls this only from the constructors used above
        if (this.lons != null) {
            throw new UnsupportedOperationException("The bounds of a polygon area cannot be changed.");
        }
        super.setRect(x, y, w, h);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PolygonArea)) {
            return false;
        }
        PolygonArea polygon = (PolygonArea) other;
        return Arrays.equals(this.lons, polygon.lons) && Arrays.equals(this.lats, polygon.lats);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(this.lons) + Arrays.hashCode(this.lats);
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("PolygonArea[");
        for (int i = 0; i < this.lons.length; i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(this.lons[i]).append(' ').append(this.lats[i]);
        }
        return text.append(']').toString();
    }
}
