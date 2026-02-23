package org.esa.snap.speclib.util;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.model.AttributeType;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralSignature;
import org.junit.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


public class SpectralLibraryAttributeValueParserTest {


    @Test
    @STTM("SNAP-4128")
    public void test_parseForType_nullTypeDefaultsToString() {
        AttributeValue v = SpectralLibraryAttributeValueParser.parseForType(null, " abc ");
        assertEquals(AttributeType.STRING, v.getType());
        assertEquals("abc", v.asString());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_parseForType_typedObjects_shortCircuit() {
        assertTrue(SpectralLibraryAttributeValueParser.parseForType(AttributeType.BOOLEAN, Boolean.TRUE).asBoolean());
        assertEquals(7, SpectralLibraryAttributeValueParser.parseForType(AttributeType.INT, Integer.valueOf(7)).asInt());
        assertEquals(9L, SpectralLibraryAttributeValueParser.parseForType(AttributeType.LONG, Long.valueOf(9L)).asLong());
        assertEquals(0.5, SpectralLibraryAttributeValueParser.parseForType(AttributeType.DOUBLE, Double.valueOf(0.5)).asDouble(), 1e-12);
        assertEquals(Instant.parse("2026-04-02T04:25:43Z"), SpectralLibraryAttributeValueParser.parseForType(AttributeType.INSTANT, Instant.parse("2026-04-02T04:25:43Z")).asInstant());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_parseForType_Instant() {
        assertEquals(AttributeType.INSTANT, SpectralLibraryAttributeValueParser.parseForType(AttributeType.INSTANT, "2026-04-02T04:25:43Z").getType());
        assertEquals(Instant.parse("2026-04-02T04:25:43Z"), SpectralLibraryAttributeValueParser.parseForType(AttributeType.INSTANT, "2026-04-02T04:25:43Z").asInstant());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_parseForType_emptyValueThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.STRING, "   "));
        assertEquals("value must not be empty", ex.getMessage());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_parseForType_int_ok_and_bad() {
        assertEquals(42, SpectralLibraryAttributeValueParser.parseForType(AttributeType.INT, "42").asInt());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.INT, "xx"));
        assertEquals("expected int", ex.getMessage());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_parseForType_long_ok_and_bad() {
        assertEquals(12345678900L, SpectralLibraryAttributeValueParser.parseForType(AttributeType.LONG, "12345678900").asLong());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.LONG, "nope"));
        assertEquals("expected long", ex.getMessage());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_parseForType_double_ok_and_bad() {
        assertEquals(0.125, SpectralLibraryAttributeValueParser.parseForType(AttributeType.DOUBLE, "0.125").asDouble(), 1e-12);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.DOUBLE, "NaN_nope"));
        assertEquals("expected double", ex.getMessage());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_parseForType_boolean_ok_variants_and_bad() {
        assertTrue(SpectralLibraryAttributeValueParser.parseForType(AttributeType.BOOLEAN, "true").asBoolean());
        assertTrue(SpectralLibraryAttributeValueParser.parseForType(AttributeType.BOOLEAN, "1").asBoolean());
        assertTrue(SpectralLibraryAttributeValueParser.parseForType(AttributeType.BOOLEAN, "YES").asBoolean());

        assertFalse(SpectralLibraryAttributeValueParser.parseForType(AttributeType.BOOLEAN, "false").asBoolean());
        assertFalse(SpectralLibraryAttributeValueParser.parseForType(AttributeType.BOOLEAN, "0").asBoolean());
        assertFalse(SpectralLibraryAttributeValueParser.parseForType(AttributeType.BOOLEAN, "No").asBoolean());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.BOOLEAN, "maybe"));
        assertEquals("expected true/false", ex.getMessage());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_parseForType_stringList_ok_and_emptyThrows() {
        AttributeValue v = SpectralLibraryAttributeValueParser.parseForType(AttributeType.STRING_LIST, "a, b, , c");
        assertEquals(AttributeType.STRING_LIST, v.getType());
        assertEquals(List.of("a", "b", "c"), v.asStringList());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.STRING_LIST, " ,  , "));
        assertEquals("string list is empty", ex.getMessage());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_parseForType_doubleArray_ok_brackets_ok_and_errors() {
        AttributeValue v1 = SpectralLibraryAttributeValueParser.parseForType(AttributeType.DOUBLE_ARRAY, "0.1, 0.2, 0.3");
        assertArrayEquals(new double[]{0.1, 0.2, 0.3}, v1.asDoubleArray(), 1e-12);

        AttributeValue v2 = SpectralLibraryAttributeValueParser.parseForType(AttributeType.DOUBLE_ARRAY, "[0.1, 0.2]");
        assertArrayEquals(new double[]{0.1, 0.2}, v2.asDoubleArray(), 1e-12);

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.DOUBLE_ARRAY, " , , "));
        assertEquals("double array is empty", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.DOUBLE_ARRAY, "0.1, nope"));
        assertEquals("invalid double: nope", ex2.getMessage());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_parseForType_intArray_ok_brackets_ok_and_errors() {
        AttributeValue v1 = SpectralLibraryAttributeValueParser.parseForType(AttributeType.INT_ARRAY, "1, 2, 3");
        assertArrayEquals(new int[]{1, 2, 3}, v1.asIntArray());

        AttributeValue v2 = SpectralLibraryAttributeValueParser.parseForType(AttributeType.INT_ARRAY, "[1, 2]");
        assertArrayEquals(new int[]{1, 2}, v2.asIntArray());

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.INT_ARRAY, " , , "));
        assertEquals("int array is empty", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.INT_ARRAY, "1, x"));
        assertEquals("invalid int: x", ex2.getMessage());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_parseForType_stringMap_ok_and_errors() {
        AttributeValue v = SpectralLibraryAttributeValueParser.parseForType(AttributeType.STRING_MAP, "k1=v1, k2=v2");
        assertEquals(AttributeType.STRING_MAP, v.getType());

        Map<String, String> m = v.asStringMap();
        assertEquals("v1", m.get("k1"));
        assertEquals("v2", m.get("k2"));
        assertEquals(2, m.size());

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.STRING_MAP, "k1v1"));
        assertEquals("invalid map entry (expected key=value): k1v1", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.STRING_MAP, "=v1"));
        assertEquals("invalid map entry (expected key=value): =v1", ex2.getMessage());

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.STRING_MAP, " , "));
        assertEquals("map is empty", ex3.getMessage());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_parseForType_embeddedSpectrum_notEditable() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> SpectralLibraryAttributeValueParser.parseForType(AttributeType.EMBEDDED_SPECTRUM, "<whatever>"));
        assertEquals("EMBEDDED_SPECTRUM is not editable in table", ex.getMessage());
    }


    @Test
    @STTM("SNAP-4128")
    public void test_cellValue_null_returnsNull() {
        assertNull(SpectralLibraryAttributeValueParser.cellValue(null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_cellValue_returnsTypedObjectsOrDisplayString() {
        assertEquals(Boolean.TRUE, SpectralLibraryAttributeValueParser.cellValue(AttributeValue.ofBoolean(true)));
        assertEquals(Integer.valueOf(3), SpectralLibraryAttributeValueParser.cellValue(AttributeValue.ofInt(3)));
        assertEquals(Long.valueOf(4L), SpectralLibraryAttributeValueParser.cellValue(AttributeValue.ofLong(4L)));
        assertEquals(Double.valueOf(0.25), SpectralLibraryAttributeValueParser.cellValue(AttributeValue.ofDouble(0.25)));

        assertEquals("hello", SpectralLibraryAttributeValueParser.cellValue(AttributeValue.ofString("hello")));
    }


    @Test
    @STTM("SNAP-4128")
    public void test_toDisplayValue_null_returnsEmpty() {
        assertEquals("", SpectralLibraryAttributeValueParser.toDisplayValue(null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_toDisplayValue_allTypes() {
        assertEquals("s", SpectralLibraryAttributeValueParser.toDisplayValue(AttributeValue.ofString("s")));
        assertEquals("1", SpectralLibraryAttributeValueParser.toDisplayValue(AttributeValue.ofInt(1)));
        assertEquals("2", SpectralLibraryAttributeValueParser.toDisplayValue(AttributeValue.ofLong(2L)));
        assertEquals("0.5", SpectralLibraryAttributeValueParser.toDisplayValue(AttributeValue.ofDouble(0.5)));
        assertEquals("true", SpectralLibraryAttributeValueParser.toDisplayValue(AttributeValue.ofBoolean(true)));
        assertEquals("a,b", SpectralLibraryAttributeValueParser.toDisplayValue(AttributeValue.ofStringList(List.of("a", "b"))));
        assertEquals("0.1,0.2", SpectralLibraryAttributeValueParser.toDisplayValue(AttributeValue.ofDoubleArray(new double[]{0.1, 0.2})));
        assertEquals("1,2", SpectralLibraryAttributeValueParser.toDisplayValue(AttributeValue.ofIntArray(new int[]{1, 2})));

        Map<String, String> map = new LinkedHashMap<>();
        map.put("k1", "v1");
        map.put("k2", "v2");
        String mapStr = SpectralLibraryAttributeValueParser.toDisplayValue(AttributeValue.ofStringMap(map));
        assertTrue(mapStr.equals("k1=v1,k2=v2") || mapStr.equals("k2=v2,k1=v1"));

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2, 3}, "nm");
        SpectralSignature sig = SpectralSignature.of(new double[]{0.1, 0.2, 0.3});
        AttributeValue.EmbeddedSpectrum emb = new AttributeValue.EmbeddedSpectrum(axis, sig);
        assertEquals("<spectrum n=3>", SpectralLibraryAttributeValueParser.toDisplayValue(AttributeValue.ofEmbeddedSpectrum(emb)));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_toDisplayValue_joinArrays_emptyReturnsEmpty() {
        assertEquals("", SpectralLibraryAttributeValueParser.toDisplayValue(AttributeValue.ofDoubleArray(new double[]{})));
        assertEquals("", SpectralLibraryAttributeValueParser.toDisplayValue(AttributeValue.ofIntArray(new int[]{})));
    }
}