package eu.esa.snap.core.dataio.cache;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StorageDimensionsTest {

    @Test
    public void testConstruction()  {
        final StorageDimensions storageDimensions = new StorageDimensions();

        assertEquals(-1, storageDimensions.getRasterWidth());
        assertEquals(-1, storageDimensions.getRasterHeight());
        assertEquals(-1, storageDimensions.getRasterLayers());

        assertEquals(-1, storageDimensions.getTileWidth());
        assertEquals(-1, storageDimensions.getTileHeight());
        assertEquals(-1, storageDimensions.getTileLayers());
    }
}
