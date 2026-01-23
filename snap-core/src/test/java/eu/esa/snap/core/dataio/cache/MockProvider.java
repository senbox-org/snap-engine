package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;

class MockProvider implements CacheDataProvider {

    private final int dataType;
    private final int[] shapes;
    private final int[] tileShapes;

    public MockProvider(int dataType) {
        this(dataType, new int[] {-1, 40, 20}, new int[] {-1, 20, 10});
    }

    public MockProvider(int dataType, int[] shapes, int[] tileShapes) {
        this.dataType = dataType;
        this.shapes = shapes;
        this.tileShapes = tileShapes;
    }

    @Override
    public VariableDescriptor getVariableDescriptor(String variableName) {
        final VariableDescriptor variableDescriptor = new VariableDescriptor();
        variableDescriptor.name = variableName;
        variableDescriptor.width = shapes[2];
        variableDescriptor.height = shapes[1];
        variableDescriptor.layers = shapes[0];
        variableDescriptor.tileWidth = tileShapes[2];
        variableDescriptor.tileHeight = tileShapes[1];
        variableDescriptor.tileLayers = tileShapes[0];
        return variableDescriptor;
    }

    @Override
    public DataBuffer readCacheBlock(String variableName, int[] offsets, int[] shapes, ProductData targetData) {
        int numElems;
        if (shapes.length == 2) {
            numElems = shapes[0] * shapes[1];
        } else if (shapes.length == 3) {
            numElems = shapes[0] * shapes[1] * shapes[2];
        } else {
            throw new RuntimeException("unsupported dimensionality");
        }

        final ProductData productData = ProductData.createInstance(dataType, numElems);
        for (int i = 0; i<  productData.getNumElems(); i++) {
            productData.setElemIntAt(i, i);
        }
        return new DataBuffer(productData, offsets, shapes);
    }
}
