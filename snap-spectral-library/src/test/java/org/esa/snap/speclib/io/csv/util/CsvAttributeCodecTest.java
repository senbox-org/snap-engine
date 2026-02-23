package org.esa.snap.speclib.io.csv.util;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.model.AttributeType;
import org.esa.snap.speclib.model.AttributeValue;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;


public class CsvAttributeCodecTest {


    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4129")
    public void test_tryParse_nullType_throwsNpe() {
        CsvAttributeCodec.tryParse(null, "x");
    }

    @Test
    @STTM("SNAP-4129")
    public void test_tryParse_nullRaw_returnsNull() {
        assertNull(CsvAttributeCodec.tryParse(AttributeType.STRING, null));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_tryParse_blankRaw_returnsNull() {
        assertNull(CsvAttributeCodec.tryParse(AttributeType.STRING, "   "));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_tryParse_stringList_bracketed_isUnwrappedAndParsed() {
        AttributeValue v = CsvAttributeCodec.tryParse(AttributeType.STRING_LIST, "[a, b, c]");

        assertNotNull(v);
        assertEquals(AttributeType.STRING_LIST, v.getType());
        assertEquals(Arrays.asList("a", "b", "c"), v.asStringList());
    }

    @Test
    @STTM("SNAP-4129")
    public void test_tryParse_validParses_viaParser() {
        AttributeValue v1 = CsvAttributeCodec.tryParse(AttributeType.INT, "42");
        assertNotNull(v1);
        assertEquals(AttributeType.INT, v1.getType());
        assertEquals(42, v1.asInt());

        AttributeValue v2 = CsvAttributeCodec.tryParse(AttributeType.DOUBLE, "3.5");
        assertNotNull(v2);
        assertEquals(AttributeType.DOUBLE, v2.getType());
        assertEquals(3.5, v2.asDouble(), 0.0);

        AttributeValue v3 = CsvAttributeCodec.tryParse(AttributeType.BOOLEAN, "true");
        assertNotNull(v3);
        assertEquals(AttributeType.BOOLEAN, v3.getType());
        assertTrue(v3.asBoolean());
    }

    @Test
    @STTM("SNAP-4129")
    public void test_tryParse_invalidBoolean_fallsBackToYesNoMapping_true() {
        assertNull(CsvAttributeCodec.tryParse(AttributeType.BOOLEAN, "maybe"));
        assertNull(CsvAttributeCodec.tryParse(AttributeType.BOOLEAN, "definitely-not"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_tryParse_invalidInt_returnsNull() {
        assertNull(CsvAttributeCodec.tryParse(AttributeType.INT, "abc"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_format_null_returnsEmptyString() {
        assertEquals("", CsvAttributeCodec.format(null));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_format_doubleArray_isBracketed() {
        AttributeValue v = AttributeValue.ofDoubleArray(new double[]{1.0, 2.5});
        String s = CsvAttributeCodec.format(v);

        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
        assertTrue(s.contains("1.0"));
        assertTrue(s.contains("2.5"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_format_intArray_isBracketed() {
        AttributeValue v = AttributeValue.ofIntArray(new int[]{1, 2});
        String s = CsvAttributeCodec.format(v);

        assertEquals("[1,2]", s);
    }

    @Test
    @STTM("SNAP-4129")
    public void test_format_stringList_isBracketed() {
        AttributeValue v = AttributeValue.ofStringList(Arrays.asList("a", "b"));
        String s = CsvAttributeCodec.format(v);

        assertEquals("[a,b]", s);
    }

    @Test
    @STTM("SNAP-4129")
    public void test_format_defaultUsesDisplayValue_unquoted() {
        AttributeValue v = AttributeValue.ofString("hello");

        assertEquals("hello", CsvAttributeCodec.format(v));
    }
}