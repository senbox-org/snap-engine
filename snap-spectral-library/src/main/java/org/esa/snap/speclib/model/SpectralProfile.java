package org.esa.snap.speclib.model;

import java.util.*;


public class SpectralProfile {


    private final UUID id;
    private final String name;
    private final SpectralSignature signature;
    private final Map<String, AttributeValue> attributes;
    private final SourceRef sourceRef; // optional


    public SpectralProfile(UUID id,
                           String name,
                           SpectralSignature signature,
                           Map<String, AttributeValue> attributes,
                           SourceRef sourceRef) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(attributes, "attributes must not be null")));
        this.sourceRef = sourceRef;
    }


    public static SpectralProfile create(String name, SpectralSignature signature) {
        return new SpectralProfile(UUID.randomUUID(), name, signature, Map.of(), null);
    }

    public SpectralProfile withAttribute(String key, AttributeValue value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Map<String, AttributeValue> copy = new LinkedHashMap<>(this.attributes);
        copy.put(key, value);
        return new SpectralProfile(this.id, this.name, this.signature, copy, this.sourceRef);
    }

    public SpectralProfile withSourceRef(SourceRef ref) {
        return new SpectralProfile(this.id, this.name, this.signature, this.attributes, ref);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SpectralSignature getSignature() {
        return signature;
    }

    public Map<String, AttributeValue> getAttributes() {
        return attributes;
    }
    public Optional<AttributeValue> getAttribute(String key) {
        return Optional.ofNullable(attributes.get(key));
    }

    public Optional<SourceRef> getSourceRef() {
        return Optional.ofNullable(sourceRef);
    }

    public int size() {
        return signature.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SpectralProfile)) {
            return false;
        }
        SpectralProfile that = (SpectralProfile) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Optional link to where the profile came from (pixel extraction). */
    public static final class SourceRef {
        private final int x;
        private final int y;
        private final int level;
        private final String productId;

        public SourceRef(int x, int y, int level, String productId) {
            this.x = x;
            this.y = y;
            this.level = level;
            this.productId = productId;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getLevel() {
            return level;
        }

        public Optional<String> getProductId() {
            return Optional.ofNullable(productId);
        }
    }
}
