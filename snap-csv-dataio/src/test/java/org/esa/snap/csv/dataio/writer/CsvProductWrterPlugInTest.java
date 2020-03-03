package org.esa.snap.csv.dataio.writer;

import org.esa.snap.core.dataio.EncodeQualification;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CsvProductWrterPlugInTest {

    private CsvProductWriterPlugIn plugIn;

    @Before
    public void setUp() {
        plugIn = new CsvProductWriterPlugIn(null, 0);
    }

    @Test
    public void testGetEncodeQualification() {
        final Product product = mock(Product.class);

        when(product.isMultiSize()).thenReturn(true);
        EncodeQualification encodeQualification = plugIn.getEncodeQualification(product);
        assertEquals(EncodeQualification.Preservation.UNABLE, encodeQualification.getPreservation());

        when(product.isMultiSize()).thenReturn(false);
        encodeQualification = plugIn.getEncodeQualification(product);
        assertEquals(EncodeQualification.Preservation.PARTIAL, encodeQualification.getPreservation());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testGetOutputTypes() {
        final Class[] outputTypes = plugIn.getOutputTypes();

        assertEquals(2, outputTypes.length);
        assertEquals(String.class, outputTypes[0]);
        assertEquals(File.class, outputTypes[1]);
    }

    // @todo 2 tb/tb make this test run again
//    @Test
//    public void testCreateWriterInstance() {
//        final CsvProductWriterPlugIn writerPlugIn = new CsvProductWriterPlugIn(new File("."), 0);
//        final ProductWriter writer = writerPlugIn.createWriterInstance();
//        assertTrue(writer instanceof CsvProductWriter);
//    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();
        assertEquals(1, formatNames.length);
        assertEquals("CSV", formatNames[0]);
    }

    @Test
    public void testGetDefaultExtensions() {
        final String[] extensions = plugIn.getDefaultFileExtensions();
        assertEquals(1, extensions.length);
        assertEquals(".csv", extensions[0]);
    }

    @Test
    public void testGetDescription() {
        final String description = plugIn.getDescription(null);
        assertEquals("CSV products", description);
    }

    @Test
    public void testGetProductFileFilter() {
        final SnapFileFilter productFileFilter = plugIn.getProductFileFilter();
        assertEquals("CSV", productFileFilter.getFormatName());
        assertEquals("CSV products (*.csv)", productFileFilter.getDescription());
        assertEquals(".csv", productFileFilter.getExtensions()[0]);
    }
}
