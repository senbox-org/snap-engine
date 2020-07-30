package org.esa.snap.remote.products.repository;

/**
 * The type of the sensor of a downloaded product.
 *
 * Created by jcoravu on 9/9/2019.
 */
public enum SensorType {

    OPTICAL(1, "Optical"),
    RADAR(2, "Radar"),
    ALTIMETRIC(3, "Altimetric"),
    ATMOSPHERIC(4, "Atmospheric"),
    UNKNOWN(5, "Unknown");

    private final int value;
    private final String name;

    SensorType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getName() { return this.name; }

    public int getValue() { return this.value; }
}
