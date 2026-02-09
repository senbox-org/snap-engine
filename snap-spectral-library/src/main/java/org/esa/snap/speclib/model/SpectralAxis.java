package org.esa.snap.speclib.model;

import java.util.Arrays;
import java.util.Objects;


public class SpectralAxis {


    private final double[] wavelengths;
    private final String xUnit;


    public SpectralAxis(double[] wavelengths, String xUnit) {
        Objects.requireNonNull(wavelengths, "wavelengths must not be null");
        this.xUnit = Objects.requireNonNull(xUnit, "xUnit must not be null");
        if (wavelengths.length == 0) {
            throw new IllegalArgumentException("wavelengths must not be empty");
        }
        this.wavelengths = Arrays.copyOf(wavelengths, wavelengths.length);
    }


    public double[] getWavelengths() {
        return Arrays.copyOf(wavelengths, wavelengths.length);
    }

    public String getXUnit() {
        return xUnit;
    }

    public int size() {
        return wavelengths.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SpectralAxis)) {
            return false;
        }
        SpectralAxis that = (SpectralAxis) o;
        return xUnit.equals(that.xUnit) && Arrays.equals(wavelengths, that.wavelengths);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(xUnit);
        result = 31 * result + Arrays.hashCode(wavelengths);
        return result;
    }
}
