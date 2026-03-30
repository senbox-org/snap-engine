package org.esa.snap.speclib.impl;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.speclib.api.SpectralLibraryService;
import org.esa.snap.speclib.api.SpectralProfileExtractor;
import org.esa.snap.speclib.model.*;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


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




    @Test
    @STTM("SNAP-4128")
    public void test_addAttributeToLibrary_addsSchemaAndFillsMissingValues_onlyWhereMissing() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = svc.createLibrary("L", axis, "reflectance");

        SpectralProfile p1 = new SpectralProfile(
                UUID.randomUUID(), "p1",
                SpectralSignature.of(new double[]{0.1, 0.2}),
                Map.of(), null
        );

        SpectralProfile p2 = new SpectralProfile(
                UUID.randomUUID(), "p2",
                SpectralSignature.of(new double[]{0.3, 0.4}),
                Map.of("flag", AttributeValue.ofBoolean(false)),
                null
        );

        svc.addProfile(lib.getId(), p1);
        svc.addProfile(lib.getId(), p2);

        AttributeDef def = new AttributeDef(
                "flag",
                AttributeType.BOOLEAN,
                false,
                AttributeValue.ofBoolean(false),
                null,
                null
        );
        AttributeValue fill = AttributeValue.ofBoolean(true);

        svc.addAttributeToLibrary(lib.getId(), def, fill);

        SpectralLibrary updated = svc.getLibrary(lib.getId()).orElseThrow();

        assertTrue(updated.getSchema().find("flag").isPresent());
        assertEquals(AttributeType.BOOLEAN, updated.getSchema().find("flag").orElseThrow().getType());

        SpectralProfile up1 = updated.findProfile(p1.getId()).orElseThrow();
        assertTrue(up1.getAttribute("flag").isPresent());
        assertTrue(up1.getAttribute("flag").orElseThrow().asBoolean());

        SpectralProfile up2 = updated.findProfile(p2.getId()).orElseThrow();
        assertTrue(up2.getAttribute("flag").isPresent());
        assertFalse(up2.getAttribute("flag").orElseThrow().asBoolean());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_addAttributeToLibrary_unknownLibraryThrows() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        AttributeDef def = new AttributeDef("k", AttributeType.STRING, false, AttributeValue.ofString("d"), null, null);
        AttributeValue fill = AttributeValue.ofString("x");

        assertThrows(NoSuchElementException.class, () -> svc.addAttributeToLibrary(UUID.randomUUID(), def, fill));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_renameProfile_changesNameAndReturnsTrue() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = svc.createLibrary("L", axis, null);

        UUID pid = UUID.randomUUID();
        SpectralProfile p = new SpectralProfile(
                pid, "old",
                SpectralSignature.of(new double[]{0.1, 0.2}),
                Map.of("a", AttributeValue.ofString("v")),
                null
        );
        svc.addProfile(lib.getId(), p);

        assertTrue(svc.renameProfile(lib.getId(), pid, "new"));

        SpectralProfile got = svc.findProfile(lib.getId(), pid).orElseThrow();
        assertEquals("new", got.getName());

        assertEquals(2, got.getSignature().size());
        assertEquals("v", got.getAttribute("a").orElseThrow().asString());
        assertNull(got.getSourceRef().orElse(null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_renameProfile_sameNameReturnsFalseAndDoesNotChange() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = svc.createLibrary("L", axis, null);

        UUID pid = UUID.randomUUID();
        SpectralProfile p = new SpectralProfile(
                pid, "same",
                SpectralSignature.of(new double[]{0.1, 0.2}),
                Map.of(), null
        );
        svc.addProfile(lib.getId(), p);

        assertFalse(svc.renameProfile(lib.getId(), pid, "same"));
        assertEquals("same", svc.findProfile(lib.getId(), pid).orElseThrow().getName());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_renameProfile_unknownLibraryReturnsFalse() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        assertFalse(svc.renameProfile(UUID.randomUUID(), UUID.randomUUID(), "x"));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_renameProfile_profileNotFoundReturnsFalse() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = svc.createLibrary("L", axis, null);

        assertFalse(svc.renameProfile(lib.getId(), UUID.randomUUID(), "x"));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_setProfileAttribute_setsValueAndReturnsTrue() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = svc.createLibrary("L", axis, null);

        UUID pid = UUID.randomUUID();
        SpectralProfile p = new SpectralProfile(
                pid, "p",
                SpectralSignature.of(new double[]{0.1, 0.2}),
                Map.of(), null
        );
        svc.addProfile(lib.getId(), p);

        assertTrue(svc.setProfileAttribute(lib.getId(), pid, "foo", AttributeValue.ofString("bar")));

        SpectralProfile got = svc.findProfile(lib.getId(), pid).orElseThrow();
        assertEquals("bar", got.getAttribute("foo").orElseThrow().asString());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_setProfileAttribute_unknownLibraryReturnsFalse() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        assertFalse(svc.setProfileAttribute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "k",
                AttributeValue.ofString("v")
        ));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_setProfileAttribute_profileNotFoundReturnsFalse() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = svc.createLibrary("L", axis, null);

        assertFalse(svc.setProfileAttribute(
                lib.getId(),
                UUID.randomUUID(),
                "k",
                AttributeValue.ofString("v")
        ));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_nullChecks_newMethods() {
        SpectralProfileExtractor extractor = Mockito.mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        UUID anyId = UUID.randomUUID();
        AttributeDef def = new AttributeDef("k", AttributeType.STRING, false, AttributeValue.ofString("d"), null, null);
        AttributeValue v = AttributeValue.ofString("x");

        assertThrows(NullPointerException.class, () -> svc.addAttributeToLibrary(null, def, v));
        assertThrows(NullPointerException.class, () -> svc.addAttributeToLibrary(anyId, null, v));
        assertThrows(NullPointerException.class, () -> svc.addAttributeToLibrary(anyId, def, null));

        assertThrows(NullPointerException.class, () -> svc.renameProfile(null, anyId, "n"));
        assertThrows(NullPointerException.class, () -> svc.renameProfile(anyId, null, "n"));
        assertThrows(NullPointerException.class, () -> svc.renameProfile(anyId, anyId, null));

        assertThrows(NullPointerException.class, () -> svc.setProfileAttribute(null, anyId, "k", v));
        assertThrows(NullPointerException.class, () -> svc.setProfileAttribute(anyId, null, "k", v));
        assertThrows(NullPointerException.class, () -> svc.setProfileAttribute(anyId, anyId, null, v));
        assertThrows(NullPointerException.class, () -> svc.setProfileAttribute(anyId, anyId, "k", null));
    }

    @Test
    @STTM("SNAP-4128")
    public void extractProfiles_emptyPixels_returnsEmpty_andDoesNotCallExtractor() {
        SpectralProfileExtractor extractor = mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        SpectralAxis axis = mock(SpectralAxis.class);
        when(axis.size()).thenReturn(1);

        List<SpectralProfile> out = svc.extractProfiles(
                "base", axis, List.of(mock(Band.class)), List.of(),
                0, "unit", "prod"
        );

        assertNotNull(out);
        assertTrue(out.isEmpty());
        verify(extractor, never()).extractBulk(anyString(), any(), anyList(), anyList(), anyInt(), anyString(), anyString());
    }

    @Test
    @STTM("SNAP-4128")
    public void extractProfiles_delegatesToExtractorBulk() {
        SpectralProfileExtractor extractor = mock(SpectralProfileExtractor.class);
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl(extractor);

        SpectralAxis axis = mock(SpectralAxis.class);
        when(axis.size()).thenReturn(2);

        Band b1 = mock(Band.class);
        Band b2 = mock(Band.class);

        List<PixelPos> pixels = List.of(new PixelPos(1, 2), new PixelPos(3, 4));

        SpectralProfile p = Mockito.mock(SpectralProfile.class);
        List<SpectralProfile> expected = List.of(p);

        when(extractor.extractBulk(eq("base"), same(axis), eq(List.of(b1, b2)), eq(pixels), eq(1), eq("u"), eq("prod")))
                .thenReturn(expected);

        List<SpectralProfile> out = svc.extractProfiles("base", axis, List.of(b1, b2), pixels, 1, "u", "prod");

        assertSame(expected, out);
        verify(extractor, times(1)).extractBulk("base", axis, List.of(b1, b2), pixels, 1, "u", "prod");
    }


    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4128")
    public void addProfiles_nullLibraryId_throws() {
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl();
        svc.addProfiles(null, List.of());
    }

    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4128")
    public void addProfiles_nullProfiles_throws() {
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl();
        SpectralLibrary lib = svc.createLibrary("L", axis(3), "value");
        svc.addProfiles(lib.getId(), null);
    }

    @Test
    @STTM("SNAP-4128")
    public void addProfiles_emptyList_returns00_andDoesNotChangeLibrary() {
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl();
        SpectralLibrary lib = svc.createLibrary("L", axis(3), "value");
        UUID id = lib.getId();

        SpectralLibrary before = svc.getLibrary(id).orElseThrow();
        SpectralLibraryService.BulkAddResult r = svc.addProfiles(id, List.of());
        SpectralLibrary after = svc.getLibrary(id).orElseThrow();

        assertEquals(0, r.added());
        assertEquals(0, r.skippedExisting());
        assertSame(before, after);
        assertTrue(after.getProfiles().isEmpty());
        assertTrue(after.getSchema().asMap().isEmpty());
    }

    @Test(expected = NoSuchElementException.class)
    @STTM("SNAP-4128")
    public void addProfiles_libraryNotFound_throws() {
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl();
        svc.addProfiles(UUID.randomUUID(), List.of(profile(UUID.randomUUID(), "p", 3, Map.of())));
    }

    @Test
    @STTM("SNAP-4128")
    public void addProfiles_addsProfiles_preservesExistingOrder_andInfersSchema() {
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl();
        SpectralLibrary lib = svc.createLibrary("L", axis(3), "value");
        UUID id = lib.getId();

        UUID p0id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        SpectralProfile existing = profile(p0id, "existing", 3, Map.of());
        svc.addProfile(id, existing);

        UUID p1id = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID p2id = UUID.fromString("00000000-0000-0000-0000-000000000003");

        SpectralProfile p1 = profile(p1id, "p1", 3, Map.of("foo", AttributeValue.ofString("x")));
        SpectralProfile p2 = profile(p2id, "p2", 3, Map.of("bar", AttributeValue.ofInt(7)));

        SpectralLibraryService.BulkAddResult r = svc.addProfiles(id, List.of(p1, p2));

        assertEquals(2, r.added());
        assertEquals(0, r.skippedExisting());

        SpectralLibrary after = svc.getLibrary(id).orElseThrow();
        assertEquals(3, after.getProfiles().size());

        assertEquals(p0id, after.getProfiles().get(0).getId());
        assertEquals(p1id, after.getProfiles().get(1).getId());
        assertEquals(p2id, after.getProfiles().get(2).getId());

        assertTrue(after.getSchema().find("foo").isPresent());
        assertEquals(AttributeType.STRING, after.getSchema().find("foo").get().getType());

        assertTrue(after.getSchema().find("bar").isPresent());
        assertEquals(AttributeType.INT, after.getSchema().find("bar").get().getType());
    }

    @Test
    @STTM("SNAP-4128")
    public void addProfiles_skipsDuplicateIds_countsSkipped_andDoesNotAddDuplicate() {
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl();
        SpectralLibrary lib = svc.createLibrary("L", axis(3), "value");
        UUID id = lib.getId();

        UUID dupId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        SpectralProfile existing = profile(dupId, "existing", 3, Map.of());
        svc.addProfile(id, existing);

        SpectralProfile dup = profile(dupId, "dup", 3, Map.of("x", AttributeValue.ofString("y")));
        SpectralProfile fresh = profile(UUID.fromString("00000000-0000-0000-0000-000000000011"), "fresh", 3, Map.of());

        SpectralLibraryService.BulkAddResult r = svc.addProfiles(id, List.of(dup, fresh));

        assertEquals(1, r.added());
        assertEquals(1, r.skippedExisting());

        SpectralLibrary after = svc.getLibrary(id).orElseThrow();
        assertEquals(2, after.getProfiles().size());
        assertEquals(dupId, after.getProfiles().get(0).getId());
        assertEquals(fresh.getId(), after.getProfiles().get(1).getId());

        assertTrue(after.getSchema().find("x").isEmpty());
    }

    @Test
    @STTM("SNAP-4128")
    public void addProfiles_ignoresNullElements() {
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl();
        SpectralLibrary lib = svc.createLibrary("L", axis(3), "value");
        UUID id = lib.getId();

        SpectralProfile p = profile(UUID.fromString("00000000-0000-0000-0000-000000000020"), "p", 3, Map.of());
        List<SpectralProfile> nullList = new ArrayList<>();
        nullList.add(null);
        nullList.add(p);
        nullList.add(null);
        SpectralLibraryService.BulkAddResult r = svc.addProfiles(id, nullList);

        assertEquals(1, r.added());
        assertEquals(0, r.skippedExisting());

        SpectralLibrary after = svc.getLibrary(id).orElseThrow();
        assertEquals(1, after.getProfiles().size());
        assertEquals(p.getId(), after.getProfiles().get(0).getId());
    }

    @Test
    @STTM("SNAP-4128")
    public void addProfiles_axisMismatch_throws_andLibraryRemainsUnchanged() {
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl();
        SpectralLibrary lib = svc.createLibrary("L", axis(3), "value");
        UUID id = lib.getId();

        SpectralLibrary before = svc.getLibrary(id).orElseThrow();
        SpectralProfile bad = profile(UUID.fromString("00000000-0000-0000-0000-000000000030"), "bad", 2, Map.of());

        try {
            svc.addProfiles(id, List.of(bad));
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }

        SpectralLibrary after = svc.getLibrary(id).orElseThrow();
        assertSame(before, after);
        assertTrue(after.getProfiles().isEmpty());
        assertTrue(after.getSchema().asMap().isEmpty());
    }

    @Test
    @STTM("SNAP-4128")
    public void addProfiles_allAlreadyExist_returnsSameLibraryInstance() {
        SpectralLibraryServiceImpl svc = new SpectralLibraryServiceImpl();
        SpectralLibrary lib = svc.createLibrary("L", axis(3), "value");
        UUID id = lib.getId();

        UUID pid = UUID.fromString("00000000-0000-0000-0000-000000000040");
        SpectralProfile p = profile(pid, "p", 3, Map.of());
        svc.addProfile(id, p);

        SpectralLibrary before = svc.getLibrary(id).orElseThrow();
        SpectralLibraryService.BulkAddResult r = svc.addProfiles(id, List.of(p));
        SpectralLibrary after = svc.getLibrary(id).orElseThrow();

        assertEquals(0, r.added());
        assertEquals(1, r.skippedExisting());
        assertSame(before, after);
        assertEquals(1, after.getProfiles().size());
    }


    private static SpectralAxis axis(int n) {
        double[] wl = new double[n];
        for (int i = 0; i < n; i++) {
            wl[i] = 400.0 + i;
        }
        return new SpectralAxis(wl, "nm");
    }

    private static SpectralProfile profile(UUID id, String name, int size, Map<String, AttributeValue> attrs) {
        double[] values = new double[size];
        for (int i = 0; i < size; i++) {
            values[i] = i + 1.0;
        }
        return new SpectralProfile(id, name, SpectralSignature.of(values), attrs, null);
    }
}