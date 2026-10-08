package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.*;

public class UrlConverterTest {

    private UrlConverter converter;

    @Before
    public void setUp() {
        converter = new UrlConverter();
    }

    @Test
    public void testGetValueType() {
        assertEquals(URL.class, converter.getValueType());
    }

    @Test
    public void testParse() throws ConversionException {
        assertNull(converter.parse(""));

        final URL url = converter.parse("http://www.brockmann-consult.de");
        assertEquals("http://www.brockmann-consult.de", url.toString());
    }

    @Test
    public void testParse_illegalInput() {
        try {
            converter.parse("scp:$retojh____");
            fail("ConversionException expected");
        } catch (ConversionException expected) {
        }
    }

    @Test
    public void testFormat() throws URISyntaxException, MalformedURLException {
        URI uri = new URI("ftp://localhost:8080");
        assertEquals("ftp://localhost:8080", converter.format(uri.toURL()));
    }

    @Test
    public void testFormat_invalidInput() {
        assertEquals("", converter.format(null));
    }
}
