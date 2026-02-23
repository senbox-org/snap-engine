package org.esa.snap.speclib.model;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;


public class SpectralLibraryTest {


    @Test
    @STTM("SNAP-4128")
    public void test_createAndBasicAccessors() {
        SpectralAxis axis = new SpectralAxis(new double[]{1, 2, 3}, "nm");
        SpectralLibrary lib = SpectralLibrary.create("L", axis, "reflectance");

        assertNotNull(lib.getId());
        assertEquals("L", lib.getName());
        assertEquals(axis, lib.getAxis());
        assertEquals("reflectance", lib.getDefaultYUnit().orElseThrow());
        assertEquals(0, lib.size());
        assertNotNull(lib.getSchema());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_ctorRejectsNulls() {
        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        UUID id = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> new SpectralLibrary(null, "n", axis, null, List.of(), new AttributeSchema()));
        assertThrows(NullPointerException.class, () -> new SpectralLibrary(id, null, axis, null, List.of(), new AttributeSchema()));
        assertThrows(NullPointerException.class, () -> new SpectralLibrary(id, "n", null, null, List.of(), new AttributeSchema()));
        assertThrows(NullPointerException.class, () -> new SpectralLibrary(id, "n", axis, null, null, new AttributeSchema()));
        assertThrows(NullPointerException.class, () -> new SpectralLibrary(id, "n", axis, null, List.of(), null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_validatesProfileLengthMatchesAxis() {
        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralProfile pBad = SpectralProfile.create("p", SpectralSignature.of(new double[]{9, 9, 9}));

        assertThrows(IllegalArgumentException.class, () ->
                new SpectralLibrary(UUID.randomUUID(), "n", axis, null, List.of(pBad), new AttributeSchema()));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_withNameCopyOnWrite() {
        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = SpectralLibrary.create("A", axis, null);

        SpectralLibrary lib2 = lib.withName("B");
        assertNotSame(lib, lib2);
        assertEquals("A", lib.getName());
        assertEquals("B", lib2.getName());
        assertEquals(lib.getId(), lib2.getId());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_addProfileAddsAndInfersSchema() {
        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = SpectralLibrary.create("L", axis, "reflectance");

        SpectralProfile p = SpectralProfile.create("p", SpectralSignature.of(new double[]{0.1, 0.2}))
                .withAttribute("class", AttributeValue.ofString("veg"))
                .withAttribute("confidence", AttributeValue.ofDouble(0.9));

        SpectralLibrary lib2 = lib.withProfileAdded(p);

        assertEquals(0, lib.size());
        assertEquals(1, lib2.size());
        assertTrue(lib2.findProfile(p.getId()).isPresent());
        assertEquals(AttributeType.STRING, lib2.getSchema().find("class").orElseThrow().getType());
        assertEquals(AttributeType.DOUBLE, lib2.getSchema().find("confidence").orElseThrow().getType());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_addProfileRejectsDuplicateIdAndWrongLength() {
        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = SpectralLibrary.create("L", axis, null);

        UUID pid = UUID.randomUUID();
        SpectralProfile p1 = new SpectralProfile(pid, "p1", SpectralSignature.of(new double[]{1, 2}), Map.of(), null);
        SpectralProfile p2 = new SpectralProfile(pid, "p2", SpectralSignature.of(new double[]{3, 4}), Map.of(), null);

        SpectralLibrary lib2 = lib.withProfileAdded(p1);
        assertThrows(IllegalArgumentException.class, () -> lib2.withProfileAdded(p2));

        SpectralProfile badLen = SpectralProfile.create("bad", SpectralSignature.of(new double[]{1, 2, 3}));
        assertThrows(IllegalArgumentException.class, () -> lib.withProfileAdded(badLen));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_removeProfileIsIdempotentAndCopyOnWrite() {
        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = SpectralLibrary.create("L", axis, null);
        SpectralProfile p = SpectralProfile.create("p", SpectralSignature.of(new double[]{1, 2}));
        SpectralLibrary lib2 = lib.withProfileAdded(p);

        SpectralLibrary lib3 = lib2.withProfileRemoved(p.getId());
        assertEquals(0, lib3.size());

        SpectralLibrary lib4 = lib3.withProfileRemoved(p.getId());
        assertSame(lib3, lib4);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_collectionsAreUnmodifiable() {
        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = SpectralLibrary.create("L", axis, null);
        assertThrows(UnsupportedOperationException.class, () -> lib.getProfiles().add(null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_findProfileReturnsEmptyWhenNotFound() {
        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = SpectralLibrary.create("L", axis, null);

        assertTrue(lib.findProfile(UUID.randomUUID()).isEmpty());
    }
}