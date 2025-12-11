package eu.esa.snap.core.dataio.cache;

class CacheContext {

    private final VariableDescriptor variableDescriptor;
    private final CacheDataProvider dataProvider;

    CacheContext(VariableDescriptor variableDescriptor, CacheDataProvider dataProvider) {
        this.variableDescriptor = variableDescriptor;
        this.dataProvider = dataProvider;
    }

    VariableDescriptor getVariableDescriptor() {
        return variableDescriptor;
    }

    CacheDataProvider getDataProvider() {
        return dataProvider;
    }
}
