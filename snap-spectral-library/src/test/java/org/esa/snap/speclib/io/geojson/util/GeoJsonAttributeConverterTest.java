package org.esa.snap.speclib.io.geojson.util;

import com.bc.ceres.annotation.STTM;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.esa.snap.speclib.model.AttributeType;
import org.esa.snap.speclib.model.AttributeValue;
import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


public class GeoJsonAttributeConverterTest {


    private final ObjectMapper mapper = new ObjectMapper();


    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_null_returnsNull() {
        assertNull(GeoJsonAttributeConverter.fromJsonNode(null));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_jsonNull_returnsNull() {
        assertNull(GeoJsonAttributeConverter.fromJsonNode(mapper.nullNode()));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_booleanTrue() {
        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(mapper.getNodeFactory().booleanNode(true));

        assertEquals(AttributeType.BOOLEAN, av.getType());
        assertTrue(av.asBoolean());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_booleanFalse() {
        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(mapper.getNodeFactory().booleanNode(false));
        assertFalse(av.asBoolean());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_intInRange_returnsInt() {
        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(mapper.getNodeFactory().numberNode(42));

        assertEquals(AttributeType.INT, av.getType());
        assertEquals(42, av.asInt());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_longOutOfIntRange_returnsLong() {
        long big = (long) Integer.MAX_VALUE + 1L;
        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(mapper.getNodeFactory().numberNode(big));

        assertEquals(AttributeType.LONG, av.getType());
        assertEquals(big, av.asLong());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_longMinBoundary_returnsInt() {
        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(mapper.getNodeFactory().numberNode((long) Integer.MIN_VALUE));
        assertEquals(AttributeType.INT, av.getType());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_double_returnsDouble() {
        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(mapper.getNodeFactory().numberNode(3.14));

        assertEquals(AttributeType.DOUBLE, av.getType());
        assertEquals(3.14, av.asDouble(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_plainString_returnsString() {
        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(mapper.getNodeFactory().textNode("hello"));

        assertEquals(AttributeType.STRING, av.getType());
        assertEquals("hello", av.asString());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_iso8601String_returnsInstant() {
        String ts = "2024-06-15T12:00:00Z";
        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(mapper.getNodeFactory().textNode(ts));

        assertEquals(AttributeType.INSTANT, av.getType());
        assertEquals(Instant.parse(ts), av.asInstant());
    }


    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_emptyArray_returnsEmptyStringList() {
        ArrayNode arr = mapper.createArrayNode();
        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(arr);

        assertEquals(AttributeType.STRING_LIST, av.getType());
        assertTrue(av.asStringList().isEmpty());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_intArray_returnsIntArray() {
        ArrayNode arr = mapper.createArrayNode();
        arr.add(1); arr.add(2); arr.add(3);
        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(arr);

        assertEquals(AttributeType.INT_ARRAY, av.getType());
        assertArrayEquals(new int[]{1, 2, 3}, av.asIntArray());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_doubleArray_returnsDoubleArray() {
        ArrayNode arr = mapper.createArrayNode();
        arr.add(1.1); arr.add(2.2);
        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(arr);

        assertEquals(AttributeType.DOUBLE_ARRAY, av.getType());
        assertArrayEquals(new double[]{1.1, 2.2}, av.asDoubleArray(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_mixedIntAndDouble_treatedAsDoubleArray() {
        ArrayNode arr = mapper.createArrayNode();
        arr.add(1); arr.add(2.5);

        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(arr);
        assertEquals(AttributeType.DOUBLE_ARRAY, av.getType());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_stringArray_returnsStringList() {
        ArrayNode arr = mapper.createArrayNode();
        arr.add("a"); arr.add("b");
        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(arr);

        assertEquals(AttributeType.STRING_LIST, av.getType());
        assertEquals(List.of("a", "b"), av.asStringList());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_arrayOfObjects_fallsBackToString() {
        ArrayNode arr = mapper.createArrayNode();
        arr.addObject().put("k", "v");

        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(arr);
        assertEquals(AttributeType.STRING, av.getType());
    }


    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_allStringObject_returnsStringMap() {
        ObjectNode obj = mapper.createObjectNode();
        obj.put("a", "1"); obj.put("b", "2");
        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(obj);

        assertEquals(AttributeType.STRING_MAP, av.getType());
        assertEquals(Map.of("a", "1", "b", "2"), av.asStringMap());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_fromJsonNode_objectWithNonStringValue_fallsBackToJsonString() {
        ObjectNode obj = mapper.createObjectNode();
        obj.put("a", "text");
        obj.put("b", 42);

        AttributeValue av = GeoJsonAttributeConverter.fromJsonNode(obj);
        assertEquals(AttributeType.STRING, av.getType());
        assertTrue(av.asString().contains("\"b\""));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_toJsonNode_string() {
        ObjectNode parent = mapper.createObjectNode();
        GeoJsonAttributeConverter.toJsonNode(parent, "k", AttributeValue.ofString("val"));

        assertEquals("val", parent.get("k").asText());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toJsonNode_int() {
        ObjectNode parent = mapper.createObjectNode();
        GeoJsonAttributeConverter.toJsonNode(parent, "k", AttributeValue.ofInt(7));

        assertEquals(7, parent.get("k").intValue());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toJsonNode_long() {
        ObjectNode parent = mapper.createObjectNode();
        long big = (long) Integer.MAX_VALUE + 100L;

        GeoJsonAttributeConverter.toJsonNode(parent, "k", AttributeValue.ofLong(big));
        assertEquals(big, parent.get("k").longValue());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toJsonNode_double() {
        ObjectNode parent = mapper.createObjectNode();
        GeoJsonAttributeConverter.toJsonNode(parent, "k", AttributeValue.ofDouble(2.71));

        assertEquals(2.71, parent.get("k").doubleValue(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toJsonNode_boolean() {
        ObjectNode parent = mapper.createObjectNode();
        GeoJsonAttributeConverter.toJsonNode(parent, "k", AttributeValue.ofBoolean(true));

        assertTrue(parent.get("k").booleanValue());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toJsonNode_instant() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        ObjectNode parent = mapper.createObjectNode();

        GeoJsonAttributeConverter.toJsonNode(parent, "k", AttributeValue.ofInstant(now));
        assertEquals(now.toString(), parent.get("k").asText());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toJsonNode_stringList() {
        ObjectNode parent = mapper.createObjectNode();
        GeoJsonAttributeConverter.toJsonNode(parent, "k", AttributeValue.ofStringList(List.of("x", "y")));
        ArrayNode arr = (ArrayNode) parent.get("k");

        assertEquals(2, arr.size());
        assertEquals("x", arr.get(0).asText());
        assertEquals("y", arr.get(1).asText());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toJsonNode_doubleArray() {
        ObjectNode parent = mapper.createObjectNode();
        GeoJsonAttributeConverter.toJsonNode(parent, "k", AttributeValue.ofDoubleArray(new double[]{1.0, 2.0}));
        ArrayNode arr = (ArrayNode) parent.get("k");

        assertEquals(2, arr.size());
        assertEquals(1.0, arr.get(0).doubleValue(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toJsonNode_intArray() {
        ObjectNode parent = mapper.createObjectNode();
        GeoJsonAttributeConverter.toJsonNode(parent, "k", AttributeValue.ofIntArray(new int[]{3, 4}));
        ArrayNode arr = (ArrayNode) parent.get("k");

        assertEquals(2, arr.size());
        assertEquals(3, arr.get(0).intValue());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toJsonNode_stringMap() {
        ObjectNode parent = mapper.createObjectNode();
        GeoJsonAttributeConverter.toJsonNode(parent, "k", AttributeValue.ofStringMap(Map.of("foo", "bar")));
        ObjectNode obj = (ObjectNode) parent.get("k");

        assertEquals("bar", obj.get("foo").asText());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_toJsonNode_embeddedSpectrum_silentlySkipped() {
        ObjectNode parent = mapper.createObjectNode();
        var axis = new org.esa.snap.speclib.model.SpectralAxis(new double[]{400.0}, "nm");
        var sig  = org.esa.snap.speclib.model.SpectralSignature.of(new double[]{1.0});
        var es   = new AttributeValue.EmbeddedSpectrum(axis, sig);

        GeoJsonAttributeConverter.toJsonNode(parent, "k", AttributeValue.ofEmbeddedSpectrum(es));
        assertNull(parent.get("k"));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_string() {
        assertRoundtrip(AttributeValue.ofString("hello"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_int() {
        assertRoundtrip(AttributeValue.ofInt(99));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_double() {
        ObjectNode parent = mapper.createObjectNode();
        AttributeValue original = AttributeValue.ofDouble(1.23);
        GeoJsonAttributeConverter.toJsonNode(parent, "v", original);

        AttributeValue result = GeoJsonAttributeConverter.fromJsonNode(parent.get("v"));
        assertEquals(AttributeType.DOUBLE, result.getType());
        assertEquals(original.asDouble(), result.asDouble(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_boolean() {
        assertRoundtrip(AttributeValue.ofBoolean(false));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_instant() {
        ObjectNode parent = mapper.createObjectNode();
        AttributeValue original = AttributeValue.ofInstant(Instant.parse("2023-03-15T08:30:00Z"));
        GeoJsonAttributeConverter.toJsonNode(parent, "v", original);

        AttributeValue result = GeoJsonAttributeConverter.fromJsonNode(parent.get("v"));
        assertEquals(AttributeType.INSTANT, result.getType());
        assertEquals(original.asInstant(), result.asInstant());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_stringList() {
        ObjectNode parent = mapper.createObjectNode();
        AttributeValue original = AttributeValue.ofStringList(List.of("a", "b", "c"));
        GeoJsonAttributeConverter.toJsonNode(parent, "v", original);

        AttributeValue result = GeoJsonAttributeConverter.fromJsonNode(parent.get("v"));
        assertEquals(AttributeType.STRING_LIST, result.getType());
        assertEquals(original.asStringList(), result.asStringList());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_intArray() {
        ObjectNode parent = mapper.createObjectNode();
        AttributeValue original = AttributeValue.ofIntArray(new int[]{10, 20, 30});
        GeoJsonAttributeConverter.toJsonNode(parent, "v", original);

        AttributeValue result = GeoJsonAttributeConverter.fromJsonNode(parent.get("v"));
        assertEquals(AttributeType.INT_ARRAY, result.getType());
        assertArrayEquals(original.asIntArray(), result.asIntArray());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_doubleArray() {
        ObjectNode parent = mapper.createObjectNode();
        AttributeValue original = AttributeValue.ofDoubleArray(new double[]{1.1, 2.2, 3.3});
        GeoJsonAttributeConverter.toJsonNode(parent, "v", original);

        AttributeValue result = GeoJsonAttributeConverter.fromJsonNode(parent.get("v"));
        assertEquals(AttributeType.DOUBLE_ARRAY, result.getType());
        assertArrayEquals(original.asDoubleArray(), result.asDoubleArray(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_stringMap() {
        ObjectNode parent = mapper.createObjectNode();
        AttributeValue original = AttributeValue.ofStringMap(Map.of("x", "1", "y", "2"));
        GeoJsonAttributeConverter.toJsonNode(parent, "v", original);

        AttributeValue result = GeoJsonAttributeConverter.fromJsonNode(parent.get("v"));
        assertEquals(AttributeType.STRING_MAP, result.getType());
        assertEquals(original.asStringMap(), result.asStringMap());
    }


    private void assertRoundtrip(AttributeValue original) {
        ObjectNode parent = mapper.createObjectNode();
        GeoJsonAttributeConverter.toJsonNode(parent, "v", original);

        AttributeValue result = GeoJsonAttributeConverter.fromJsonNode(parent.get("v"));
        assertEquals(original.getType(), result.getType());
        assertEquals(original.raw(), result.raw());
    }
}
