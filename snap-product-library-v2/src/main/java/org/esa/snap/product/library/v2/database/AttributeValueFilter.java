package org.esa.snap.product.library.v2.database;

/**
 * The interface contains a method to check if two values are equal.
 *
 * Created by jcoravu on 24/9/2019.
 */
public interface AttributeValueFilter {

    boolean matches(String attributeValue, String valueToCheck);
}
