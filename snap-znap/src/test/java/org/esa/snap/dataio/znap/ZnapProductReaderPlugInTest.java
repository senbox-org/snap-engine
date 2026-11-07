package org.esa.snap.dataio.znap;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ZnapProductReaderPlugInTest {

    private ZnapProductReaderPlugIn plugIn;

    @Before
    public void setUp() {
        plugIn = new ZnapProductReaderPlugIn();
    }

    @Test
    public void testGetInputTypes() {
        final Class<?>[] inputTypes = plugIn.getInputTypes();
        assertEquals(3, inputTypes.length);
        assertEquals(Path.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
        assertEquals(String.class, inputTypes[2]);
    }

    @Test
    public void testCreateReaderInstance() {
        final ProductReader readerInstance = plugIn.createReaderInstance();
        assertTrue(readerInstance instanceof ZnapProductReader);
    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();

        assertEquals(1, formatNames.length);
        assertEquals("ZNAP", formatNames[0]);
    }

    @Test
    public void testGetDefaultFileExtensions() {
        final String[] defaultFileExtensions = plugIn.getDefaultFileExtensions();

        assertEquals(2, defaultFileExtensions.length);
        assertEquals(".znap", defaultFileExtensions[0]);
        assertEquals(".znap.zip", defaultFileExtensions[1]);
    }

    @Test
    public void testGetDescription() {
        final String description = plugIn.getDescription(null);// don't require Locale here tb 2026-11-03

        assertEquals("ZNAP product reader", description);
    }

    @Test
    public void testGetProductFileFilter() {
        final SnapFileFilter productFileFilter = plugIn.getProductFileFilter();

        assertNotNull(productFileFilter);

        assertFalse(productFileFilter.accept(null));

        final File file = mock(File.class);

        // check for directory
        when(file.isDirectory()).thenReturn(true);
        assertTrue(productFileFilter.accept(file));

        when(file.isDirectory()).thenReturn(false);
        assertFalse(productFileFilter.accept(file));

        // check for file - zip
        when(file.isFile()).thenReturn(true);
        when(file.getName()).thenReturn("testomat.znap.zip");
        assertTrue(productFileFilter.accept(file));
        when(file.getName()).thenReturn("testomat.other_format");
        assertFalse(productFileFilter.accept(file));

        // check for file - in ZNAP directory
        final File directory = mock(File.class);
        when(directory.isDirectory()).thenReturn(true);
        when(directory.getName()).thenReturn("testomat.znap");
        when(file.getName()).thenReturn("testomat.not_zipped");
        when(file.getParentFile()).thenReturn(directory);
        assertTrue(productFileFilter.accept(file));

        when(directory.getName()).thenReturn("testomat.not_znap_but_other_format");
        assertFalse(productFileFilter.accept(file));

        // compound document
        when(directory.isDirectory()).thenReturn(true);
        when(directory.getName()).thenReturn("testomat.znap");
        when(file.getName()).thenReturn("testomat.not_zipped");
        when(file.getParentFile()).thenReturn(directory);
        assertTrue(productFileFilter.isCompoundDocument(file));

        when(directory.getName()).thenReturn("testomat.not_znap_but_other_format");
        assertFalse(productFileFilter.isCompoundDocument(file));

        assertFalse(productFileFilter.isCompoundDocument(null));
    }
}
