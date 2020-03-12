package org.esa.snap.product.library.v2.database;

import org.esa.snap.remote.products.repository.Attribute;

/**
 * Created by jcoravu on 24/9/2019.
 */
public class AttributeFilter extends Attribute {

    private final AttributeValueFilter valueFilter;

    public AttributeFilter(String name, String value, AttributeValueFilter attributeValueFilter) {
        super(name, value);

        this.valueFilter = attributeValueFilter;
    }

    public boolean matches(Attribute attribute) {
        if (attribute.getName().equalsIgnoreCase(getName()) && this.valueFilter.matches(attribute.getValue(), getValue())) {
            return true;
        }
        return false;
    }

    public AttributeValueFilter getValueFilter() {
        return valueFilter;
    }
}
