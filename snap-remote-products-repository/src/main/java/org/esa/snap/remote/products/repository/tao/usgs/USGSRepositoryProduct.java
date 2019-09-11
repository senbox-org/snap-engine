package org.esa.snap.remote.products.repository.tao.usgs;

import org.esa.snap.remote.products.repository.Polygon2D;
import org.esa.snap.remote.products.repository.tao.AbstractTAORepositoryProduct;
import ro.cs.tao.eodata.EOProduct;

/**
 * Created by jcoravu on 26/8/2019.
 */
public class USGSRepositoryProduct extends AbstractTAORepositoryProduct {

    public USGSRepositoryProduct(EOProduct product, String mission, Polygon2D polygon) {
        super(product, mission, polygon);
    }

    EOProduct getProduct() {
        return this.product;
    }
}
