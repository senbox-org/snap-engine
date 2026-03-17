package eu.esa.snap.core.dataio.cache;

class CacheContext {

    private final VariableDescriptor variableDescriptor;
    private final CacheDataProvider dataProvider;
    private final MemoryUsageTracker memoryUsageTracker;

    CacheContext(VariableDescriptor variableDescriptor, CacheDataProvider dataProvider, MemoryUsageTracker memoryUsageTracker) {
        this.variableDescriptor = variableDescriptor;
        this.dataProvider = dataProvider;
        this.memoryUsageTracker = memoryUsageTracker;
    }

    VariableDescriptor getVariableDescriptor() {
        return variableDescriptor;
    }

    CacheDataProvider getDataProvider() {
        return dataProvider;
    }

    MemoryUsageTracker getMemoryUsageTracker() {
        return memoryUsageTracker;
    }
}
