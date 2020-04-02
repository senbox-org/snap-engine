package org.esa.snap.dataio.geotiff;

import it.geosolutions.imageio.plugins.tiff.TIFFField;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TiffFileInfoTest {

    @Test
    public void testGetDoubleValues_zero_elements() {
        final TIFFField field = mock(TIFFField.class);
        when(field.getCount()).thenReturn(0);

        final double[] values = TiffFileInfo.getDoubleValues(field);
        assertEquals(0, values.length);

        verify(field, times(1)).getCount();
        verifyNoMoreInteractions(field);
    }

    @Test
    public void testGetDoubleValues_three_elements() {
        final TIFFField field = mock(TIFFField.class);
        when(field.getCount()).thenReturn(3);
        when(field.getAsDouble(anyInt())).thenReturn(2.8, 3.2, 4.6);

        final double[] values = TiffFileInfo.getDoubleValues(field);
        assertEquals(3, values.length);
        assertEquals(2.8, values[0], 1e-8);
        assertEquals(3.2, values[1], 1e-8);
        assertEquals(4.6, values[2], 1e-8);

        verify(field, times(1)).getCount();
        verify(field, times(3)).getAsDouble(anyInt());
        verifyNoMoreInteractions(field);
    }

    @Test
    public void testGetStringValues_zero_elements() {
        final TIFFField field = mock(TIFFField.class);
        when(field.getCount()).thenReturn(0);

        final String[] values = TiffFileInfo.getStringValues(field);
        assertEquals(0, values.length);

        verify(field, times(1)).getCount();
        verifyNoMoreInteractions(field);
    }

    @Test
    public void testGetStringValues_three_elements() {
        final TIFFField field = mock(TIFFField.class);
        when(field.getCount()).thenReturn(3);
        when(field.getAsString(anyInt())).thenReturn("alpha", "beta", "lametta");

        final String[] values = TiffFileInfo.getStringValues(field);
        assertEquals(3, values.length);
        assertEquals("alpha", values[0]);
        assertEquals("beta", values[1]);
        assertEquals("lametta", values[2]);

        verify(field, times(1)).getCount();
        verify(field, times(3)).getAsString(anyInt());
        verifyNoMoreInteractions(field);
    }
}
