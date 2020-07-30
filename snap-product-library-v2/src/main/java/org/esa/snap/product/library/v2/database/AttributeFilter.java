package org.esa.snap.product.library.v2.database;

import org.esa.snap.remote.products.repository.Attribute;

/**
 * The data about an attribute.
 *
 * Created by jcoravu on 24/9/2019.
 */
public class AttributeFilter {

    private final String name;
    private final String value;
    private final AttributeValueFilter valueFilter;

    public AttributeFilter(String name, String value, AttributeValueFilter attributeValueFilter) {
        this.name = name;
        this.value = value;
        this.valueFilter = attributeValueFilter;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean matches(Attribute attribute) {
        if (attribute.getName().equalsIgnoreCase(this.name) && this.valueFilter.matches(attribute.getValue(), this.value)) {
            return true;
        }
        return false;
    }

    public AttributeValueFilter getValueFilter() {
        return valueFilter;
    }
}
