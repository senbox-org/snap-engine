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

    @Override
    public String getType() {
        return this.product.getAttributeValue("producttype");
    }

    @Override
    public String getInstrument() {
        return this.product.getAttributeValue("instrumentshortname");
    }
}
