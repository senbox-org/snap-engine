package org.esa.snap.speclib.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;


public class SpectralProfile {


    private final UUID id;
    private final String name;
    private final double[] wavelengths;
    private final double[] values;
    private final String unit;


    public SpectralProfile(UUID id, String name, double[] wavelengths, double[] values, String unit) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.unit = Objects.requireNonNull(unit, "unit must not be null");

        Objects.requireNonNull(wavelengths, "wavelengths must not be null");
        Objects.requireNonNull(values, "values must not be null");

        if (wavelengths.length != values.length) {
            throw new IllegalArgumentException("wavelengths and values must have the same length");
        }
        if (wavelengths.length == 0) {
            throw new IllegalArgumentException("wavelengths/values must not be empty");
        }

        this.wavelengths = Arrays.copyOf(wavelengths, wavelengths.length);
        this.values = Arrays.copyOf(values, values.length);
    }

    public static SpectralProfile create(String name, double[] wavelengths, double[] values, String unit) {
        return new SpectralProfile(UUID.randomUUID(), name, wavelengths, values, unit);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public double[] getWavelengths() {
        return Arrays.copyOf(wavelengths, wavelengths.length);
    }

    public double[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    public int size() {
        return values.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpectralProfile)) return false;
        SpectralProfile that = (SpectralProfile) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
