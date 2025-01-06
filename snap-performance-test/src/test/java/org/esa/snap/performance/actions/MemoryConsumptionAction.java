package org.esa.snap.performance.actions;

import org.esa.snap.performance.util.Result;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

public class MemoryConsumptionAction implements Action, NestedAction {

    private final Action nestedAction;
//    private Double result;
    private List<Result> allResults;

    public MemoryConsumptionAction(Action nestedAction) {
        this.nestedAction = nestedAction;
    }

    @Override
    public void execute() throws IOException {
        this.allResults = new ArrayList<>();
//        long memoryBefore = getUsedMemory();
        resetMemoryPeaks();
//        System.out.println("BEFORE: " + memoryBefore);
        nestedAction.execute();
//        long memoryAfter = getUsedMemory();
        long memoryConsumption = getPeakMemoryUsage();
//        System.out.println("AFTER: " + memoryAfter);
//        System.out.println("AFTER: " + memoryConsumption);
//        long memoryConsumption = memoryAfter - memoryBefore;
//        this.result = memoryConsumption / (1024.0 * 1024.0);

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
//        List<Result> results = this.nestedAction.fetchResults();
//        results.add(new Result(ActionName.MEMORY.getName(), true, this.result, "MB"));
        return this.allResults;
    }

//    private long getUsedMemory() {
//        Runtime runtime = Runtime.getRuntime();
//        return runtime.totalMemory() - runtime.freeMemory();
//    }

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
