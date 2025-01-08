package org.esa.snap.performance.actions;

import org.esa.snap.performance.util.Result;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

public class MemoryConsumptionAction implements Action, NestedAction {

    private final Action nestedAction;
    private List<Result> allResults;

    public MemoryConsumptionAction(Action nestedAction) {
        this.nestedAction = nestedAction;
    }

    @Override
    public void execute() throws Throwable {
        this.allResults = new ArrayList<>();

        resetMemoryPeaks();
        nestedAction.execute();
        long memoryConsumption = getPeakMemoryUsage();

        List<Result> results = this.nestedAction.fetchResults();
        results.add(new Result(ActionName.MEMORY.getName(), true, memoryConsumption / (1024.0 * 1024.0), "MB"));
        this.allResults = results;
    }

    @Override
    public void cleanUp() {
        this.nestedAction.cleanUp();
    }

    @Override
    public List<Result> fetchResults() {
        return this.allResults;
    }

    private void resetMemoryPeaks() {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.isValid()) {
                pool.resetPeakUsage();
            }
        }
    }

    private long getPeakMemoryUsage() {
        long maxMemory = 0;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.isValid()) {
                MemoryUsage peak = pool.getPeakUsage();
                maxMemory += peak.getUsed();
            }
        }
        return maxMemory;
    }

    @Override
    public Action getNestedAction() {
        return this.nestedAction;
    }
}
