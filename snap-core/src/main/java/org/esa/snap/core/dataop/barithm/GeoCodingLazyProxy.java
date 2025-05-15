package org.esa.snap.core.dataop.barithm;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

public class GeoCodingLazyProxy implements GeoCoding {

    private Product product;
    private GeoCoding geoCoding;

    public GeoCodingLazyProxy(Product product) {
        this.product = product;
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        ensureGeoCoding();
        return geoCoding.isCrossingMeridianAt180();
    }

    @Override
    public boolean canGetPixelPos() {
        ensureGeoCoding();
        return geoCoding.canGetPixelPos();
    }

    @Override
    public boolean canGetGeoPos() {
        ensureGeoCoding();
        return geoCoding.canGetGeoPos();
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        ensureGeoCoding();
        return geoCoding.getPixelPos(geoPos, pixelPos);
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        ensureGeoCoding();
        return geoCoding.getGeoPos(pixelPos, geoPos);
    }

    @Override
    public Datum getDatum() {
        ensureGeoCoding();
        return geoCoding.getDatum();
    }

    @Override
    public void dispose() {
        if (geoCoding != null) {
            geoCoding.dispose();
            geoCoding = null;
        }
        product = null;
    }

    @Override
    public CoordinateReferenceSystem getImageCRS() {
        ensureGeoCoding();
        return geoCoding.getImageCRS();
    }

    @Override
    public CoordinateReferenceSystem getMapCRS() {
        ensureGeoCoding();
        return geoCoding.getMapCRS();
    }

    @Override
    public CoordinateReferenceSystem getGeoCRS() {
        ensureGeoCoding();
        return geoCoding.getGeoCRS();
    }

    @Override
    public MathTransform getImageToMapTransform() {
        ensureGeoCoding();
        return geoCoding.getImageToMapTransform();
    }

    @Override
    public GeoCoding clone() {
        ensureGeoCoding();
        return geoCoding.clone();
    }

    @Override
    public boolean canClone() {
        ensureGeoCoding();
        return geoCoding.canClone();
    }

    private void ensureGeoCoding() {
        synchronized (this) {
            if (geoCoding == null) {
                System.out.println("Loading geocoding for product " + product.getName() + " ...");
                geoCoding = product.getSceneGeoCoding();
                if (geoCoding == null) {
                    throw new RuntimeException("No geocoding available for product " + product.getName());
                }
                System.out.println("done");
            }
        }
    }
}
