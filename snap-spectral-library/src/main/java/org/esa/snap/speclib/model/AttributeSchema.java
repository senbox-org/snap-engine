package org.esa.snap.speclib.model;

import java.util.*;


public class AttributeSchema {


    private final Map<String, AttributeDef> defs;


    public AttributeSchema() {
        this.defs = new LinkedHashMap<>();
    }


    public AttributeSchema(Map<String, AttributeDef> defs) {
        Objects.requireNonNull(defs, "defs must not be null");
        this.defs = new LinkedHashMap<>(defs);
    }

    public Map<String, AttributeDef> asMap() {
        return Collections.unmodifiableMap(defs);
    }

    public Optional<AttributeDef> find(String key) {
        return Optional.ofNullable(defs.get(key));
    }

    public void put(AttributeDef def) {
        Objects.requireNonNull(def, "def must not be null");
        defs.put(def.getKey(), def);
    }

    public void inferFromAttributes(Map<String, AttributeValue> attrs) {
        Objects.requireNonNull(attrs, "attrs must not be null");
        for (Map.Entry<String, AttributeValue> e : attrs.entrySet()) {
            String k = e.getKey();
            AttributeValue v = e.getValue();
            defs.putIfAbsent(k, AttributeDef.optional(k, v.getType()));
        }
    }
}
