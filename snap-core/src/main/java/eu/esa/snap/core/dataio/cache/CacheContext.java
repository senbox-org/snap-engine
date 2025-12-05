package eu.esa.snap.core.dataio.cache;

class CacheContext {

    VariableDescriptor variableDescriptor;
    CacheDataProvider dataProvider;

    public CacheContext(VariableDescriptor variableDescriptor, CacheDataProvider dataProvider) {
        this.variableDescriptor = variableDescriptor;
        this.dataProvider = dataProvider;
    }

    public VariableDescriptor getVariableDescriptor() {
        return variableDescriptor;
    }

    public CacheDataProvider getDataProvider() {
        return dataProvider;
    }
}
