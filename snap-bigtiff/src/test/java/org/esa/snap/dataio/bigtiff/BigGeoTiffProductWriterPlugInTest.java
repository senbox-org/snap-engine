package org.esa.snap.dataio.bigtiff;

import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BigGeoTiffProductWriterPlugInTest {

    private BigGeoTiffProductWriterPlugIn plugIn;

    @Before
    public void setUp() {
        plugIn = new BigGeoTiffProductWriterPlugIn();
    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();
        assertArrayEquals(new String[]{BigGeoTiffProductWriterPlugIn.FORMAT_NAME}, formatNames);
    }

    @Test
    public void testGetDefaultFileExtensions() {
        final String[] defaultFileExtensions = plugIn.getDefaultFileExtensions();
        assertArrayEquals(new String[]{".tif", ".tiff"}, defaultFileExtensions);
    }

    @Test
    public void testGetOutputTypes() {
        final Class[] outputTypes = plugIn.getOutputTypes();
        assertArrayEquals(new Class[]{String.class, File.class,}, outputTypes);
    }

    @Test
    public void testGetDescription() {
        assertNotNull(plugIn.getDescription(null));
    }

    @Test
    public void testProductFileFilter() {
        final SnapFileFilter snapFileFilter = plugIn.getProductFileFilter();

        assertNotNull(snapFileFilter);
        assertArrayEquals(plugIn.getDefaultFileExtensions(), snapFileFilter.getExtensions());
        assertEquals(plugIn.getFormatNames()[0], snapFileFilter.getFormatName());
        assertTrue(snapFileFilter.getDescription().contains(plugIn.getDescription(Locale.getDefault())));
    }

    @Test
    public void testCreateWriterInstance() {
        final ProductWriter writer = plugIn.createWriterInstance();

        assertNotNull(writer);
        assertTrue(writer instanceof BigGeoTiffProductWriter);
    }

}
