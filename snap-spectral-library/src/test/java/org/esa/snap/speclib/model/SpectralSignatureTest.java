package org.esa.snap.speclib.model;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.*;


public class SpectralSignatureTest {


    @Test
    @STTM("SNAP-4128")
    public void test_factoryRejectsNullsAndEmpty() {
        assertThrows(NullPointerException.class, () -> SpectralSignature.of(null));
        assertThrows(IllegalArgumentException.class, () -> SpectralSignature.of(new double[]{}));
        assertThrows(NullPointerException.class, () -> SpectralSignature.of(new double[]{1}, null));
    }

    @Test
    @STTM("SNAP-4128")
    public void test_valuesAreDefensivelyCopied() {
        double[] v = new double[]{0.1, 0.2};
        SpectralSignature s = SpectralSignature.of(v);
        v[0] = 9.9;
        assertEquals(0.1, s.getValues()[0], 0.0001);

        double[] g = s.getValues();
        g[0] = 8.8;
        assertEquals(0.1, s.getValues()[0], 0.0001);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_yUnitOptionalBehaviour() {
        SpectralSignature s1 = SpectralSignature.of(new double[]{1, 2});
        assertNull(s1.getYUnitOrNull());

        SpectralSignature s2 = SpectralSignature.of(new double[]{1, 2}, "reflectance");
        assertEquals("reflectance", s2.getYUnitOrNull());
    }

}