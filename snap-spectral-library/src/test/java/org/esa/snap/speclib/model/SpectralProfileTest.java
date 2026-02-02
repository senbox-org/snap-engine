package org.esa.snap.speclib.model;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;


public class SpectralProfileTest {


    @Test
    @STTM("SNAP-4128")
    public void test_createsValidProfile() {
        UUID id = UUID.randomUUID();
        double[] wl = {500.0, 600.0, 700.0};
        double[] v = {0.1, 0.2, 0.3};

        SpectralProfile p = new SpectralProfile(id, "p1", wl, v, "reflectance");

        assertEquals(id, p.getId());
        assertEquals("p1", p.getName());
        assertEquals("reflectance", p.getUnit());
        assertEquals(3, p.size());
        assertArrayEquals(wl, p.getWavelengths(), 0.0);
        assertArrayEquals(v, p.getValues(), 0.0);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_rejectsNullArguments() {
        UUID id = UUID.randomUUID();
        double[] wl = {500.0};
        double[] v = {0.1};

        assertThrows(NullPointerException.class, () -> new SpectralProfile(null, "name", wl, v, "unit"));
        assertThrows(NullPointerException.class, () -> new SpectralProfile(id, null, wl, v, "unit"));
        assertThrows(NullPointerException.class, () -> new SpectralProfile(id, "name", null, v, "unit"));
        assertThrows(NullPointerException.class, () -> new SpectralProfile(id, "name", wl, null, "unit"));
        assertThrows(NullPointerException.class, () -> new SpectralProfile(id, "name", wl, v, null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_rejectsLengthMismatch() {
        UUID id = UUID.randomUUID();
        double[] wl = {500.0, 600.0};
        double[] v = {0.1};

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new SpectralProfile(id, "name", wl, v, "unit"));
        assertTrue(ex.getMessage().toLowerCase().contains("same length"));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_rejectsEmptyArrays() {
        UUID id = UUID.randomUUID();
        double[] wl = {};
        double[] v = {};

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new SpectralProfile(id, "name", wl, v, "unit"));
        assertTrue(ex.getMessage().toLowerCase().contains("must not be empty"));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_isDefensivelyCopiedOnConstruction() {
        UUID id = UUID.randomUUID();
        double[] wl = {500.0, 600.0};
        double[] v = {0.1, 0.2};

        SpectralProfile p = new SpectralProfile(id, "name", wl, v, "unit");

        wl[0] = 999.0;
        v[0] = 9.9;

        assertArrayEquals(new double[]{500.0, 600.0}, p.getWavelengths(), 0.0);
        assertArrayEquals(new double[]{0.1, 0.2}, p.getValues(), 0.0);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_gettersReturnDefensiveCopies() {
        UUID id = UUID.randomUUID();
        SpectralProfile p = new SpectralProfile(id, "name", new double[]{500.0, 600.0}, new double[]{0.1, 0.2}, "unit");

        double[] wlCopy = p.getWavelengths();
        double[] vCopy = p.getValues();

        wlCopy[0] = 999.0;
        vCopy[0] = 9.9;

        assertArrayEquals(new double[]{500.0, 600.0}, p.getWavelengths(), 0.0);
        assertArrayEquals(new double[]{0.1, 0.2}, p.getValues(), 0.0);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_createSpectralProfile() {
        SpectralProfile sp1 = SpectralProfile.create("name", new double[]{500.0}, new double[]{0.1}, "unit");
        SpectralProfile sp2 = SpectralProfile.create("name", new double[]{500.0}, new double[]{0.1}, "unit");

        assertNotNull(sp1.getId());
        assertNotNull(sp2.getId());
        assertNotEquals(sp1.getId(), sp2.getId());

        assertEquals("name", sp1.getName());
        assertEquals("unit", sp1.getUnit());
        assertArrayEquals(new double[]{500.0}, sp1.getWavelengths(), 0.0);
        assertArrayEquals(new double[]{0.1}, sp1.getValues(), 0.0);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_equalsAndHashCodeAreBasedOnId() {
        UUID id = UUID.randomUUID();

        SpectralProfile a = new SpectralProfile(id, "name_1", new double[]{500.0}, new double[]{0.1}, "unit");
        SpectralProfile b = new SpectralProfile(id, "name_2", new double[]{600.0}, new double[]{0.2}, "unit2");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}