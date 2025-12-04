package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

public class TestCacheDataProvider implements CacheDataProvider {

    @Override
    public VariableDescriptor getVariableDescriptor(String variableName) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ProductData readCacheBlock(String variableName, int[] offsets, int[] shapes, ProductData targetData) {
        throw new RuntimeException("not implemented");
    }
}
