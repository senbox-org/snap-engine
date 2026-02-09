package org.esa.snap.speclib.impl;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.speclib.api.SpectralProfileExtractor;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralLibrary;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.*;


public class SpectralLibraryServiceImplTest {


    @Test
    @STTM("SNAP-4128")
    public void test_defaultConstructor_worksEndToEndWithMockedBands() {
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl();

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = svc.createLibrary("L", axis, "reflectance");
        assertNotNull(lib);

        SpectralProfile p = SpectralProfile.create("p", SpectralSignature.of(new double[]{0.1, 0.2}));
        svc.addProfile(lib.getId(), p);

        assertTrue(svc.findProfile(lib.getId(), p.getId()).isPresent());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_createGetListDeleteRename() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = svc.createLibrary("A", axis, "reflectance");

        assertTrue(svc.getLibrary(lib.getId()).isPresent());
        assertEquals(1, svc.listLibraries().size());

        assertTrue(svc.renameLibrary(lib.getId(), "B").isPresent());
        assertEquals("B", svc.getLibrary(lib.getId()).orElseThrow().getName());

        assertTrue(svc.renameLibrary(UUID.randomUUID(), "X").isEmpty());

        assertTrue(svc.deleteLibrary(lib.getId()));
        assertTrue(svc.getLibrary(lib.getId()).isEmpty());
        assertFalse(svc.deleteLibrary(lib.getId()));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_addFindRemoveProfile() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = svc.createLibrary("L", axis, null);

        SpectralProfile p = new SpectralProfile(UUID.randomUUID(), "p",
                SpectralSignature.of(new double[]{0.1, 0.2}), Map.of(), null);

        svc.addProfile(lib.getId(), p);

        assertTrue(svc.findProfile(lib.getId(), p.getId()).isPresent());
        assertTrue(svc.removeProfile(lib.getId(), p.getId()));
        assertTrue(svc.findProfile(lib.getId(), p.getId()).isEmpty());

        assertFalse(svc.removeProfile(lib.getId(), p.getId()));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_addProfile_unknownLibraryThrows() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        SpectralProfile p = SpectralProfile.create("p", SpectralSignature.of(new double[]{1, 2}));
        assertThrows(NoSuchElementException.class, () -> svc.addProfile(UUID.randomUUID(), p));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_removeProfile_unknownLibraryReturnsFalse_findProfileUnknownLibraryEmpty() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        assertFalse(svc.removeProfile(UUID.randomUUID(), UUID.randomUUID()));
        assertTrue(svc.findProfile(UUID.randomUUID(), UUID.randomUUID()).isEmpty());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_extractProfileDelegatesToExtractor() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        List<Band> bands = List.of(Mockito.mock(Band.class), Mockito.mock(Band.class));

        SpectralProfile expected = SpectralProfile.create("p", SpectralSignature.of(new double[]{1, 2}));
        Mockito.when(extractor.extract("p", axis, bands, 1, 2, 0, "reflectance", "prod"))
                .thenReturn(Optional.of(expected));

        Optional<SpectralProfile> got = svc.extractProfile("p", axis, bands, 1, 2, 0, "reflectance", "prod");
        assertTrue(got.isPresent());
        assertEquals(expected.getId(), got.orElseThrow().getId());
    }
}