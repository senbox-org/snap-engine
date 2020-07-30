package org.esa.snap.remote.products.repository;

/**
 * The data about an attribute of a downloaded product.
 *
 * Created by jcoravu on 3/9/2019.
 */
public class Attribute {

    private final String name;
    private final String value;

    public Attribute(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
