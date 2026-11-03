package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class PatternConverterTest {

    private PatternConverter converter;

    @Before
    public void setUp() {
        converter = new PatternConverter();
    }

    @Test
    public void testGetValueType() {
        assertEquals(Pattern.class, converter.getValueType());
    }

    @Test
    public void testParse() throws ConversionException {
        assertNull(converter.parse(""));

        final Pattern pattern = converter.parse("[ab|AB]");
        assertEquals("[ab|AB]", pattern.toString());
    }

    @Test
    public void testParse_invalidInput() {
        try {
            converter.parse("[");   // open without close is invalid tb 2026-10-08
            fail("ConversionException expected");
        } catch (ConversionException expected) {
        }
    }

    @Test
    public void testFormat() {
        final Pattern pattern = Pattern.compile(".*1276?");

        assertEquals(".*1276?", converter.format(pattern));
    }

    @Test
    public void testFormat_invalidInput() {
        assertEquals("", converter.format(null));
    }
}
