package org.esa.snap.remote.products.repository.tao.usgs;

import org.esa.snap.remote.products.repository.tao.AbstractTAORepositoryProduct;
import ro.cs.tao.eodata.EOProduct;

/**
 * Created by jcoravu on 26/8/2019.
 */
public class USGSRepositoryProduct extends AbstractTAORepositoryProduct {

    public USGSRepositoryProduct(EOProduct product, String mission) {
        super(product, mission);
    }

    EOProduct getProduct() {
        return this.product;
    }
}
