package org.esa.snap.core.dataio.dimap;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DimapProductWriterTest {

    @Test
    public void testIsFullRaster() {
        assertTrue(DimapProductWriter.isFullRaster(100, 200, 100, 200));
        assertTrue(DimapProductWriter.isFullRaster(1011, 199, 1011, 199));

        assertFalse(DimapProductWriter.isFullRaster(99, 200, 100, 200));
        assertFalse(DimapProductWriter.isFullRaster(100, 201, 100, 200));
        assertFalse(DimapProductWriter.isFullRaster(100, 200, 101, 200));
        assertFalse(DimapProductWriter.isFullRaster(100, 200, 100, 199));
    }
}
