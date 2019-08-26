package org.esa.snap.product.library.v2.scihub;

import org.esa.snap.product.library.v2.ProductLibraryItem;
import ro.cs.tao.eodata.EOProduct;

import java.util.Date;

/**
 * Created by jcoravu on 26/8/2019.
 */
public class SciHubProductLibraryItem implements ProductLibraryItem {

    private final EOProduct product;
    private final String mission;

    public SciHubProductLibraryItem(EOProduct product, String mission) {
        this.product = product;
        this.mission = mission;
    }

    @Override
    public String getMission() {
        return mission;
    }

    @Override
    public String getName() {
        return this.product.getName();
    }

    @Override
    public String getType() {
        return this.product.getAttributeValue("producttype");
    }

    @Override
    public String getInstrument() {
        return this.product.getAttributeValue("instrumentshortname");
    }

    @Override
    public long getApproximateSize() {
        return this.product.getApproximateSize();
    }

    @Override
    public String getQuickLookLocation() {
        return this.product.getQuicklookLocation();
    }

    @Override
    public String getLocation() {
        return this.product.getLocation();
    }

    @Override
    public Date getAcquisitionDate() {
        return this.product.getAcquisitionDate();
    }

//    public Path2D.Double computeAreaPath() throws Exception {
//        GeometryAdapter geometryAdapter = new GeometryAdapter();
//        Geometry geometry = geometryAdapter.marshal(product.getGeometry());
//        Coordinate[] coordinates = geometry.getCoordinates();
//        Path2D.Double path = new Path2D.Double();
//        path.moveTo(coordinates[0].getX(), coordinates[0].getY());
//        for (int k = 0; k < coordinates.length; k++) {
//            path.lineTo(coordinates[k].getX(), coordinates[k].getY());
//        }
//        return path;
//    }

    EOProduct getProduct() {
        return this.product;
    }
}
