package org.esa.snap.performance.util;

public class Result {

    private final String name;
    private final boolean measurment;
    private final Object value;
    private final String unit;

    public Result(String name, boolean measurment, Object value, String unit) {
        this.name = name;
        this.measurment = measurment;
        this.value = value;
        this.unit = unit;
    }

    public String getName() {
        return this.name;
    }

    public boolean isMeasurment() {
        return this.measurment;
    }

    public Object getValue() {
        return this.value;
    }


    public String getUnit() {
        return this.unit;
    }

    @Override
    public String toString() {
        return "Result{" +
                "name='" + this.name + '\'' +
                ", value=" + this.value +
                ", unit='" + this.unit + '\'' +
                '}';
    }
}
