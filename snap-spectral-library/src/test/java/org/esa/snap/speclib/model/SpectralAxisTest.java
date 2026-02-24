package org.esa.snap.speclib.model;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.*;


public class SpectralAxisTest {


    @Test
    @STTM("SNAP-4128")
    public void test_ctor_rejectsNullsAndEmpty() {
        assertThrows(NullPointerException.class, () -> new SpectralAxis(null, "nm"));
        assertThrows(NullPointerException.class, () -> new SpectralAxis(new double[]{1}, null));
        assertThrows(IllegalArgumentException.class, () -> new SpectralAxis(new double[]{}, "nm"));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_wavelengthsAreDefensivelyCopied() {
        double[] wl = new double[]{400, 500};
        SpectralAxis axis = new SpectralAxis(wl, "nm");
        wl[0] = 999;
        assertEquals(400, axis.getWavelengths()[0], 0.001);

        double[] fromGetter = axis.getWavelengths();
        fromGetter[0] = 888;
        assertEquals(400, axis.getWavelengths()[0], 0.001);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_sizeAndUnit() {
        SpectralAxis axis = new SpectralAxis(new double[]{1, 2, 3}, "nm");
        assertEquals(3, axis.size());
        assertEquals("nm", axis.getXUnit());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_equalsAndHashCode_useUnitAndWavelengthsContent() {
        SpectralAxis a = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralAxis b = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralAxis c = new SpectralAxis(new double[]{1, 3}, "nm");
        SpectralAxis d = new SpectralAxis(new double[]{1, 2}, "um");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertNotEquals(null, a);
        assertNotEquals("axis", a);
    }
}