package org.esa.snap.speclib.impl;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.speclib.api.SpectralSampleProvider;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralProfile;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


public class SpectralProfileExtractorImplTest {


    @Test
    @STTM("SNAP-4128")
    public void test_ctorRejectsNull() {
        try {
            new SpectralProfileExtractorImpl(null);
            fail("expected NullPointerException");
        } catch (NullPointerException ignored) {}
    }

    @Test
    @STTM("SNAP-4128")
    public void test_extractRejectsNulls() {
        SpectralSampleProvider sp = Mockito.mock(SpectralSampleProvider.class);
        SpectralProfileExtractorImpl ex = new SpectralProfileExtractorImpl(sp);
        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        List<Band> bands = List.of(Mockito.mock(Band.class), Mockito.mock(Band.class));

        assertThrows(NullPointerException.class, () -> ex.extract(null, axis, bands, 0, 0, 0, null, null));
        assertThrows(NullPointerException.class, () -> ex.extract("n", null, bands, 0, 0, 0, null, null));
        assertThrows(NullPointerException.class, () -> ex.extract("n", axis, null, 0, 0, 0, null, null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_extractBandsSizeMismatchThrows() {
        SpectralSampleProvider sp = Mockito.mock(SpectralSampleProvider.class);
        SpectralProfileExtractorImpl ex = new SpectralProfileExtractorImpl(sp);

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2, 3}, "nm");
        List<Band> bands = List.of(Mockito.mock(Band.class));

        assertThrows(IllegalArgumentException.class, () -> ex.extract("n", axis, bands, 1, 2, 0, null, null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_extractBuildsProfileWithSourceRef_andYUnitOptional() {
        SpectralSampleProvider sp = Mockito.mock(SpectralSampleProvider.class);
        SpectralProfileExtractorImpl ex = new SpectralProfileExtractorImpl(sp);

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        List<Band> bands = List.of(Mockito.mock(Band.class), Mockito.mock(Band.class));
        Mockito.when(sp.readSamples(bands, 10, 20, 3)).thenReturn(new double[]{0.1, 0.2});

        SpectralProfile p1 = ex.extract("p", axis, bands, 10, 20, 3, null, "prod").orElseThrow();
        assertEquals("p", p1.getName());
        assertEquals(2, p1.size());
        assertNull(p1.getSignature().getYUnitOrNull());
        assertTrue(p1.getSourceRef().isPresent());
        assertEquals(20, p1.getSourceRef().orElseThrow().getY());
        assertEquals(3, p1.getSourceRef().orElseThrow().getLevel());
        assertEquals("prod", p1.getSourceRef().orElseThrow().getProductId().orElseThrow());

        SpectralProfile p2 = ex.extract("p2", axis, bands, 10, 20, 3, "reflectance", null).orElseThrow();
        assertEquals("reflectance", p2.getSignature().getYUnitOrNull());
        assertTrue(p2.getSourceRef().isPresent());
        assertTrue(p2.getSourceRef().orElseThrow().getProductId().isEmpty());
    }


    @Test
    @STTM("SNAP-4128")
    public void extractBulk_skipsNullPixels_andUsesIndexInName() {
        SpectralSampleProvider sp = mock(SpectralSampleProvider.class);
        SpectralProfileExtractorImpl ex = new SpectralProfileExtractorImpl(sp);

        Band b1 = mock(Band.class);
        Band b2 = mock(Band.class);
        List<Band> bands = List.of(b1, b2);

        SpectralAxis axis = mock(SpectralAxis.class);
        when(axis.size()).thenReturn(2);

        List<PixelPos> pixels = List.of(new PixelPos(1, 1), new PixelPos(2, 2));
        double[][] samples = new double[][]{
                new double[]{1.0, 2.0},
                new double[]{3.0, 4.0}
        };
        when(sp.readSamples(eq(bands), any(int[].class), any(int[].class), eq(0))).thenReturn(samples);

        List<SpectralProfile> out = ex.extractBulk("base", axis, bands, pixels, 0, "Y", "prod");

        assertEquals(2, out.size());
        assertEquals("base1", out.get(0).getName());
        assertEquals("base2", out.get(1).getName());

        assertArrayEquals(new double[]{1.0, 2.0}, out.get(0).getSignature().getValues(), 0.0);
        assertArrayEquals(new double[]{3.0, 4.0}, out.get(1).getSignature().getValues(), 0.0);

        assertTrue(out.get(0).getSourceRef().isPresent());
        assertEquals(1, out.get(0).getSourceRef().get().getX());
        assertEquals(1, out.get(0).getSourceRef().get().getY());
        assertEquals("prod", out.getFirst().getSourceRef().get().getProductId().get());

        assertEquals(Map.of(), out.getFirst().getAttributes());
    }

    @Test
    @STTM("SNAP-4128")
    public void extractBulk_skipsAllNaNSignatures_andSanitizesInfinite() {
        SpectralSampleProvider sp = mock(SpectralSampleProvider.class);
        SpectralProfileExtractorImpl ex = new SpectralProfileExtractorImpl(sp);

        List<Band> bands = List.of(mock(Band.class));
        SpectralAxis axis = mock(SpectralAxis.class);
        when(axis.size()).thenReturn(1);

        List<PixelPos> pixels = List.of(new PixelPos(1, 1), new PixelPos(2, 2));

        double[][] samples = new double[][]{new double[]{Double.POSITIVE_INFINITY}, new double[]{Double.NaN}};
        when(sp.readSamples(eq(bands), any(int[].class), any(int[].class), eq(0))).thenReturn(samples);

        List<SpectralProfile> out = ex.extractBulk("base", axis, bands, pixels, 0, null, "prod");
        assertTrue(out.isEmpty());
    }
}