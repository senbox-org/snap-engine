package eu.esa.snap.dataio.cached;

import org.esa.snap.core.util.io.SnapFileFilter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class PaceOCICachedProductReaderPluginTest {

    private PaceOCICachedProductReaderPlugin plugin;

    @Before
    public void setUp() {
        plugin = new PaceOCICachedProductReaderPlugin();
    }

    @Test
    public void testGetInputTypes() {
        final Class[] inputTypes = plugin.getInputTypes();

        assertEquals(2, inputTypes.length);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    @Test
    public void testGetProductFileFilter() {
        final SnapFileFilter productFileFilter = plugin.getProductFileFilter();

        assertEquals("PaceOCI_L1B", productFileFilter.getFormatName());
        assertEquals(".nc", productFileFilter.getDefaultExtension());
        assertEquals("PACE OCI L1B Products caching reader (*.nc)", productFileFilter.getDescription());
    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = plugin.getFormatNames();

        assertEquals(1, formatNames.length);
        assertEquals("PaceOCI_L1B", formatNames[0]);
    }

    @Test
    public void testGetDefaultFileExtensions() {
        final String[] extensions = plugin.getDefaultFileExtensions();

        assertEquals(1, extensions.length);
        assertEquals(".nc", extensions[0]);
    }

    @Test
    public void testGetDescription() {
        final String description = plugin.getDescription(null);// no locale requires here tb 2024-05-15

        assertEquals("PACE OCI L1B Products caching reader", description);
    }
}
