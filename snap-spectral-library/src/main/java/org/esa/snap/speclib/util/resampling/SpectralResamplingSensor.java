package org.esa.snap.speclib.util.resampling;

public enum SpectralResamplingSensor {

    ENMAP("ENMAP"),
    PRISMA("PRISMA"),
    OLCI("OLCI");

    private String name;

    SpectralResamplingSensor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
