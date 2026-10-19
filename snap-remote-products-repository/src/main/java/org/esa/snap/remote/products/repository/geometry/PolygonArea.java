package org.esa.snap.remote.products.repository.geometry;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * An area of interest drawn as a polygon, in degrees (x = longitude, y = latitude).
 * <p>
 * The area of interest of the product library is a {@link Rectangle2D}; a polygon area is one too, with the bounds of
 * the polygon. Code that knows polygon areas ({@link GeometryUtils#buildPolygon(Rectangle2D)}, the remote repository
 * queries) uses the vertices; code that only knows rectangles searches the bounding box, which contains the polygon.
 */
public class PolygonArea extends Rectangle2D.Double {

    private final double[] lons;
    private final double[] lats;

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
