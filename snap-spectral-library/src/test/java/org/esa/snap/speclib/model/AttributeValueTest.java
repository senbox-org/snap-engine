package org.esa.snap.speclib.model;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


public class AttributeValueTest {


    @Test
    @STTM("SNAP-4128")
    public void test_factoriesSetCorrectTypesAndValues() {
        assertEquals(AttributeType.STRING, AttributeValue.ofString("a").getType());
        assertEquals("a", AttributeValue.ofString("a").asString());

        assertEquals(AttributeType.INT, AttributeValue.ofInt(7).getType());
        assertEquals(7, AttributeValue.ofInt(7).asInt());

        assertEquals(AttributeType.LONG, AttributeValue.ofLong(7L).getType());
        assertEquals(7L, AttributeValue.ofLong(7L).asLong());

        assertEquals(AttributeType.DOUBLE, AttributeValue.ofDouble(1.5).getType());
        assertEquals(1.5, AttributeValue.ofDouble(1.5).asDouble(), 0.0001);

        assertEquals(AttributeType.BOOLEAN, AttributeValue.ofBoolean(true).getType());
        assertTrue(AttributeValue.ofBoolean(true).asBoolean());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_listAndMapFactoriesAreImmutableCopies() {
        AttributeValue vList = AttributeValue.ofStringList(List.of("a", "b"));
        assertEquals(List.of("a", "b"), vList.asStringList());

        AttributeValue vMap = AttributeValue.ofStringMap(Map.of("k", "v"));
        assertEquals(Map.of("k", "v"), vMap.asStringMap());
    }

    @Test
    @STTM("SNAP-4128")
    public void test_arrayFactoriesDefensivelyCopy() {
        double[] d = new double[]{1.0, 2.0};
        AttributeValue vd = AttributeValue.ofDoubleArray(d);
        d[0] = 9.0;
        assertEquals(1.0, vd.asDoubleArray()[0], 0.0001);

        int[] i = new int[]{1, 2};
        AttributeValue vi = AttributeValue.ofIntArray(i);
        i[0] = 9;
        assertEquals(1, vi.asIntArray()[0]);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_embeddedSpectrumValidatesLengthMatch() {
        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralSignature sigOk = SpectralSignature.of(new double[]{10, 20}, "reflectance");
        AttributeValue.EmbeddedSpectrum ok = new AttributeValue.EmbeddedSpectrum(axis, sigOk);
        AttributeValue v = AttributeValue.ofEmbeddedSpectrum(ok);
        assertEquals(AttributeType.EMBEDDED_SPECTRUM, v.getType());
        assertSame(ok, v.asEmbeddedSpectrum());

        SpectralSignature sigBad = SpectralSignature.of(new double[]{10}, "reflectance");
        assertThrows(IllegalArgumentException.class, () -> new AttributeValue.EmbeddedSpectrum(axis, sigBad));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_nullChecks() {
        assertThrows(NullPointerException.class, () -> AttributeValue.ofString(null));
        assertThrows(NullPointerException.class, () -> AttributeValue.ofStringList(null));
        assertThrows(NullPointerException.class, () -> AttributeValue.ofStringMap(null));
        assertThrows(NullPointerException.class, () -> AttributeValue.ofDoubleArray(null));
        assertThrows(NullPointerException.class, () -> AttributeValue.ofIntArray(null));
        assertThrows(NullPointerException.class, () -> AttributeValue.ofEmbeddedSpectrum(null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_rawAndEmbeddedSpectrumAccessors() {
        AttributeValue v = AttributeValue.ofString("hello");
        assertEquals("hello", v.raw());

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralSignature sig = SpectralSignature.of(new double[]{10, 20}, "reflectance");
        AttributeValue.EmbeddedSpectrum es = new AttributeValue.EmbeddedSpectrum(axis, sig);

        assertSame(axis, es.getAxis());
        assertSame(sig, es.getSignature());
    }
}