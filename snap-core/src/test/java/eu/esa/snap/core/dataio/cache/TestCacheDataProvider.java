package eu.esa.snap.core.dataio.cache;

public class TestCacheDataProvider implements CacheDataProvider {

    @Override
    public VariableDescriptor getVariableDescriptor(String variableName) {
        throw new RuntimeException("not implemented");
    }
}
