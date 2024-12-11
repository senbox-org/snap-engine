package org.esa.snap.oldImpl.performance.util;

public enum Unit {
    MS("ms"),
    MB_PER_S("MB/s"),
    MB("MB");

    private final String name;

    Unit(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
