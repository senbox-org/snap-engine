package org.esa.snap.core.dataio.dimap;

import org.esa.snap.core.datamodel.Band;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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

    void flush(Map<Band, ImageOutputStream> bandOutputStreams) throws IOException {
        final Set<Map.Entry<Band, ImageOutputStream>> entries = bandOutputStreams.entrySet();
        for (Map.Entry<Band, ImageOutputStream> next : entries) {
            final String bandName = next.getKey().getName();
            final VariableCache variableCache = variableMap.get(bandName);
            if (variableCache != null) {
                variableCache.flush(next.getValue());
            }
        }
    }
}
