package org.esa.snap.speclib.io.csv.util;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.model.AttributeType;
import org.junit.Test;

import static org.junit.Assert.*;


public class CsvAttributeTypeInferenceTest {


    @Test
    @STTM("SNAP-4129")
    public void test_inferType_null_returnsString() {
        assertEquals(AttributeType.STRING, CsvAttributeTypeInference.inferType(null));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_inferType_blank_returnsString() {
        assertEquals(AttributeType.STRING, CsvAttributeTypeInference.inferType("   "));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_inferType_bracketsEmptyInner_returnsString() {
        assertEquals(AttributeType.STRING, CsvAttributeTypeInference.inferType("[]"));
        assertEquals(AttributeType.STRING, CsvAttributeTypeInference.inferType("[   ]"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_inferType_bracketsOnlyCommas_returnsString_nNonEmptyZero() {
        assertEquals(AttributeType.STRING, CsvAttributeTypeInference.inferType("[,, ,   ,]"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_inferType_intArray_allInt_true() {
        assertEquals(AttributeType.INT_ARRAY, CsvAttributeTypeInference.inferType("[1,2,3]"));
        assertEquals(AttributeType.INT_ARRAY, CsvAttributeTypeInference.inferType("[ -1 , +2 , 0 ]"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_inferType_doubleArray_allDouble_true_includingExponent() {
        assertEquals(AttributeType.DOUBLE_ARRAY, CsvAttributeTypeInference.inferType("[1.0,2,3.5]"));
        assertEquals(AttributeType.DOUBLE_ARRAY, CsvAttributeTypeInference.inferType("[1e3, -2E-2, 3]"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_inferType_stringList_notAllNumeric() {
        assertEquals(AttributeType.STRING_LIST, CsvAttributeTypeInference.inferType("[a,b,c]"));
        assertEquals(AttributeType.STRING_LIST, CsvAttributeTypeInference.inferType("[1, x, 2]"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_inferType_stringMap_detectedByEquals() {
        assertEquals(AttributeType.STRING_MAP, CsvAttributeTypeInference.inferType("a=b"));
        assertEquals(AttributeType.STRING_MAP, CsvAttributeTypeInference.inferType("k=v, x=y"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_inferType_boolean_variants() {
        assertEquals(AttributeType.BOOLEAN, CsvAttributeTypeInference.inferType("true"));
        assertEquals(AttributeType.BOOLEAN, CsvAttributeTypeInference.inferType("FALSE"));
        assertEquals(AttributeType.BOOLEAN, CsvAttributeTypeInference.inferType(" yes "));
        assertEquals(AttributeType.BOOLEAN, CsvAttributeTypeInference.inferType("No"));
        assertEquals(AttributeType.BOOLEAN, CsvAttributeTypeInference.inferType("0"));
        assertEquals(AttributeType.BOOLEAN, CsvAttributeTypeInference.inferType("1"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_inferType_intWithinRange_returnsInt() {
        assertEquals(AttributeType.INT, CsvAttributeTypeInference.inferType("2147483647"));
        assertEquals(AttributeType.INT, CsvAttributeTypeInference.inferType("-2147483648"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_inferType_intOutOfRange_returnsLong() {
        assertEquals(AttributeType.LONG, CsvAttributeTypeInference.inferType("2147483648"));
        assertEquals(AttributeType.LONG, CsvAttributeTypeInference.inferType("-2147483649"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_inferType_intPatternButParseFails_returnsString() {
        assertEquals(AttributeType.STRING, CsvAttributeTypeInference.inferType("999999999999999999999999999999999999999"));
        assertEquals(AttributeType.STRING, CsvAttributeTypeInference.inferType("-999999999999999999999999999999999999999"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_inferType_double_returnsDouble() {
        assertEquals(AttributeType.DOUBLE, CsvAttributeTypeInference.inferType("1.23"));
        assertEquals(AttributeType.DOUBLE, CsvAttributeTypeInference.inferType(".5"));
        assertEquals(AttributeType.DOUBLE, CsvAttributeTypeInference.inferType("5."));
        assertEquals(AttributeType.DOUBLE, CsvAttributeTypeInference.inferType("1e3"));
        assertEquals(AttributeType.DOUBLE, CsvAttributeTypeInference.inferType("-2E-2"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_inferType_default_returnsString() {
        assertEquals(AttributeType.STRING, CsvAttributeTypeInference.inferType("abc"));
        assertEquals(AttributeType.STRING, CsvAttributeTypeInference.inferType("1,2,3"));
    }
}