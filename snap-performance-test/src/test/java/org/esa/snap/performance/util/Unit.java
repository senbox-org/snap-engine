package org.esa.snap.performance.util;

public enum Unit {
    MS("ms"),
    MB_S("Mb/s");

    private final String name;

    Unit(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
