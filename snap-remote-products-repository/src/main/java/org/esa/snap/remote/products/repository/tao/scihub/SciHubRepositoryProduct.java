package org.esa.snap.remote.products.repository.tao.scihub;

import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.tao.AbstractTAORepositoryProduct;
import ro.cs.tao.eodata.EOProduct;

/**
 * Created by jcoravu on 26/8/2019.
 */
public class SciHubRepositoryProduct extends AbstractTAORepositoryProduct {

    public SciHubRepositoryProduct(EOProduct product, String mission) {
        super(product, mission);

        this.attributes = new Attribute[2];
        this.attributes[0] = new Attribute("Product type", product.getAttributeValue("producttype"));
        this.attributes[1] = new Attribute("Instrument", product.getAttributeValue("instrumentshortname"));
    }

    EOProduct getProduct() {
        return this.product;
    }
}
