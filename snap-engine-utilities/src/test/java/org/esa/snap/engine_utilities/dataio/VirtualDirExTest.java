package org.esa.snap.engine_utilities.dataio;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VirtualDirExTest {

    @Test
    public void testIsTar() {
        assertFalse(VirtualDirEx.isTar("xxxxx.nc.gz"));
        assertFalse(VirtualDirEx.isTar("xxxxx.ppt.gz"));

        assertTrue(VirtualDirEx.isTar("xxxxx.tar"));
        assertTrue(VirtualDirEx.isTar("xxxxx.TAR"));
    }
}
