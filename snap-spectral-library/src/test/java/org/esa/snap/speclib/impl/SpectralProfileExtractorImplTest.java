package org.esa.snap.speclib.impl;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.speclib.api.SpectralSampleProvider;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralProfile;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.*;


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
}