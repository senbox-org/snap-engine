package org.esa.snap.speclib.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class AttributeValue {


    private final AttributeType type;
    private final Object value;


    private AttributeValue(AttributeType type, Object value) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null");
    }


    public static AttributeValue ofString(String v) {
        return new AttributeValue(AttributeType.STRING, Objects.requireNonNull(v));
    }

    public static AttributeValue ofInt(int v) {
        return new AttributeValue(AttributeType.INT, v);
    }

    public static AttributeValue ofLong(long v) {
        return new AttributeValue(AttributeType.LONG, v);
    }

    public static AttributeValue ofDouble(double v) {
        return new AttributeValue(AttributeType.DOUBLE, v);
    }

    public static AttributeValue ofBoolean(boolean v) {
        return new AttributeValue(AttributeType.BOOLEAN, v);
    }

    public static AttributeValue ofInstant(Instant v) {
        return new AttributeValue(AttributeType.INSTANT, v);
    }

    public static AttributeValue ofStringList(List<String> v) {
        Objects.requireNonNull(v);
        return new AttributeValue(AttributeType.STRING_LIST, List.copyOf(v));
    }

    public static AttributeValue ofDoubleArray(double[] v) {
        Objects.requireNonNull(v);
        return new AttributeValue(AttributeType.DOUBLE_ARRAY, Arrays.copyOf(v, v.length));
    }

    public static AttributeValue ofIntArray(int[] v) {
        Objects.requireNonNull(v);
        return new AttributeValue(AttributeType.INT_ARRAY, Arrays.copyOf(v, v.length));
    }

    public static AttributeValue ofStringMap(Map<String, String> v) {
        Objects.requireNonNull(v);
        return new AttributeValue(AttributeType.STRING_MAP, Map.copyOf(v));
    }

    public static AttributeValue ofEmbeddedSpectrum(EmbeddedSpectrum v) {
        Objects.requireNonNull(v);
        return new AttributeValue(AttributeType.EMBEDDED_SPECTRUM, v);
    }

    public AttributeType getType() {
        return type;
    }

    public Object raw() {
        return value;
    }

    public String asString() {
        return (String) value;
    }
    public int asInt() {
        return (Integer) value;
    }
    public long asLong() {
        return (Long) value;
    }
    public double asDouble() {
        return (Double) value;
    }
    public boolean asBoolean() {
        return (Boolean) value;
    }
    public Instant asInstant() {
        return (Instant) value;
    }

    @SuppressWarnings("unchecked")
    public List<String> asStringList() {
        return (List<String>) value;
    }

    public double[] asDoubleArray() {
        return (double[]) value;
    }
    public int[] asIntArray() {
        return (int[]) value;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> asStringMap() {
        return (Map<String, String>) value;
    }

    public EmbeddedSpectrum asEmbeddedSpectrum() {
        return (EmbeddedSpectrum) value;
    }


    public static final class EmbeddedSpectrum {
        private final SpectralAxis axis;
        private final SpectralSignature signature;

        public EmbeddedSpectrum(SpectralAxis axis, SpectralSignature signature) {
            this.axis = Objects.requireNonNull(axis, "axis must not be null");
            this.signature = Objects.requireNonNull(signature, "signature must not be null");
            if (axis.size() != signature.size()) {
                throw new IllegalArgumentException("axis size and signature size must match");
            }
        }

        public SpectralAxis getAxis() {
            return axis;
        }
        public SpectralSignature getSignature() {
            return signature;
        }
    }
}
