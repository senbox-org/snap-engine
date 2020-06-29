package org.esa.snap.core.dataio.dimap;

import org.esa.snap.core.datamodel.Band;

import java.util.HashMap;

class WriteCache {

    private final HashMap<String, VariableCache> variableMap;

    WriteCache() {
        variableMap = new HashMap<>();
    }

    VariableCache get(Band band) {
        VariableCache variableCache = variableMap.get(band.getName());
        if (variableCache == null) {
            variableCache = new VariableCache(band);
            variableMap.put(band.getName(), variableCache);
        }
        return variableCache;
    }

    void flush() {

    }


}
