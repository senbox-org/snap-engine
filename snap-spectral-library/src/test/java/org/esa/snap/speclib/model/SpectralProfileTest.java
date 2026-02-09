package org.esa.snap.speclib.model;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;


public class SpectralProfileTest {


    @Test
    @STTM("SNAP-4128")
    public void test_ctorRejectsNulls() {
        UUID id = UUID.randomUUID();
        SpectralSignature sig = SpectralSignature.of(new double[]{1, 2});

        assertThrows(NullPointerException.class, () -> new SpectralProfile(null, "n", sig, Map.of(), null));
        assertThrows(NullPointerException.class, () -> new SpectralProfile(id, null, sig, Map.of(), null));
        assertThrows(NullPointerException.class, () -> new SpectralProfile(id, "n", null, Map.of(), null));
        assertThrows(NullPointerException.class, () -> new SpectralProfile(id, "n", sig, null, null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_attributesAreUnmodifiableAndCopied() {
        SpectralSignature sig = SpectralSignature.of(new double[]{1, 2});
        Map<String, AttributeValue> attrs = Map.of("a", AttributeValue.ofString("v"));
        SpectralProfile p = new SpectralProfile(UUID.randomUUID(), "p", sig, attrs, null);

        assertThrows(UnsupportedOperationException.class, () -> p.getAttributes().put("x", AttributeValue.ofInt(1)));
        assertEquals("v", p.getAttribute("a").orElseThrow().asString());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_withAttributeReturnsNewInstanceKeepsId() {
        SpectralSignature sig = SpectralSignature.of(new double[]{1, 2});
        UUID id = UUID.randomUUID();
        SpectralProfile p1 = new SpectralProfile(id, "p", sig, Map.of(), null);

        SpectralProfile p2 = p1.withAttribute("class", AttributeValue.ofString("veg"));
        assertNotSame(p1, p2);
        assertEquals(id, p2.getId());
        assertTrue(p1.getAttribute("class").isEmpty());
        assertEquals("veg", p2.getAttribute("class").orElseThrow().asString());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_sourceRefOptional() {
        SpectralSignature sig = SpectralSignature.of(new double[]{1, 2});
        SpectralProfile.SourceRef ref = new SpectralProfile.SourceRef(1, 2, 0, "prod");
        SpectralProfile p = new SpectralProfile(UUID.randomUUID(), "p", sig, Map.of(), ref);

        assertTrue(p.getSourceRef().isPresent());
        assertEquals(1, p.getSourceRef().orElseThrow().getX());
        assertEquals("prod", p.getSourceRef().orElseThrow().getProductId().orElseThrow());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_equalsAndHashCode_useIdOnly() {
        UUID id = UUID.randomUUID();
        SpectralSignature sig = SpectralSignature.of(new double[]{1, 2});

        SpectralProfile a = new SpectralProfile(id, "a", sig, Map.of(), null);
        SpectralProfile b = new SpectralProfile(id, "b", sig, Map.of("x", AttributeValue.ofInt(1)), null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }


    @Test
    @STTM("SNAP-4128")
    public void test_getSignatureAndWithSourceRef_andSourceRefGetters() {
        SpectralSignature sig = SpectralSignature.of(new double[]{1, 2});
        SpectralProfile p1 = new SpectralProfile(UUID.randomUUID(), "p", sig, Map.of(), null);

        assertSame(sig, p1.getSignature());

        SpectralProfile.SourceRef ref = new SpectralProfile.SourceRef(10, 20, 3, "prod");
        SpectralProfile p2 = p1.withSourceRef(ref);

        assertNotSame(p1, p2);
        assertTrue(p2.getSourceRef().isPresent());
        SpectralProfile.SourceRef r = p2.getSourceRef().orElseThrow();
        assertEquals(20, r.getY());
        assertEquals(3, r.getLevel());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_equalsCoversSameInstanceAndDifferentType() {
        SpectralSignature sig = SpectralSignature.of(new double[]{1, 2});
        SpectralProfile p = new SpectralProfile(UUID.randomUUID(), "p", sig, Map.of(), null);

        assertTrue(p.equals(p));
        assertFalse(p.equals("not a profile"));
    }
}