package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValueRange;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LongConverterTest {

    private LongConverter converter;

    @Before
    public void setUp() {
        converter = new LongConverter();
    }

    @Test
    public void testGetValueType() {
        assertEquals(Long.class, converter.getValueType());
    }

    @Test
    public void testParse() throws ConversionException {
        assertNull(converter.parse(""));

        assertEquals(1L, (long) converter.parse("1"));
        assertEquals(1123467891L, (long) converter.parse("1123467891"));
    }

    @Test
    public void testFormat() {
        assertEquals("22", converter.format(22L));
    }

    @Test
    public void testFormat_invalidInput() {
        assertEquals("", converter.format(null));
    }
}
