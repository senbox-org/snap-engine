package org.esa.snap.product.library.v2;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.serialization.GeometryAdapter;

import java.awt.geom.Path2D;
import java.util.Date;

/**
 * Created by jcoravu on 12/8/2019.
 */
public class ProductLibraryItem {

    private final EOProduct product;
    private final String mission;

    public ProductLibraryItem(EOProduct product, String mission) {
        this.product = product;
        this.mission = mission;
    }

    public String getMission() {
        return mission;
    }

    EOProduct getProduct() {
        return product;
    }

    public String getName() {
        return this.product.getName();
    }

    public String getType() {
        return this.product.getAttributeValue("producttype");
    }

    public String getInstrument() {
        return this.product.getAttributeValue("instrumentshortname");
    }

    public long getApproximateSize() {
        return this.product.getApproximateSize();
    }

    public String getQuickLookLocation() {
        return this.product.getQuicklookLocation();
    }

    public String getLocation() {
        return this.product.getLocation();
    }

    public Date getAcquisitionDate() {
        return this.product.getAcquisitionDate();
    }

    public Path2D.Double computeAreaPath() throws Exception {
        GeometryAdapter geometryAdapter = new GeometryAdapter();
        Geometry geometry = geometryAdapter.marshal(product.getGeometry());
        Coordinate[] coordinates = geometry.getCoordinates();
        Path2D.Double path = new Path2D.Double();
        path.moveTo(coordinates[0].getX(), coordinates[0].getY());
        for (int k = 0; k < coordinates.length; k++) {
            path.lineTo(coordinates[k].getX(), coordinates[k].getY());
        }
        return path;
    }
}
