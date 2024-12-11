package org.esa.snap.oldImpl.performance.util;

import java.io.File;

public class CalculationContainer {

    private final long[] times;
    private final File[] files;
    private final double maxMemoryConsumptionInMB;

    public CalculationContainer(long[] times, File[] files, double maxMemoryConsumptionInMB) {
        this.times = times;
        this.files = files;
        this.maxMemoryConsumptionInMB = maxMemoryConsumptionInMB;
    }

    public long[] getTimes() {
        return times;
    }

    public File[] getFiles() {
        return files;
    }

    public double getMaxMemoryConsumptionInMB() {
        return maxMemoryConsumptionInMB;
    }
}
