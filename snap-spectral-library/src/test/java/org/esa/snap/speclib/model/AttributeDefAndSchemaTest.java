package org.esa.snap.speclib.model;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;


public class AttributeDefAndSchemaTest {


    @Test
    @STTM("SNAP-4128")
    public void test_attributeDef_basicAccessorsAndNullSafety() {
        AttributeDef def = new AttributeDef("class", AttributeType.STRING, true,
                null, "Land cover class", "category");

        assertEquals("class", def.getKey());
        assertEquals(AttributeType.STRING, def.getType());
        assertTrue(def.isRequired());
        assertTrue(def.getDefaultValue().isEmpty());
        assertEquals("Land cover class", def.getDescription().orElseThrow());
        assertEquals("category", def.getUiHint().orElseThrow());

        assertThrows(NullPointerException.class, () -> new AttributeDef(null, AttributeType.STRING, false, null, null, null));
        assertThrows(NullPointerException.class, () -> new AttributeDef("k", null, false, null, null, null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_attributeDef_rejectsDefaultValueWithMismatchedType() {
        AttributeValue wrongDefault = AttributeValue.ofInt(1);
        assertThrows(IllegalArgumentException.class, () ->
                new AttributeDef("k", AttributeType.STRING, false, wrongDefault, null, null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_attributeSchema_putFindAndInfer() {
        AttributeSchema schema = new AttributeSchema();
        assertTrue(schema.find("x").isEmpty());

        schema.put(AttributeDef.optional("x", AttributeType.DOUBLE));
        assertEquals(AttributeType.DOUBLE, schema.find("x").orElseThrow().getType());

        schema.inferFromAttributes(Map.of(
                "a", AttributeValue.ofString("v"),
                "b", AttributeValue.ofDouble(1.0)
        ));
        assertEquals(AttributeType.STRING, schema.find("a").orElseThrow().getType());
        assertEquals(AttributeType.DOUBLE, schema.find("b").orElseThrow().getType());

        schema.put(AttributeDef.optional("a", AttributeType.STRING));
        schema.inferFromAttributes(Map.of("a", AttributeValue.ofInt(5)));
        assertEquals(AttributeType.STRING, schema.find("a").orElseThrow().getType());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_schema_asMapIsUnmodifiable() {
        AttributeSchema schema = new AttributeSchema();
        schema.put(AttributeDef.optional("x", AttributeType.STRING));
        assertThrows(UnsupportedOperationException.class, () -> schema.asMap().put("y", AttributeDef.optional("y", AttributeType.STRING)));
    }


    @Test
    @STTM("SNAP-4128")
    public void test_attributeDefRequiredFactory() {
        AttributeDef def = AttributeDef.required("q", AttributeType.INT);
        assertEquals("q", def.getKey());
        assertEquals(AttributeType.INT, def.getType());
        assertTrue(def.isRequired());
    }
}
