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

        int productTypeAttributeIndex = -1;
        int instrumentShortNameAttributeIndex = -1;
        for (int i=0; i<this.attributes.size(); i++) {
            Attribute attribute = this.attributes.get(i);
            if (attribute.getName().equalsIgnoreCase("producttype")) {
                productTypeAttributeIndex = i;
            } else if (attribute.getName().equalsIgnoreCase("instrumentshortname")) {
                instrumentShortNameAttributeIndex = i;
            }
        }
        if (productTypeAttributeIndex >= 0) {
            Attribute tempAttribute = this.attributes.get(0);
            this.attributes.set(0, this.attributes.get(productTypeAttributeIndex));
            this.attributes.set(productTypeAttributeIndex, tempAttribute);
        }
        if (instrumentShortNameAttributeIndex >= 0) {
            Attribute tempAttribute = this.attributes.get(1);
            this.attributes.set(1, this.attributes.get(instrumentShortNameAttributeIndex));
            this.attributes.set(instrumentShortNameAttributeIndex, tempAttribute);
        }
    }

    EOProduct getProduct() {
        return this.product;
    }
}
