package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;

class MockProvider implements CacheDataProvider {

    private final int dataType;

    public MockProvider(int dataType) {
        this.dataType = dataType;
    }

    @Override
    public VariableDescriptor getVariableDescriptor(String variableName) throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ProductData readCacheBlock(String variableName, int[] offsets, int[] shapes, ProductData targetData) throws IOException {
        int numElems;
        if (shapes.length == 2) {
            numElems = shapes[0] * shapes[1];
        } else if (shapes.length == 3) {
            numElems = shapes[0] * shapes[1] * shapes[2];
        } else {
            throw new RuntimeException("unsupported dimensionality");
        }

        return ProductData.createInstance(dataType, numElems);
    }
}
