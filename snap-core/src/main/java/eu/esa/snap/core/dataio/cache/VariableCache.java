package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

public class VariableCache {

    public VariableCache(VariableDescriptor variableDescriptor) {

    }

    public void dispose() {
        System.out.println("VarCache - dispose()");
    }

    public ProductData read(int[] offsets, int[] shapes) {
        return null;
    }
}
