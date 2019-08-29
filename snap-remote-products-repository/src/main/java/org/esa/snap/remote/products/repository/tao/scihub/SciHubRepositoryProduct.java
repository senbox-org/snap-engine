package org.esa.snap.remote.products.repository.tao.scihub;

import org.esa.snap.remote.products.repository.tao.AbstractTAORepositoryProduct;
import ro.cs.tao.eodata.EOProduct;

/**
 * Created by jcoravu on 26/8/2019.
 */
public class SciHubRepositoryProduct extends AbstractTAORepositoryProduct {

    public SciHubRepositoryProduct(EOProduct product, String mission) {
        super(product, mission);
    }

    @Override
    public String getType() {
        return this.product.getAttributeValue("producttype");
    }

    @Override
    public String getInstrument() {
        return this.product.getAttributeValue("instrumentshortname");
    }

    EOProduct getProduct() {
        return this.product;
    }
}
