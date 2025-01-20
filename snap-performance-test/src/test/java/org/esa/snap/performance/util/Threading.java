package org.esa.snap.performance.util;

public enum Threading {
    SINGLE("Single"),
    MULTI("Multi");

    private final String name;

    Threading(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static Threading matchStringToEnum(String str) {
        for (Threading threading : Threading.values()) {
            if (threading.getName().toLowerCase()
                    .equals(str.toLowerCase())) {
                return threading;
            }
        }
        return SINGLE;
    }
}
