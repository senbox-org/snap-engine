package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;

public interface CacheDataProvider {

    VariableDescriptor getVariableDescriptor(String variableName) throws IOException;

    ProductData readCacheBlock(String variableName, int[] offsets, int[] shapes, ProductData targetData) throws IOException;
}
