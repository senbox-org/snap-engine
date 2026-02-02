package org.esa.snap.speclib.model;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;


public class SpectralLibraryTest {


    private static SpectralProfile profile(UUID id, String name) {
        return new SpectralProfile(id, name, new double[]{500.0}, new double[]{0.1}, "unit");
    }


    @Test
    @STTM("SNAP-4128")
    public void test_createGeneratesIdAndDefaults() {
        SpectralLibrary lib = SpectralLibrary.create("lib");
        assertNotNull(lib.getId());
        assertEquals("lib", lib.getName());
        assertEquals(0, lib.size());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_rejectsNulls() {
        UUID id = UUID.randomUUID();
        assertThrows(NullPointerException.class, () -> new SpectralLibrary(null, "n"));
        assertThrows(NullPointerException.class, () -> new SpectralLibrary(id, null));
        assertThrows(NullPointerException.class, () -> new SpectralLibrary(id, "n", null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_addAndRemoveProfiles() {
        SpectralLibrary lib = SpectralLibrary.create("lib");
        UUID pid = UUID.randomUUID();
        SpectralProfile p = profile(pid, "p1");

        lib.addProfile(p);
        assertEquals(1, lib.size());
        assertTrue(lib.containsProfile(pid));
        assertTrue(lib.findProfile(pid).isPresent());

        assertTrue(lib.removeProfile(pid));
        assertEquals(0, lib.size());
        assertFalse(lib.containsProfile(pid));
        assertFalse(lib.findProfile(pid).isPresent());

        assertFalse(lib.removeProfile(pid));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_addRejectsNullAndDuplicateIds() {
        SpectralLibrary lib = SpectralLibrary.create("lib");
        assertThrows(NullPointerException.class, () -> lib.addProfile(null));

        UUID pid = UUID.randomUUID();
        lib.addProfile(profile(pid, "a"));
        assertThrows(IllegalArgumentException.class, () -> lib.addProfile(profile(pid, "b")));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_getProfilesIsUnmodifiable() {
        SpectralLibrary lib = SpectralLibrary.create("lib");
        lib.addProfile(profile(UUID.randomUUID(), "p1"));

        List<SpectralProfile> view = lib.getProfiles();
        assertThrows(UnsupportedOperationException.class, () -> view.add(profile(UUID.randomUUID(), "p2")));
        assertEquals(1, lib.size());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_constructorDefensivelyCopiesAndEnforcesUniqueness() {
        UUID pid = UUID.randomUUID();
        SpectralProfile p = profile(pid, "p1");

        List<SpectralProfile> input = new ArrayList<>();
        input.add(p);

        SpectralLibrary lib = new SpectralLibrary(UUID.randomUUID(), "lib", input);
        assertEquals(1, lib.size());

        input.clear();
        assertEquals(1, lib.size());

        List<SpectralProfile> dup = List.of(p, profile(pid, "p2"));
        assertThrows(IllegalArgumentException.class,
                () -> new SpectralLibrary(UUID.randomUUID(), "lib2", dup));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_canRenameLibrary() {
        SpectralLibrary lib = SpectralLibrary.create("old");
        lib.setName("new");
        assertEquals("new", lib.getName());
        assertThrows(NullPointerException.class, () -> lib.setName(null));
    }
}