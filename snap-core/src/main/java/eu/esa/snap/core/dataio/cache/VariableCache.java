package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;

interface VariableCache {

    ProductData read(int[] offsets, int[] shapes, DataBuffer targetBuffer) throws IOException;

    long getSizeInBytes();

    long getLastAccessTime();

    void dispose();
}
