package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValueRange;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class IntervalConverterTest {

    private IntervalConverter converter;

    @Before
    public void setUp() {
        converter = new IntervalConverter();
    }

    @Test
    public void testGetValueType() {
        assertEquals(ValueRange.class, converter.getValueType());
    }

    @Test
    public void testParse() throws ConversionException {
        assertNull(converter.parse(""));

        ValueRange range = converter.parse("[-2,5]");
        assertEquals(-2, range.getMin(), 1e-8);
        assertEquals(5, range.getMax(), 1e-8);

        range = converter.parse("(108,111)");
        assertEquals(108, range.getMin(), 1e-8);
        assertEquals(111, range.getMax(), 1e-8);
    }

    @Test
    public void testParse_invalidInput() {
        try {
            converter.parse("Start=12,stop=14");
            fail("ConversionException expected");
        } catch (ConversionException expected) {
        }
    }

    @Test
    public void testFormat() {
        final ValueRange valueRange = new ValueRange(0.76, 1.54);
        assertEquals("[0.76,1.54]", converter.format(valueRange));
    }

    @Test
    public void testFormat_invalidInput() {
        assertEquals("", converter.format(null));
    }
}
