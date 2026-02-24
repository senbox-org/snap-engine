package org.esa.snap.speclib.model;

import java.util.Arrays;
import java.util.Objects;


public class SpectralSignature {


    private final double[] values;
    private final String yUnit;


    private SpectralSignature(double[] values, String yUnit) {
        Objects.requireNonNull(values, "values must not be null");
        if (values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }
        this.values = Arrays.copyOf(values, values.length);
        this.yUnit = yUnit;
    }


    public static SpectralSignature of(double[] values) {
        return new SpectralSignature(values, null);
    }

    public static SpectralSignature of(double[] values, String yUnit) {
        Objects.requireNonNull(yUnit, "yUnit must not be null");
        return new SpectralSignature(values, yUnit);
    }

    public double[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    public String getYUnitOrNull() {
        return yUnit;
    }

    public int size() {
        return values.length;
    }
}
