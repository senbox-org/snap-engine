package org.esa.snap.speclib.impl;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.speclib.api.SpectralLibraryService;
import org.esa.snap.speclib.api.SpectralProfileExtractor;
import org.esa.snap.speclib.model.SpectralLibrary;
import org.esa.snap.speclib.model.SpectralProfile;
import org.junit.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class SpectralLibraryServiceImplTest {


    private static SpectralProfile profile(UUID id, String name) {
        return new SpectralProfile(id, name, new double[]{500.0}, new double[]{0.1}, "unit");
    }


    @Test
    @STTM("SNAP-4128")
    public void test_createAndGetLibrary() {
        SpectralLibraryService svc = new SpectralLibraryServiceImpl();

        SpectralLibrary lib = svc.createLibrary("lib");
        assertNotNull(lib.getId());
        assertEquals("lib", lib.getName());

        Optional<SpectralLibrary> loaded = svc.getLibrary(lib.getId());
        assertTrue(loaded.isPresent());
        assertSame(lib, loaded.get());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_listLibrariesReturnsSnapshotAndIsUnmodifiable() {
        SpectralLibraryService svc = new SpectralLibraryServiceImpl();
        SpectralLibrary a = svc.createLibrary("a");
        SpectralLibrary b = svc.createLibrary("b");

        List<SpectralLibrary> list = svc.listLibraries();
        assertEquals(2, list.size());
        assertThrows(UnsupportedOperationException.class, () -> list.add(a));

        svc.createLibrary("c");
        assertEquals(2, list.size());
        assertEquals(3, svc.listLibraries().size());

        assertEquals("a", svc.listLibraries().get(0).getName());
        assertEquals("b", svc.listLibraries().get(1).getName());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_deleteLibraryReturnsTrueIfRemoved() {
        SpectralLibraryService svc = new SpectralLibraryServiceImpl();
        SpectralLibrary lib = svc.createLibrary("lib");

        assertTrue(svc.deleteLibrary(lib.getId()));
        assertFalse(svc.deleteLibrary(lib.getId()));
        assertTrue(svc.getLibrary(lib.getId()).isEmpty());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_addFindAndRemoveProfile() {
        SpectralLibraryService svc = new SpectralLibraryServiceImpl();
        SpectralLibrary lib = svc.createLibrary("lib");

        UUID pid = UUID.randomUUID();
        SpectralProfile p = profile(pid, "p1");

        svc.addProfile(lib.getId(), p);

        Optional<SpectralProfile> found = svc.findProfile(lib.getId(), pid);
        assertTrue(found.isPresent());
        assertSame(p, found.get());

        assertTrue(svc.removeProfile(lib.getId(), pid));
        assertFalse(svc.removeProfile(lib.getId(), pid));
        assertTrue(svc.findProfile(lib.getId(), pid).isEmpty());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_addProfileThrowsIfLibraryMissing() {
        SpectralLibraryService svc = new SpectralLibraryServiceImpl();
        UUID missingLib = UUID.randomUUID();

        assertThrows(NoSuchElementException.class,
                () -> svc.addProfile(missingLib, profile(UUID.randomUUID(), "p")));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_findAndRemoveReturnEmptyFalseIfLibraryMissing() {
        SpectralLibraryService svc = new SpectralLibraryServiceImpl();
        UUID missingLib = UUID.randomUUID();
        UUID pid = UUID.randomUUID();

        assertTrue(svc.findProfile(missingLib, pid).isEmpty());
        assertFalse(svc.removeProfile(missingLib, pid));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_addProfileRejectsDuplicateProfileId() {
        SpectralLibraryService svc = new SpectralLibraryServiceImpl();
        SpectralLibrary lib = svc.createLibrary("lib");

        UUID pid = UUID.randomUUID();
        svc.addProfile(lib.getId(), profile(pid, "a"));

        assertThrows(IllegalArgumentException.class,
                () -> svc.addProfile(lib.getId(), profile(pid, "b")));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_nullArgumentsAreRejected() {
        SpectralLibraryService svc = new SpectralLibraryServiceImpl();
        SpectralLibrary lib = svc.createLibrary("lib");

        assertThrows(NullPointerException.class, () -> svc.getLibrary(null));
        assertThrows(NullPointerException.class, () -> svc.deleteLibrary(null));
        assertThrows(NullPointerException.class, () -> svc.addProfile(null, profile(UUID.randomUUID(), "p")));
        assertThrows(NullPointerException.class, () -> svc.addProfile(lib.getId(), null));
        assertThrows(NullPointerException.class, () -> svc.removeProfile(null, UUID.randomUUID()));
        assertThrows(NullPointerException.class, () -> svc.removeProfile(lib.getId(), null));
        assertThrows(NullPointerException.class, () -> svc.findProfile(null, UUID.randomUUID()));
        assertThrows(NullPointerException.class, () -> svc.findProfile(lib.getId(), null));
    }


    @Test
    @STTM("SNAP-4128")
    public void test_extractProfile_delegatesToExtractor_andReturnsProfile() {
        SpectralProfileExtractor extractor = mock(SpectralProfileExtractor.class);
        SpectralLibraryService svc = new SpectralLibraryServiceImpl(extractor);

        Band b1 = mock(Band.class);
        Band b2 = mock(Band.class);
        List<Band> bands = List.of(b1, b2);

        SpectralProfile expected = SpectralProfile.create("p",
                new double[]{500.0, 600.0},
                new double[]{0.1, 0.2},
                "unit");

        when(extractor.extract("p", bands, 3, 4, 0, "unit")).thenReturn(Optional.of(expected));

        Optional<SpectralProfile> out = svc.extractProfile("p", bands, 3, 4, 0, "unit");
        assertTrue(out.isPresent());
        assertSame(expected, out.get());

        verify(extractor).extract("p", bands, 3, 4, 0, "unit");
        verifyNoMoreInteractions(extractor);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_extractProfile_delegatesToExtractor_andReturnsEmpty() {
        SpectralProfileExtractor extractor = mock(SpectralProfileExtractor.class);
        SpectralLibraryService svc = new SpectralLibraryServiceImpl(extractor);

        Band b = mock(Band.class);
        List<Band> bands = List.of(b);

        when(extractor.extract("p", bands, 1, 2, 0, "unit")).thenReturn(Optional.empty());

        Optional<SpectralProfile> out = svc.extractProfile("p", bands, 1, 2, 0, "unit");
        assertTrue(out.isEmpty());

        verify(extractor).extract("p", bands, 1, 2, 0, "unit");
        verifyNoMoreInteractions(extractor);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_extractProfile_rejectsNullArguments() {
        SpectralProfileExtractor extractor = mock(SpectralProfileExtractor.class);
        SpectralLibraryService svc = new SpectralLibraryServiceImpl(extractor);

        Band b = mock(Band.class);
        List<Band> bands = List.of(b);

        assertThrows(NullPointerException.class, () -> svc.extractProfile(null, bands, 0, 0, 0, "u"));
        assertThrows(NullPointerException.class, () -> svc.extractProfile("n", null, 0, 0, 0, "u"));
        assertThrows(NullPointerException.class, () -> svc.extractProfile("n", bands, 0, 0, 0, null));
    }
}