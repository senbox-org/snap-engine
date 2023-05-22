package com.bc.ceres.swing;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BrightBlueImageFilterTest {

    @Test
    public void testFiltering() {
        int expected;
        int actual;
        BrightBlueImageFilter imageFilter = new BrightBlueImageFilter();

        expected = 0xFF1515FF;
        actual = imageFilter.filterRGB(0, 0, 0xFF151515);
        assertEquals(String.format("Expected: 0x%x, actual 0x%x", expected, actual), expected, actual);

        expected = 0xF04747FF;
        actual = imageFilter.filterRGB(0, 0, 0xF0A7042C);
        assertEquals(String.format("Expected: 0x%x, actual 0x%x", expected, actual), expected, actual);
    }
}