package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;

interface VariableCache {

    ProductData read(int[] offsets, int[] shapes, int[] targetOffsets, int[] targetShapes, ProductData targetData) throws IOException;

    long getSizeInBytes();

    void dispose();
}
