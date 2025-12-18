package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;

class MockProvider implements CacheDataProvider {
    @Override
    public VariableDescriptor getVariableDescriptor(String variableName) throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ProductData readCacheBlock(String variableName, int[] offsets, int[] shapes, ProductData targetData) throws IOException {
        return ProductData.createInstance(ProductData.TYPE_FLOAT32, shapes[0] * shapes[1]);
    }
}
