package org.esa.snap.remote.products.repository.tao.scihub;

import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.Polygon2D;
import org.esa.snap.remote.products.repository.tao.AbstractTAORepositoryProduct;
import ro.cs.tao.eodata.EOProduct;

/**
 * Created by jcoravu on 26/8/2019.
 */
public class SciHubRepositoryProduct extends AbstractTAORepositoryProduct {

    public SciHubRepositoryProduct(EOProduct product, String mission, Polygon2D polygon) {
        super(product, mission, polygon);
    }

    EOProduct getProduct() {
        return this.product;
    }
}
