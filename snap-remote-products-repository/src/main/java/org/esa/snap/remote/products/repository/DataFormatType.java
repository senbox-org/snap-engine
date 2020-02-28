package org.esa.snap.remote.products.repository;

/**
 * Created by jcoravu on 9/9/2019.
 */
public enum DataFormatType {

    RASTER(1, "Raster"),
    VECTOR(2, "Vector"),
    OTHER(3, "Unknown");

    private final int value;
    private final String name;

    DataFormatType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getName() { return this.name; }

    public Integer getValue() { return this.value; }
}
