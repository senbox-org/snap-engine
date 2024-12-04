package org.esa.snap.tests.unittests.util;

import org.esa.snap.performance.util.Threading;
import org.junit.Test;

import static org.junit.Assert.*;

public class ThreadingTest {

    @Test
    public void testMatchStringToEnum() {

        assertEquals(Threading.SINGLE, Threading.matchStringToEnum("SINGLE"));
        assertEquals(Threading.SINGLE, Threading.matchStringToEnum("single"));
        assertEquals(Threading.SINGLE, Threading.matchStringToEnum("SinGLe"));
        assertEquals(Threading.SINGLE, Threading.matchStringToEnum("dsfdsfs"));
        assertEquals(Threading.SINGLE, Threading.matchStringToEnum(""));

        assertEquals(Threading.MULTI, Threading.matchStringToEnum("multi"));
        assertEquals(Threading.MULTI, Threading.matchStringToEnum("MULTI"));
        assertEquals(Threading.MULTI, Threading.matchStringToEnum("muLTi"));
        assertEquals(Threading.MULTI, Threading.matchStringToEnum("Multi"));
    }
}