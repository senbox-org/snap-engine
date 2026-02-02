package org.esa.snap.speclib.impl;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.speclib.api.SpectralProfileExtractor;
import org.esa.snap.speclib.api.SpectralSampleProvider;
import org.esa.snap.speclib.model.SpectralProfile;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class SpectralProfileExtractorImplTest {


    private static Band band(float wavelength) {
        Band b = mock(Band.class);
        when(b.getSpectralWavelength()).thenReturn(wavelength);
        return b;
    }

    @Test
    @STTM("SNAP-4128")
    public void test_extractsProfileForValidSamples() {
        Band b1 = band(500f);
        Band b2 = band(600f);

        SpectralSampleProvider p = mock(SpectralSampleProvider.class);
        when(p.isPixelValid(any(), anyInt(), anyInt(), anyInt())).thenReturn(true);
        when(p.noDataValue(any())).thenReturn(-9999d);
        when(p.readSample(b1, 1, 2, 0)).thenReturn(0.1d);
        when(p.readSample(b2, 1, 2, 0)).thenReturn(0.2d);

        SpectralProfileExtractor ex = new SpectralProfileExtractorImpl(p);

        Optional<SpectralProfile> out = ex.extract("p", List.of(b1, b2), 1, 2, 0, "unit");
        assertTrue(out.isPresent());
        assertArrayEquals(new double[]{500.0, 600.0}, out.get().getWavelengths(), 0.0);
        assertArrayEquals(new double[]{0.1, 0.2}, out.get().getValues(), 0.0);
        assertEquals("p", out.get().getName());
        assertEquals("unit", out.get().getUnit());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_filtersNoDataAndInvalid() {
        Band b1 = band(500f);
        Band b2 = band(600f);

        SpectralSampleProvider p = mock(SpectralSampleProvider.class);
        when(p.noDataValue(any())).thenReturn(-9999d);
        when(p.isPixelValid(b1, 0, 0, 0)).thenReturn(true);
        when(p.isPixelValid(b2, 0, 0, 0)).thenReturn(false);
        when(p.readSample(b1, 0, 0, 0)).thenReturn(-9999d);

        SpectralProfileExtractor ex = new SpectralProfileExtractorImpl(p);

        Optional<SpectralProfile> out = ex.extract("p", List.of(b1, b2), 0, 0, 0, "unit");
        assertTrue(out.isEmpty());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_ignoresNonSpectralBands() {
        Band nonSpectral = band(0f);
        Band spectral = band(700f);

        SpectralSampleProvider p = mock(SpectralSampleProvider.class);
        when(p.isPixelValid(any(), anyInt(), anyInt(), anyInt())).thenReturn(true);
        when(p.noDataValue(any())).thenReturn(-9999d);
        when(p.readSample(spectral, 1, 1, 0)).thenReturn(1.23d);

        SpectralProfileExtractor ex = new SpectralProfileExtractorImpl(p);

        Optional<SpectralProfile> out = ex.extract("p", List.of(nonSpectral, spectral), 1, 1, 0, "unit");
        assertTrue(out.isPresent());
        assertArrayEquals(new double[]{700.0}, out.get().getWavelengths(), 0.0);
        assertArrayEquals(new double[]{1.23}, out.get().getValues(), 0.0);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_rejectsNulls() {
        SpectralSampleProvider p = mock(SpectralSampleProvider.class);
        SpectralProfileExtractor ex = new SpectralProfileExtractorImpl(p);

        assertThrows(NullPointerException.class, () -> ex.extract(null, List.of(), 0, 0, 0, "u"));
        assertThrows(NullPointerException.class, () -> ex.extract("n", null, 0, 0, 0, "u"));
        assertThrows(NullPointerException.class, () -> ex.extract("n", List.of(), 0, 0, 0, null));
    }
}