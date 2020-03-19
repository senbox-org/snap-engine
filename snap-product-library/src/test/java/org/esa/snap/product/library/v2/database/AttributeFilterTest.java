package org.esa.snap.product.library.v2.database;

import org.esa.snap.remote.products.repository.Attribute;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by jcoravu on 20/1/2020.
 */
public class AttributeFilterTest {

    public AttributeFilterTest() {
    }

    @Test
    public void testMatches() {
        AttributeValueFilter attributeValueFilter = new AttributeValueFilter() {
            @Override
            public boolean matches(String attributeValue, String valueToCheck) {
                return attributeValue.equalsIgnoreCase(valueToCheck);
            }
        };
        AttributeFilter attributeFilter = new AttributeFilter("name", "value", attributeValueFilter);
        boolean result = attributeFilter.matches(new Attribute("name", "value"));
        assertEquals(true, result);

        result = attributeFilter.matches(new Attribute("Name", "valUE"));
        assertEquals(true, result);

        result = attributeFilter.matches(new Attribute("NameAttribute", "valUE"));
        assertEquals(false, result);
    }
}
