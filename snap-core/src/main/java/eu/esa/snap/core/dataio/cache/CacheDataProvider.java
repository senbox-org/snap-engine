package eu.esa.snap.core.dataio.cache;

public interface CacheDataProvider {

    VariableDescriptor getVariableDescriptor(String variableName);

    // readCacheBlock(name, x, y, z, w, h , l) or similar ....
}
