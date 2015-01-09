package org.esa.beam.dataio.bigtiff.internal;

import org.junit.Test;

import static org.junit.Assert.*;

public class GeoKeyEntryTest {

    @Test
    public void testConstruct_Integer(){
        final GeoKeyEntry entry = new GeoKeyEntry(3072, 9, 1, 89);

        assertEquals(89, entry.getIntValue().intValue());
        assertEquals(3072, entry.getKeyId());
        assertEquals("ProjectedCSTypeGeoKey", entry.getName());
        assertTrue(entry.hasIntValue());
    }
}
