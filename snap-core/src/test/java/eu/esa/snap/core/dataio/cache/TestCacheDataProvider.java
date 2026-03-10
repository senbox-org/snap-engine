package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

public class TestCacheDataProvider implements CacheDataProvider {

    private final int[] shapes;
    private final int[] cacheSizes;
    private final int dataType;
    public TestCacheDataProvider(int[] shapes, int[] cacheSizes, int dataType) {
        this.shapes = shapes;
        this.cacheSizes = cacheSizes;
        this.dataType = dataType;
    }

    @Override
    public VariableDescriptor getVariableDescriptor(String variableName) {
        VariableDescriptor variableDescriptor = new VariableDescriptor();
        if (shapes.length == 2) {
            variableDescriptor.width = shapes[1];
            variableDescriptor.height = shapes[0];
            variableDescriptor.layers = -1;
            variableDescriptor.tileWidth = cacheSizes[1];
            variableDescriptor.tileHeight = cacheSizes[0];
            variableDescriptor.tileLayers = -1;
        } else if (shapes.length == 3) {
            variableDescriptor.width = shapes[2];
            variableDescriptor.height = shapes[1];
            variableDescriptor.layers = shapes[0];
            variableDescriptor.tileWidth = cacheSizes[2];
            variableDescriptor.tileHeight = cacheSizes[1];
            variableDescriptor.tileLayers = cacheSizes[0];
        }

        return variableDescriptor;
    }

    @Override
    public DataBuffer readCacheBlock(String variableName, int[] offsets, int[] shapes, ProductData targetData) {
        int numElems = 0;
        if (shapes.length == 2) {
            numElems = shapes[0] * shapes[1];
        } else if (shapes.length == 3) {
            numElems = shapes[0] * shapes[1] * shapes[2];
        }

        final ProductData productData = ProductData.createInstance(dataType, numElems);
        return new DataBuffer(productData, offsets, shapes);
    }

    private static @NonNull ProductData createTargetDataBuffer(int[] shapes, int rasterDataType) throws
            IOException {
        ProductData targetData;
        if (shapes.length == 2) {
            targetData = ProductData.createInstance(rasterDataType, shapes[0] * shapes[1]);
        } else if (shapes.length == 3) {
            targetData = ProductData.createInstance(rasterDataType, shapes[0] * shapes[1] * shapes[2]);
        } else {
            throw new IOException("Illegal shaped variable");
        }
        return targetData;
    }
}
