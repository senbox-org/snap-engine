package org.esa.snap.performance.actions;

public enum ActionName {
    MEASURE_TIME("Measured Time"),
    THROUGHPUT("Throughput"),
    MEMORY("Memory Consumption");

    private final String name;

    ActionName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
